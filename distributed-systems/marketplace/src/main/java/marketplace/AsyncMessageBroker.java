package marketplace;

import com.google.gson.Gson;
import common.Message;
import common.RetryManager;
import common.CircuitBreaker;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class AsyncMessageBroker {
    private final Properties config;
    private final Map<String, String> sellerEndpoints;
    private final ZContext context;
    private final Gson gson;
    private final RetryManager retryManager;
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    // Router-Dealer pattern for async messaging
    private ZMQ.Socket routerSocket;
    private final Map<String, CompletableFuture<Message>> pendingRequests;
    private final ExecutorService messageExecutor;
    private final ScheduledExecutorService timeoutScheduler;
    private final ScheduledExecutorService heartbeatScheduler;
    
    private volatile boolean running = false;
    private Thread receiverThread;
    private final int routerPort;
    
    public AsyncMessageBroker(Properties config) {
        this.config = config;
        this.context = new ZContext();
        this.gson = new Gson();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.messageExecutor = Executors.newFixedThreadPool(10);
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.retryManager = new RetryManager();
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.routerPort = Integer.parseInt(config.getProperty("marketplace.router.port", "5555"));
        
        // Configure seller endpoints - not needed for ROUTER binding
        this.sellerEndpoints = new HashMap<>();
        System.out.println("AsyncMessageBroker initialized with router port: " + routerPort);
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        
        // Create ROUTER socket for async communication
        routerSocket = context.createSocket(SocketType.ROUTER);
        routerSocket.setIdentity(UUID.randomUUID().toString().getBytes());
        
        // Bind ROUTER socket - sellers will connect to us
        String bindAddress = "tcp://*:" + routerPort;
        routerSocket.bind(bindAddress);
        System.out.println("MessageBroker ROUTER socket bound to " + bindAddress);
        
        // Start receiver thread
        receiverThread = new Thread(this::receiveLoop, "MessageBroker-Receiver");
        receiverThread.start();
        
        // Start heartbeat monitoring
        startHeartbeatMonitoring();
    }
    
    private void receiveLoop() {
        ZMQ.Poller poller = context.createPoller(1);
        poller.register(routerSocket, ZMQ.Poller.POLLIN);
        
        while (running) {
            if (poller.poll(1000) > 0) {
                if (poller.pollin(0)) {
                    // Receive multipart message [identity, empty, message]
                    byte[] identity = routerSocket.recv();
                    byte[] empty = routerSocket.recv();
                    byte[] messageBytes = routerSocket.recv();
                    
                    if (messageBytes != null) {
                        try {
                            String messageJson = new String(messageBytes, ZMQ.CHARSET);
                            Message response = gson.fromJson(messageJson, Message.class);
                            
                            // Complete the pending future
                            CompletableFuture<Message> future = pendingRequests.remove(response.getCorrelationId());
                            if (future != null) {
                                future.complete(response);
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing response: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        poller.close();
    }
    
    public CompletableFuture<Message> sendAsyncRequest(String sellerId, Message request) {
        return sendAsyncRequestWithRetry(sellerId, request, "Request to " + sellerId);
    }
    
    public CompletableFuture<Message> sendAsyncRequestWithRetry(String sellerId, Message request, String operationName) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(sellerId);
        
        return circuitBreaker.execute(() -> {
            return retryManager.executeWithRetry(() -> {
                return sendAsyncRequestInternal(sellerId, request);
            }, operationName);
        }, operationName);
    }
    
    private CompletableFuture<Message> sendAsyncRequestInternal(String sellerId, Message request) {
        CompletableFuture<Message> future = new CompletableFuture<>();
        String correlationId = request.getCorrelationId();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            request.setCorrelationId(correlationId);
        }
        
        // Ensure request has a message ID for idempotency
        if (request.getMessageId() == null) {
            request.setMessageId(UUID.randomUUID().toString());
        }
        
        // Schedule timeout first (optimized approach)
        int timeoutMs = Integer.parseInt(config.getProperty("request.timeout.ms", "5000"));
        String finalCorrelationId = correlationId;
        ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(() -> {
            CompletableFuture<Message> pendingFuture = pendingRequests.remove(finalCorrelationId);
            if (pendingFuture != null) {
                pendingFuture.completeExceptionally(
                    new TimeoutException("Request to " + sellerId + " timed out after " + timeoutMs + "ms")
                );
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        // Store future with its timeout task for potential cancellation
        pendingRequests.put(correlationId, future);
        
        // Send request
        final String finalCorrelationId2 = correlationId;
        messageExecutor.execute(() -> {
            try {
                String messageJson = gson.toJson(request);
                byte[] sellerIdentity = sellerId.getBytes(ZMQ.CHARSET);
                
                synchronized (routerSocket) {
                    routerSocket.send(sellerIdentity, ZMQ.SNDMORE);
                    routerSocket.send("", ZMQ.SNDMORE);
                    routerSocket.send(messageJson.getBytes(ZMQ.CHARSET), 0);
                }
                
                System.out.println("Sent request to " + sellerId + " with correlation ID: " + finalCorrelationId2);
                
                // Add hook to cancel timeout when future completes
                future.whenComplete((result, ex) -> {
                    if (!timeoutFuture.isDone()) {
                        timeoutFuture.cancel(false);
                    }
                });
                
            } catch (Exception e) {
                future.completeExceptionally(e);
                pendingRequests.remove(finalCorrelationId2);
            }
        });
        
        return future;
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String sellerId) {
        return circuitBreakers.computeIfAbsent(sellerId, 
            id -> new CircuitBreaker(id, 5, 30000, 3));
    }
    
    private void startHeartbeatMonitoring() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            // Send heartbeat to all connected sellers to check connectivity
            // This is optional - sellers send heartbeats to us
            System.out.println("Heartbeat monitoring active. Pending requests: " + pendingRequests.size());
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        running = false;
        
        // Shutdown executors
        messageExecutor.shutdown();
        timeoutScheduler.shutdown();
        heartbeatScheduler.shutdown();
        retryManager.shutdown();
        
        try {
            if (receiverThread != null) {
                receiverThread.join(5000);
            }
            messageExecutor.awaitTermination(5, TimeUnit.SECONDS);
            timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS);
            heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cancel all pending requests
        pendingRequests.values().forEach(future -> 
            future.completeExceptionally(new RuntimeException("Broker shutdown"))
        );
        pendingRequests.clear();
        
        // Close sockets
        if (routerSocket != null) {
            routerSocket.close();
        }
        context.close();
        
        System.out.println("AsyncMessageBroker shutdown complete");
    }
    
    public Map<String, String> getCircuitBreakerStats() {
        Map<String, String> stats = new HashMap<>();
        circuitBreakers.forEach((sellerId, cb) -> stats.put(sellerId, cb.getStats()));
        return stats;
    }
    
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }
}
