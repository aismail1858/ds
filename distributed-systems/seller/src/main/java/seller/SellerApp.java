package seller;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import common.IdempotencyManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SellerApp {
    private static final String CONFIG_FILE = "config.properties";
    
    private String sellerId;
    private String marketplaceEndpoint;
    private EnhancedInventory inventory;
    private AdvancedFailureSimulator failureSimulator;
    private IdempotencyManager idempotencyManager;
    private final Gson gson = new Gson();
    private volatile boolean running = false;
    
    public SellerApp() {
        this.sellerId = System.getenv().getOrDefault("SELLER_ID", "seller1");
        this.marketplaceEndpoint = System.getenv().getOrDefault("MARKETPLACE_ENDPOINT", "tcp://localhost:5555");
        Properties config = loadConfig();
        this.inventory = new EnhancedInventory(sellerId, config);
        this.failureSimulator = new AdvancedFailureSimulator(config);
        this.idempotencyManager = new IdempotencyManager();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Seller...");
        new SellerApp().run();
    }
    
    public void run() {
        System.out.println("Seller " + sellerId + " connecting to marketplace at " + marketplaceEndpoint + "...");
        System.out.println("Initial inventory: " + inventory.getStatus());
        
        running = true;
        
        try (ZContext context = new ZContext()) {
            // Use DEALER socket instead of REP for proper identity routing
            ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER);
            dealerSocket.setIdentity(sellerId.getBytes(ZMQ.CHARSET));
            dealerSocket.connect(marketplaceEndpoint);
            
            // Set up poller for non-blocking receive
            ZMQ.Poller poller = context.createPoller(1);
            poller.register(dealerSocket, ZMQ.Poller.POLLIN);
            
            System.out.println("Seller " + sellerId + " connected and ready for requests");
            
            while (running && !Thread.currentThread().isInterrupted()) {
                // Poll for messages with timeout
                if (poller.poll(1000) > 0) {
                    if (poller.pollin(0)) {
                        processIncomingMessage(dealerSocket);
                    }
                }
                
                // Send heartbeat periodically
                sendHeartbeat(dealerSocket);
            }
            
            poller.close();
        } catch (Exception e) {
            System.err.println("Seller " + sellerId + " crashed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
    
    private void processIncomingMessage(ZMQ.Socket dealerSocket) {
        try {
            // Receive multipart message [empty, message]
            byte[] empty = dealerSocket.recv();
            byte[] messageBytes = dealerSocket.recv();
            
            if (messageBytes != null) {
                String jsonRequest = new String(messageBytes, ZMQ.CHARSET);
                System.out.println("\nReceived request: " + jsonRequest);
                
                String jsonResponse = processRequest(jsonRequest);
                
                // Send response back [empty, response]
                dealerSocket.send("", ZMQ.SNDMORE);
                dealerSocket.send(jsonResponse.getBytes(ZMQ.CHARSET), 0);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
    
    private long lastHeartbeat = 0;
    private void sendHeartbeat(ZMQ.Socket dealerSocket) {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeat > 30000) { // Send heartbeat every 30 seconds
            try {
                Message heartbeat = new Message();
                heartbeat.setType(Message.Type.HEARTBEAT);
                heartbeat.setSellerId(sellerId);
                
                String heartbeatJson = gson.toJson(heartbeat);
                dealerSocket.send("", ZMQ.SNDMORE);
                dealerSocket.send(heartbeatJson.getBytes(ZMQ.CHARSET), 0);
                
                lastHeartbeat = now;
            } catch (Exception e) {
                System.err.println("Error sending heartbeat: " + e.getMessage());
            }
        }
    }
    
    private String processRequest(String jsonRequest) {
        try {
            Message request = gson.fromJson(jsonRequest, Message.class);
            
            // Handle heartbeat messages
            if (request.getType() == Message.Type.HEARTBEAT) {
                Message response = new Message();
                response.setType(Message.Type.HEARTBEAT);
                response.setSellerId(sellerId);
                response.setSuccess(true);
                response.setCorrelationId(request.getCorrelationId());
                response.setMessageId(request.getMessageId());
                return gson.toJson(response);
            }
            
            // Check for idempotency - if we already processed this message, return cached result
            if (request.getMessageId() != null && idempotencyManager.isAlreadyProcessed(request.getMessageId())) {
                System.out.println("Request " + request.getMessageId() + " already processed, returning cached result");
                return idempotencyManager.getProcessedResult(request.getMessageId());
            }
            
            // Check for various failure scenarios
            AdvancedFailureSimulator.FailureDecision noResponseDecision = 
                failureSimulator.shouldSimulateFailure("no_response");
            if (noResponseDecision.shouldFail()) {
                System.out.println("Simulating no response: " + noResponseDecision.getReason());
                Message response = new Message();
                response.setSuccess(false);
                response.setReason(noResponseDecision.getReason());
                response.setCorrelationId(request.getCorrelationId());
                response.setMessageId(request.getMessageId());
                return gson.toJson(response);
            }
            
            // Check for slow response simulation
            AdvancedFailureSimulator.FailureDecision slowResponseDecision = 
                failureSimulator.shouldSimulateFailure("slow_response");
            if (slowResponseDecision.shouldFail()) {
                System.out.println("Simulating slow response: " + slowResponseDecision.getReason());
                try {
                    Thread.sleep(slowResponseDecision.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // Normal processing delay
                try {
                    Thread.sleep(Integer.parseInt(loadConfig().getProperty("seller.processing.delay.ms", "200")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Check for processing failure
            AdvancedFailureSimulator.FailureDecision processingFailureDecision = 
                failureSimulator.shouldSimulateFailure("processing_failure");
            if (processingFailureDecision.shouldFail()) {
                System.out.println("Simulating processing failure: " + processingFailureDecision.getReason());
                Message response = new Message();
                response.setSuccess(false);
                response.setReason(processingFailureDecision.getReason());
                response.setCorrelationId(request.getCorrelationId());
                response.setMessageId(request.getMessageId());
                return gson.toJson(response);
            }
            
            // Process based on message type
            Message response = null;
            
            switch (request.getType()) {
                case RESERVE:
                    response = handleReserve(request);
                    break;
                case CONFIRM:
                    response = handleConfirm(request);
                    break;
                case CANCEL:
                    response = handleCancel(request);
                    break;
                default:
                    response = createErrorResponse("Unknown message type");
                    response.setCorrelationId(request.getCorrelationId());
                    response.setMessageId(request.getMessageId());
            }
            
            // Ensure response has correlation info
            if (response != null) {
                response.setCorrelationId(request.getCorrelationId());
                response.setMessageId(request.getMessageId());
            }
            
            String responseJson = gson.toJson(response);
            
            // Cache the result for idempotency (only if message has an ID)
            if (request.getMessageId() != null) {
                idempotencyManager.markAsProcessed(request.getMessageId(), responseJson);
            }
            
            // Report success to failure simulator for pattern learning
            if (response != null && response.isSuccess()) {
                failureSimulator.reportSuccess();
            }
            
            return responseJson;
            
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace();
            Message errorResponse = createErrorResponse("Internal processing error: " + e.getMessage());
            return gson.toJson(errorResponse);
        }
    }
    
    private Message handleReserve(Message request) {
        Message response = new Message();
        response.setType(Message.Type.RESERVE);
        response.setOrderId(request.getOrderId());
        response.setProductId(request.getProductId());
        response.setQuantity(request.getQuantity());
        response.setSellerId(sellerId);
        
        // Check for out of stock simulation
        AdvancedFailureSimulator.FailureDecision outOfStockDecision = 
            failureSimulator.shouldSimulateFailure("out_of_stock");
        if (outOfStockDecision.shouldFail()) {
            System.out.println("Simulating out of stock: " + outOfStockDecision.getReason());
            response.setSuccess(false);
            response.setReason(outOfStockDecision.getReason());
            return response;
        }
        
        String reservationId = inventory.reserve(request.getProductId(), request.getQuantity());
        
        if (reservationId != null) {
            response.setSuccess(true);
            response.setReservationId(reservationId);
            System.out.println("Reserved: " + request.getQuantity() + "x " +
                               request.getProductId() + " (ID: " + reservationId + ")");
        } else {
            response.setSuccess(false);
            response.setReason("Insufficient stock");
            System.out.println("Reservation failed: Insufficient stock");
        }
        
        return response;
    }
    
    private Message handleConfirm(Message request) {
        Message response = new Message();
        response.setType(Message.Type.CONFIRM);
        response.setOrderId(request.getOrderId());
        
        boolean confirmed = inventory.confirm(request.getReservationId());
        response.setSuccess(confirmed);
        
        if (confirmed) {
            System.out.println("Confirmed reservation: " + request.getReservationId());
        } else {
            response.setReason("Reservation not found or already confirmed");
            System.out.println("Confirmation failed: " + response.getReason());
        }
        
        return response;
    }
    
    private Message handleCancel(Message request) {
        Message response = new Message();
        response.setType(Message.Type.CANCEL);
        response.setOrderId(request.getOrderId());
        
        boolean cancelled = inventory.cancel(request.getReservationId());
        response.setSuccess(cancelled);
        
        if (cancelled) {
            System.out.println("Cancelled reservation: " + request.getReservationId());
        } else {
            response.setReason("Reservation not found or already cancelled/confirmed");
            System.out.println("Cancellation failed: " + response.getReason());
        }
        
        return response;
    }
    
    private Message createErrorResponse(String reason) {
        Message response = new Message();
        response.setSuccess(false);
        response.setReason(reason);
        return response;
    }
    
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(CONFIG_FILE)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Could not load config, using defaults: " + e.getMessage());
            // Defaults
            props.setProperty("seller.inventory.size", "100");
            props.setProperty("seller.processing.delay.ms", "200");
            props.setProperty("failure.no.response", "0.05");
            props.setProperty("failure.processing", "0.10");
        }
        return props;
    }
    
    public void shutdown() {
        running = false;
        if (idempotencyManager != null) {
            idempotencyManager.shutdown();
        }
        if (inventory != null) {
            inventory.shutdown();
        }
        System.out.println("Seller " + sellerId + " shutting down...");
    }
}