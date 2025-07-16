package marketplace;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.OrderStatus;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderProcessor {
    private final String marketplaceId;
    private final List<Order> orders;
    private final int orderDelayMs;
    private final Properties config;
    private final Gson gson;
    
    private final ExecutorService orderExecutor;
    private final ScheduledExecutorService scheduler;
    private final SagaOrchestrator sagaOrchestrator;
    private final AsyncMessageBroker messageBroker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public OrderProcessor(Properties config) {
        this.config = config;
        this.marketplaceId = config.getProperty("marketplace.id", "marketplace1");
        this.orderDelayMs = Integer.parseInt(config.getProperty("order.delay.ms", "5000"));
        this.gson = new Gson();
        
        // Initialize thread pools
        this.orderExecutor = Executors.newFixedThreadPool(
            Integer.parseInt(config.getProperty("order.processing.threads", "10"))
        );
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize components
        this.messageBroker = new AsyncMessageBroker(config);
        this.sagaOrchestrator = new SagaOrchestrator(marketplaceId, messageBroker, config);
        
        // Load orders
        this.orders = loadOrders();
    }
    
    private List<Order> loadOrders() {
        try (FileReader reader = new FileReader("orders.json")) {
            Type listType = new TypeToken<List<Order>>(){}.getType();
            List<Order> loadedOrders = gson.fromJson(reader, listType);
            return loadedOrders != null ? loadedOrders : generateDefaultOrders();
        } catch (IOException e) {
            System.out.println("Could not load orders.json, generating default orders: " + e.getMessage());
            return generateDefaultOrders();
        }
    }
    
    private List<Order> generateDefaultOrders() {
        List<Order> defaultOrders = new ArrayList<>();
        
        // Generate diverse test orders
        for (int i = 1; i <= 10; i++) {
            Order order = new Order("O" + i, "customer" + i, marketplaceId);
            
            // Add random products
            int productCount = 1 + (i % 3);
            for (int j = 0; j < productCount; j++) {
                int sellerId = 1 + ((i + j) % 5);
                int productId = 1 + ((i * j) % 3);
                int quantity = 1 + (i % 4);
                
                order.addItem("P" + productId, quantity, "seller" + sellerId);
            }
            
            defaultOrders.add(order);
        }
        
        return defaultOrders;
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            System.out.println("Order processor started for " + marketplaceId);
            
            // Start message broker
            messageBroker.start();
            
            // Schedule order processing
            scheduleOrderProcessing();
        }
    }
    
    private void scheduleOrderProcessing() {
        AtomicInteger orderIndex = new AtomicInteger(0);
        
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            
            int index = orderIndex.getAndIncrement();
            if (index < orders.size()) {
                Order order = orders.get(index);
                processOrderAsync(order);
            } else {
                System.out.println("All orders submitted for processing.");
            }
        }, 1000, orderDelayMs, TimeUnit.MILLISECONDS);
    }
    
    private void processOrderAsync(Order order) {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("\n=== Submitting Order " + order.getOrderId() + " for processing ===");
            order.setStatus(OrderStatus.CREATED);
            
            try {
                return sagaOrchestrator.processOrder(order);
            } catch (Exception e) {
                System.err.println("Error processing order " + order.getOrderId() + ": " + e.getMessage());
                order.setStatus(OrderStatus.FAILED);
                return order;
            }
        }, orderExecutor).thenAccept(processedOrder -> {
            System.out.println("Order " + processedOrder.getOrderId() + 
                             " completed with status: " + processedOrder.getStatus());
        });
    }
    
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            System.out.println("Shutting down order processor...");
            
            // Shutdown schedulers
            scheduler.shutdown();
            orderExecutor.shutdown();
            
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                if (!orderExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    orderExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Shutdown components
            sagaOrchestrator.shutdown();
            messageBroker.shutdown();
        }
    }
}
