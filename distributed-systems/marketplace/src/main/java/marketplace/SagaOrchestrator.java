package marketplace;

import common.Message;
import common.OrderStatus;
import common.SagaState;
import common.RetryManager;
import common.CircuitBreaker;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class SagaOrchestrator {
    private final String marketplaceId;
    private final AsyncMessageBroker messageBroker;
    private final ExecutorService sagaExecutor;
    private final int sagaTimeoutSeconds;
    private final RetryManager retryManager;
    private final SagaStateManager stateManager;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    // Active sagas tracking
    private final Map<String, SagaInstance> activeSagas = new ConcurrentHashMap<>();
    
    public SagaOrchestrator(String marketplaceId, AsyncMessageBroker messageBroker, Properties config) {
        this.marketplaceId = marketplaceId;
        this.messageBroker = messageBroker;
        this.sagaTimeoutSeconds = Integer.parseInt(config.getProperty("saga.timeout.seconds", "60"));
        this.sagaExecutor = Executors.newFixedThreadPool(
            Integer.parseInt(config.getProperty("saga.processing.threads", "20"))
        );
        this.retryManager = new RetryManager(
            Integer.parseInt(config.getProperty("retry.max.attempts", "3")),
            Long.parseLong(config.getProperty("retry.base.delay.ms", "1000")),
            Double.parseDouble(config.getProperty("retry.backoff.multiplier", "2.0")),
            Long.parseLong(config.getProperty("retry.max.delay.ms", "30000"))
        );
        this.stateManager = new SagaStateManager(
            config.getProperty("saga.state.directory", "./saga-states")
        );
        
        // Recover any incomplete sagas on startup
        recoverIncompleteSagas();
        
        System.out.println("SagaOrchestrator initialized for " + marketplaceId);
    }
    
    private void recoverIncompleteSagas() {
        List<String> activeSagaIds = stateManager.getActiveSagaIds();
        for (String sagaId : activeSagaIds) {
            try {
                SagaStateManager.SagaSnapshot snapshot = stateManager.getSagaState(sagaId);
                if (snapshot != null && !isTerminalState(snapshot.getCurrentState())) {
                    // TODO: Implement saga recovery logic
                    System.out.println("Found incomplete saga to recover: " + sagaId + " in state " + snapshot.getCurrentState());
                }
            } catch (Exception e) {
                System.err.println("Error recovering saga " + sagaId + ": " + e.getMessage());
            }
        }
    }
    
    private boolean isTerminalState(SagaState state) {
        return state == SagaState.COMPLETED || state == SagaState.FAILED || state == SagaState.COMPENSATION_COMPLETED;
    }
    
    public Order processOrder(Order order) throws Exception {
        String sagaId = UUID.randomUUID().toString();
        SagaInstance saga = new SagaInstance(sagaId, order);
        activeSagas.put(sagaId, saga);
        
        // Save initial saga state
        stateManager.saveSagaState(sagaId, createSnapshot(saga));
        
        try {
            Order result = executeSaga(saga).get(sagaTimeoutSeconds, TimeUnit.SECONDS);
            
            // Clean up completed saga state
            if (result.getStatus() == OrderStatus.COMPLETED) {
                stateManager.removeSagaState(sagaId);
            }
            
            return result;
        } catch (TimeoutException e) {
            System.err.println("SAGA timeout for order " + order.getOrderId());
            compensateSaga(saga);
            order.setStatus(OrderStatus.FAILED);
            throw new RuntimeException("SAGA execution timeout", e);
        } finally {
            activeSagas.remove(sagaId);
        }
    }
    
    private CompletableFuture<Order> executeSaga(SagaInstance saga) {
        return CompletableFuture.supplyAsync(() -> {
            Order order = saga.getOrder();
            
            try {
                // Phase 1: Reserve all products
                if (!saga.transitionTo(SagaState.RESERVING_PRODUCTS)) {
                    throw new IllegalStateException("Cannot start reservation phase");
                }
                order.setStatus(OrderStatus.RESERVING_PRODUCTS);
                
                Map<String, CompletableFuture<ReservationResult>> reservationFutures = new HashMap<>();
                
                // Send all reservation requests in parallel
                for (Order.OrderItem item : order.getItems()) {
                    String correlationId = UUID.randomUUID().toString();
                    CompletableFuture<ReservationResult> future = reserveProduct(
                        item.getSellerId(), 
                        item.getProductId(), 
                        item.getQuantity(),
                        correlationId
                    );
                    reservationFutures.put(item.getProductId() + "@" + item.getSellerId(), future);
                }
                
                // Wait for all reservations to complete
                Map<String, ReservationResult> reservations = new HashMap<>();
                boolean allSuccessful = true;
                
                for (Map.Entry<String, CompletableFuture<ReservationResult>> entry : reservationFutures.entrySet()) {
                    try {
                        ReservationResult result = entry.getValue().get(10, TimeUnit.SECONDS);
                        reservations.put(entry.getKey(), result);
                        
                        if (!result.isSuccess()) {
                            allSuccessful = false;
                            System.out.println("Reservation failed for " + entry.getKey() + 
                                             ": " + result.getErrorMessage());
                        } else {
                            saga.addCompensationAction(new CancelReservationAction(
                                result.getSellerId(), 
                                result.getReservationId()
                            ));
                        }
                    } catch (Exception e) {
                        allSuccessful = false;
                        System.err.println("Error reserving " + entry.getKey() + ": " + e.getMessage());
                    }
                }
                
                if (!allSuccessful) {
                    throw new RuntimeException("Not all products could be reserved");
                }
                
                // Phase 2: Confirm all reservations
                if (!saga.transitionTo(SagaState.PRODUCTS_RESERVED)) {
                    throw new IllegalStateException("Cannot transition to products reserved");
                }
                order.setStatus(OrderStatus.ALL_RESERVED);
                
                if (!saga.transitionTo(SagaState.CONFIRMING_RESERVATIONS)) {
                    throw new IllegalStateException("Cannot start confirmation phase");
                }
                order.setStatus(OrderStatus.CONFIRMING_PRODUCTS);
                
                // Confirm all reservations in parallel
                List<CompletableFuture<Boolean>> confirmationFutures = new ArrayList<>();
                
                for (ReservationResult reservation : reservations.values()) {
                    if (reservation.isSuccess()) {
                        CompletableFuture<Boolean> confirmFuture = confirmReservation(
                            reservation.getSellerId(),
                            reservation.getReservationId()
                        );
                        confirmationFutures.add(confirmFuture);
                    }
                }
                
                // Wait for all confirmations
                CompletableFuture<Void> allConfirmations = CompletableFuture.allOf(
                    confirmationFutures.toArray(new CompletableFuture[0])
                );
                
                allConfirmations.get(10, TimeUnit.SECONDS);
                
                // Check if all confirmations succeeded
                boolean allConfirmed = confirmationFutures.stream()
                    .map(CompletableFuture::join)
                    .allMatch(Boolean::booleanValue);
                
                if (!allConfirmed) {
                    throw new RuntimeException("Not all reservations could be confirmed");
                }
                
                // Success!
                if (!saga.transitionTo(SagaState.COMPLETED)) {
                    throw new IllegalStateException("Cannot complete SAGA");
                }
                order.setStatus(OrderStatus.COMPLETED);
                
                return order;
                
            } catch (Exception e) {
                System.err.println("SAGA failed for order " + order.getOrderId() + ": " + e.getMessage());
                compensateSaga(saga);
                order.setStatus(OrderStatus.FAILED);
                throw new RuntimeException("SAGA execution failed", e);
            }
        }, sagaExecutor);
    }
    
    private void compensateSaga(SagaInstance saga) {
        if (!saga.transitionTo(SagaState.COMPENSATING)) {
            System.err.println("Cannot start compensation for SAGA " + saga.getSagaId());
            return;
        }
        
        saga.getOrder().setStatus(OrderStatus.COMPENSATING);
        
        List<CompensationAction> actions = saga.getCompensationActions();
        Collections.reverse(actions); // Execute in reverse order
        
        for (CompensationAction action : actions) {
            try {
                action.execute(messageBroker).get(5, TimeUnit.SECONDS);
                System.out.println("Compensation executed: " + action.getDescription());
            } catch (Exception e) {
                System.err.println("Compensation failed: " + action.getDescription() + 
                                 " - " + e.getMessage());
            }
        }
        
        saga.transitionTo(SagaState.COMPENSATION_COMPLETED);
        saga.getOrder().setStatus(OrderStatus.CANCELLED);
    }
    
    private CompletableFuture<ReservationResult> reserveProduct(String sellerId, String productId, 
                                                               int quantity, String correlationId) {
        Message request = new Message();
        request.setType("RESERVE");
        request.setData(Map.of(
            "productId", productId,
            "quantity", String.valueOf(quantity)
        ));
        request.setCorrelationId(correlationId);
        request.setSenderId(marketplaceId);
        
        return messageBroker.sendAsyncRequestWithRetry(sellerId, request, 
                "Reserve " + quantity + "x " + productId + " from " + sellerId)
            .thenApply(response -> {
                if (response != null && "SUCCESS".equals(response.getType())) {
                    return new ReservationResult(
                        true,
                        sellerId,
                        response.getData().get("reservationId"),
                        null
                    );
                } else {
                    String error = response != null ? 
                        response.getData().getOrDefault("error", "Unknown error") : 
                        "No response";
                    return new ReservationResult(false, sellerId, null, error);
                }
            });
    }
    
    private CompletableFuture<Boolean> confirmReservation(String sellerId, String reservationId) {
        Message request = new Message();
        request.setType("CONFIRM");
        request.setData(Map.of("reservationId", reservationId));
        request.setSenderId(marketplaceId);
        
        return messageBroker.sendAsyncRequestWithRetry(sellerId, request, 
                "Confirm reservation " + reservationId + " from " + sellerId)
            .thenApply(response -> response != null && "SUCCESS".equals(response.getType()));
    }
    
    public void shutdown() {
        sagaExecutor.shutdown();
        retryManager.shutdown();
        stateManager.shutdown();
        
        try {
            if (!sagaExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                sagaExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("SagaOrchestrator shut down");
    }
    
    private SagaStateManager.SagaSnapshot createSnapshot(SagaInstance saga) {
        List<SagaStateManager.CompensationActionSnapshot> actionSnapshots = new ArrayList<>();
        
        for (CompensationAction action : saga.getCompensationActions()) {
            if (action instanceof CancelReservationAction) {
                CancelReservationAction cancelAction = (CancelReservationAction) action;
                actionSnapshots.add(new SagaStateManager.CompensationActionSnapshot(
                    cancelAction.sellerId, 
                    cancelAction.reservationId, 
                    "CANCEL"
                ));
            }
        }
        
        return new SagaStateManager.SagaSnapshot(
            saga.getSagaId(),
            saga.getOrder().getOrderId(),
            saga.getState(),
            actionSnapshots,
            saga.getReservationIds()
        );
    }
    
    public int getActiveSagaCount() {
        return activeSagas.size();
    }
    
    public Map<String, String> getCircuitBreakerStats() {
        Map<String, String> stats = new HashMap<>();
        circuitBreakers.forEach((sellerId, cb) -> stats.put(sellerId, cb.getStats()));
        return stats;
    }
    
    // Inner classes
    private static class SagaInstance {
        private final String sagaId;
        private final Order order;
        private final AtomicReference<SagaState> state = new AtomicReference<>(SagaState.STARTED);
        private final List<CompensationAction> compensationActions = new CopyOnWriteArrayList<>();
        private final Map<String, String> reservationIds = new ConcurrentHashMap<>();
        
        public SagaInstance(String sagaId, Order order) {
            this.sagaId = sagaId;
            this.order = order;
        }
        
        public boolean transitionTo(SagaState newState) {
            SagaState currentState = state.get();
            if (currentState.canTransitionTo(newState)) {
                return state.compareAndSet(currentState, newState);
            }
            return false;
        }
        
        public void addCompensationAction(CompensationAction action) {
            compensationActions.add(action);
        }
        
        public void addReservation(String sellerId, String reservationId) {
            reservationIds.put(sellerId, reservationId);
        }
        
        public String getSagaId() { return sagaId; }
        public Order getOrder() { return order; }
        public SagaState getState() { return state.get(); }
        public List<CompensationAction> getCompensationActions() { 
            return new ArrayList<>(compensationActions); 
        }
        public Map<String, String> getReservationIds() {
            return new HashMap<>(reservationIds);
        }
    }
    
    private interface CompensationAction {
        CompletableFuture<Void> execute(AsyncMessageBroker broker);
        String getDescription();
    }
    
    private class CancelReservationAction implements CompensationAction {
        private final String sellerId;
        private final String reservationId;
        
        public CancelReservationAction(String sellerId, String reservationId) {
            this.sellerId = sellerId;
            this.reservationId = reservationId;
        }
        
        @Override
        public CompletableFuture<Void> execute(AsyncMessageBroker broker) {
            Message request = new Message();
            request.setType("CANCEL");
            request.setData(Map.of("reservationId", reservationId));
            request.setSenderId(marketplaceId);
            
            return broker.sendAsyncRequestWithRetry(sellerId, request, 
                    "Cancel reservation " + reservationId + " from " + sellerId)
                .thenAccept(response -> {
                    if (response == null || !"SUCCESS".equals(response.getType())) {
                        System.err.println("Failed to cancel reservation " + reservationId);
                    } else {
                        System.out.println("Successfully cancelled reservation " + reservationId);
                    }
                });
        }
        
        @Override
        public String getDescription() {
            return "Cancel reservation " + reservationId + " at " + sellerId;
        }
        
        // Expose fields for snapshot creation
        public String getSellerId() { return sellerId; }
        public String getReservationId() { return reservationId; }
    }
    
    private static class ReservationResult {
        private final boolean success;
        private final String sellerId;
        private final String reservationId;
        private final String errorMessage;
        
        public ReservationResult(boolean success, String sellerId, String reservationId, String errorMessage) {
            this.success = success;
            this.sellerId = sellerId;
            this.reservationId = reservationId;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getSellerId() { return sellerId; }
        public String getReservationId() { return reservationId; }
        public String getErrorMessage() { return errorMessage; }
    }
}
