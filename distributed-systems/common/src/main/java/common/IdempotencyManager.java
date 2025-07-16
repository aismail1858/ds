package common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages idempotency for distributed message processing.
 * Prevents duplicate message processing during retries and ensures exactly-once semantics.
 */
public class IdempotencyManager {
    private final Map<String, ProcessedMessage> processedMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    private final long retentionTimeMs;
    
    public IdempotencyManager() {
        this(TimeUnit.MINUTES.toMillis(30)); // Default 30 minutes retention
    }
    
    public IdempotencyManager(long retentionTimeMs) {
        this.retentionTimeMs = retentionTimeMs;
        // Clean expired entries every 5 minutes
        cleaner.scheduleAtFixedRate(this::cleanExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Checks if a message has already been processed.
     * @param messageId The unique message identifier
     * @return true if the message was already processed and hasn't expired
     */
    public boolean isAlreadyProcessed(String messageId) {
        ProcessedMessage processed = processedMessages.get(messageId);
        return processed != null && !processed.isExpired();
    }
    
    /**
     * Marks a message as processed and stores the result.
     * @param messageId The unique message identifier
     * @param result The processing result (JSON string)
     */
    public void markAsProcessed(String messageId, String result) {
        processedMessages.put(messageId, new ProcessedMessage(result, System.currentTimeMillis()));
    }
    
    /**
     * Retrieves the result of a previously processed message.
     * @param messageId The unique message identifier
     * @return The processing result or null if not found
     */
    public String getProcessedResult(String messageId) {
        ProcessedMessage processed = processedMessages.get(messageId);
        return processed != null && !processed.isExpired() ? processed.getResult() : null;
    }
    
    /**
     * Cleans up expired processed message entries.
     */
    private void cleanExpiredEntries() {
        long cutoff = System.currentTimeMillis() - retentionTimeMs;
        processedMessages.entrySet().removeIf(entry -> entry.getValue().getTimestamp() < cutoff);
        System.out.println("Cleaned up " + processedMessages.size() + " expired idempotency entries");
    }
    
    /**
     * Shuts down the cleanup scheduler.
     */
    public void shutdown() {
        cleaner.shutdown();
        try {
            if (!cleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Gets the current number of tracked processed messages.
     * @return The count of processed messages
     */
    public int getProcessedMessageCount() {
        return processedMessages.size();
    }
    
    /**
     * Represents a processed message with its result and timestamp.
     */
    private class ProcessedMessage {
        private final String result;
        private final long timestamp;
        
        public ProcessedMessage(String result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
        
        public String getResult() { 
            return result; 
        }
        
        public long getTimestamp() { 
            return timestamp; 
        }
        
        public boolean isExpired() { 
            return System.currentTimeMillis() - timestamp > retentionTimeMs;
        }
    }
}