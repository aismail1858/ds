package common;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages retry logic with exponential backoff for distributed operations.
 * Provides sophisticated retry mechanisms to handle transient failures.
 */
public class RetryManager {
    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    /**
     * Creates a retry manager with default settings.
     */
    public RetryManager() {
        this(3, 1000, 2.0, 30000);
    }
    
    /**
     * Creates a retry manager with custom settings.
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelayMs Base delay between retries in milliseconds
     * @param backoffMultiplier Multiplier for exponential backoff
     * @param maxDelayMs Maximum delay between retries in milliseconds
     */
    public RetryManager(int maxRetries, long baseDelayMs, double backoffMultiplier, long maxDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
    }
    
    /**
     * Executes an operation with retry logic.
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @return CompletableFuture with the operation result
     */
    public <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> operation, 
                                                     String operationName) {
        return executeWithRetry(operation, operationName, 0);
    }
    
    /**
     * Internal method to execute operation with retry logic.
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @param attemptNumber Current attempt number
     * @return CompletableFuture with the operation result
     */
    private <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> operation, 
                                                      String operationName, int attemptNumber) {
        CompletableFuture<T> result = new CompletableFuture<>();
        
        try {
            operation.get()
                .whenComplete((value, exception) -> {
                    if (exception != null) {
                        handleFailure(operation, operationName, attemptNumber, exception, result);
                    } else {
                        result.complete(value);
                    }
                });
        } catch (Exception e) {
            handleFailure(operation, operationName, attemptNumber, e, result);
        }
        
        return result;
    }
    
    /**
     * Handles operation failures and decides whether to retry.
     * @param operation The operation to potentially retry
     * @param operationName Name for logging purposes
     * @param attemptNumber Current attempt number
     * @param exception The exception that occurred
     * @param result The result future to complete
     */
    private <T> void handleFailure(Supplier<CompletableFuture<T>> operation, 
                                   String operationName, 
                                   int attemptNumber, 
                                   Throwable exception, 
                                   CompletableFuture<T> result) {
        if (attemptNumber < maxRetries && isRetryableException(exception)) {
            long delay = calculateDelay(attemptNumber);
            System.out.println(String.format(
                "Retry %d/%d for %s after %dms delay. Error: %s", 
                attemptNumber + 1, maxRetries, operationName, delay, exception.getMessage()
            ));
            
            scheduler.schedule(() -> {
                executeWithRetry(operation, operationName, attemptNumber + 1)
                    .whenComplete((value, retryException) -> {
                        if (retryException != null) {
                            result.completeExceptionally(retryException);
                        } else {
                            result.complete(value);
                        }
                    });
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            System.err.println(String.format(
                "Operation %s failed after %d attempts. Final error: %s", 
                operationName, attemptNumber + 1, exception.getMessage()
            ));
            result.completeExceptionally(exception);
        }
    }
    
    /**
     * Calculates delay for the next retry attempt using exponential backoff with jitter.
     * @param attemptNumber Current attempt number
     * @return Delay in milliseconds
     */
    private long calculateDelay(int attemptNumber) {
        long delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attemptNumber));
        delay = Math.min(delay, maxDelayMs);
        
        // Add jitter to prevent thundering herd problem
        double jitter = random.nextGaussian() * 0.1; // 10% jitter
        delay += (long) (delay * jitter);
        
        return Math.max(delay, 0);
    }
    
    /**
     * Determines if an exception is retryable.
     * @param exception The exception to check
     * @return true if the exception is retryable
     */
    private boolean isRetryableException(Throwable exception) {
        // Consider timeout exceptions and runtime exceptions as retryable
        // But not illegal state exceptions or validation errors
        return !(exception instanceof IllegalStateException || 
                exception instanceof IllegalArgumentException ||
                exception instanceof NullPointerException);
    }
    
    /**
     * Shuts down the retry manager.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Gets the maximum number of retries configured.
     * @return Maximum retry attempts
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * Gets the base delay configured.
     * @return Base delay in milliseconds
     */
    public long getBaseDelayMs() {
        return baseDelayMs;
    }
}