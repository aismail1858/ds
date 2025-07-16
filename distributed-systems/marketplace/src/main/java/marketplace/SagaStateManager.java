package marketplace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.SagaState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages saga state persistence and recovery for distributed transactions.
 * Ensures saga durability across system failures and restarts.
 */
public class SagaStateManager {
    private final Map<String, SagaSnapshot> sagaSnapshots = new ConcurrentHashMap<>();
    private final ScheduledExecutorService persistenceExecutor = Executors.newSingleThreadScheduledExecutor();
    private final String stateDirectory;
    private final Gson gson;
    private final long persistenceIntervalMs;
    
    /**
     * Creates a saga state manager with default settings.
     * @param stateDirectory Directory to store saga state files
     */
    public SagaStateManager(String stateDirectory) {
        this(stateDirectory, 10000); // 10 seconds default persistence interval
    }
    
    /**
     * Creates a saga state manager with custom settings.
     * @param stateDirectory Directory to store saga state files
     * @param persistenceIntervalMs Interval for periodic persistence in milliseconds
     */
    public SagaStateManager(String stateDirectory, long persistenceIntervalMs) {
        this.stateDirectory = stateDirectory;
        this.persistenceIntervalMs = persistenceIntervalMs;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create state directory if it doesn't exist
        File dir = new File(stateDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Recover existing saga states
        recoverSagaStates();
        
        // Start periodic persistence
        persistenceExecutor.scheduleAtFixedRate(
            this::persistAllStates, 
            persistenceIntervalMs, 
            persistenceIntervalMs, 
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("SagaStateManager initialized with " + sagaSnapshots.size() + " recovered sagas");
    }
    
    /**
     * Saves saga state immediately.
     * @param sagaId The saga identifier
     * @param snapshot The saga state snapshot
     */
    public void saveSagaState(String sagaId, SagaSnapshot snapshot) {
        sagaSnapshots.put(sagaId, snapshot);
        // Immediate persistence for critical state changes
        persistSagaState(sagaId, snapshot);
    }
    
    /**
     * Retrieves saga state.
     * @param sagaId The saga identifier
     * @return The saga state snapshot or null if not found
     */
    public SagaSnapshot getSagaState(String sagaId) {
        return sagaSnapshots.get(sagaId);
    }
    
    /**
     * Removes saga state from memory and disk.
     * @param sagaId The saga identifier
     */
    public void removeSagaState(String sagaId) {
        sagaSnapshots.remove(sagaId);
        File stateFile = new File(stateDirectory + "/" + sagaId + ".json");
        if (stateFile.exists()) {
            stateFile.delete();
        }
    }
    
    /**
     * Gets all active saga IDs.
     * @return List of active saga IDs
     */
    public List<String> getActiveSagaIds() {
        return new ArrayList<>(sagaSnapshots.keySet());
    }
    
    /**
     * Gets the count of active sagas.
     * @return Number of active sagas
     */
    public int getActiveSagaCount() {
        return sagaSnapshots.size();
    }
    
    /**
     * Persists a single saga state to disk.
     * @param sagaId The saga identifier
     * @param snapshot The saga state snapshot
     */
    private void persistSagaState(String sagaId, SagaSnapshot snapshot) {
        try {
            String json = gson.toJson(snapshot);
            Files.write(Paths.get(stateDirectory + "/" + sagaId + ".json"), 
                       json.getBytes(StandardCharsets.UTF_8));
            System.out.println("Persisted saga state: " + sagaId);
        } catch (IOException e) {
            System.err.println("Failed to persist saga state " + sagaId + ": " + e.getMessage());
        }
    }
    
    /**
     * Persists all saga states to disk.
     */
    private void persistAllStates() {
        int persistedCount = 0;
        for (Map.Entry<String, SagaSnapshot> entry : sagaSnapshots.entrySet()) {
            try {
                persistSagaState(entry.getKey(), entry.getValue());
                persistedCount++;
            } catch (Exception e) {
                System.err.println("Error persisting saga " + entry.getKey() + ": " + e.getMessage());
            }
        }
        if (persistedCount > 0) {
            System.out.println("Persisted " + persistedCount + " saga states");
        }
    }
    
    /**
     * Recovers saga states from disk on startup.
     */
    private void recoverSagaStates() {
        File directory = new File(stateDirectory);
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                try {
                    String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    SagaSnapshot snapshot = gson.fromJson(json, SagaSnapshot.class);
                    String sagaId = file.getName().replace(".json", "");
                    sagaSnapshots.put(sagaId, snapshot);
                    System.out.println("Recovered saga state: " + sagaId + " in state " + snapshot.getCurrentState());
                } catch (IOException e) {
                    System.err.println("Failed to recover saga state from " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Shuts down the saga state manager.
     */
    public void shutdown() {
        persistenceExecutor.shutdown();
        try {
            // Final persistence before shutdown
            persistAllStates();
            
            if (!persistenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                persistenceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("SagaStateManager shut down");
    }
    
    /**
     * Represents a snapshot of saga state for persistence.
     */
    public static class SagaSnapshot {
        private final String sagaId;
        private final String orderId;
        private final SagaState currentState;
        private final List<CompensationActionSnapshot> compensationActions;
        private final Map<String, String> reservationIds;
        private final long lastUpdated;
        private final long createdAt;
        
        public SagaSnapshot(String sagaId, String orderId, SagaState currentState,
                           List<CompensationActionSnapshot> compensationActions,
                           Map<String, String> reservationIds) {
            this.sagaId = sagaId;
            this.orderId = orderId;
            this.currentState = currentState;
            this.compensationActions = compensationActions != null ? compensationActions : new ArrayList<>();
            this.reservationIds = reservationIds != null ? reservationIds : new ConcurrentHashMap<>();
            this.lastUpdated = System.currentTimeMillis();
            this.createdAt = System.currentTimeMillis();
        }
        
        // Getters
        public String getSagaId() { return sagaId; }
        public String getOrderId() { return orderId; }
        public SagaState getCurrentState() { return currentState; }
        public List<CompensationActionSnapshot> getCompensationActions() { return compensationActions; }
        public Map<String, String> getReservationIds() { return reservationIds; }
        public long getLastUpdated() { return lastUpdated; }
        public long getCreatedAt() { return createdAt; }
        
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - lastUpdated > timeoutMs;
        }
        
        @Override
        public String toString() {
            return String.format("SagaSnapshot{sagaId='%s', orderId='%s', state=%s, compensations=%d, reservations=%d}", 
                               sagaId, orderId, currentState, compensationActions.size(), reservationIds.size());
        }
    }
    
    /**
     * Represents a compensation action for persistence.
     */
    public static class CompensationActionSnapshot {
        private final String sellerId;
        private final String reservationId;
        private final String actionType;
        private final long timestamp;
        
        public CompensationActionSnapshot(String sellerId, String reservationId, String actionType) {
            this.sellerId = sellerId;
            this.reservationId = reservationId;
            this.actionType = actionType;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getSellerId() { return sellerId; }
        public String getReservationId() { return reservationId; }
        public String getActionType() { return actionType; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("CompensationAction{sellerId='%s', reservationId='%s', actionType='%s'}", 
                               sellerId, reservationId, actionType);
        }
    }
}