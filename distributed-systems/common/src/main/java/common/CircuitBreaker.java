package common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker pattern implementation to prevent cascading failures.
 * Provides protection against repeated calls to failing services.
 */
public class CircuitBreaker {
    
    /**
     * Circuit breaker states.
     */
    public enum State { 
        CLOSED,    // Normal operation
        OPEN,      // Failing fast
        HALF_OPEN  // Testing if service recovered
    }
    
    private final int failureThreshold;
    private final long timeoutMs;
    private final int successThreshold;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final String name;
    
    /**
     * Creates a circuit breaker with default settings.
     * @param name Name for logging purposes
     */
    public CircuitBreaker(String name) {
        this(name, 5, 30000, 3);
    }
    
    /**
     * Creates a circuit breaker with custom settings.
     * @param name Name for logging purposes
     * @param failureThreshold Number of failures before opening circuit
     * @param timeoutMs Time to wait before trying again when circuit is open
     * @param successThreshold Number of successes needed to close circuit from half-open
     */
    public CircuitBreaker(String name, int failureThreshold, long timeoutMs, int successThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.successThreshold = successThreshold;
    }
    
    /**
     * Executes an operation through the circuit breaker.
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @return CompletableFuture with the operation result
     */
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation, String operationName) {
        State currentState = state.get();
        
        if (currentState == State.OPEN) {
            if (shouldAttemptReset()) {
                return attemptReset(operation, operationName);
            } else {
                return CompletableFuture.failedFuture(
                    new RuntimeException("Circuit breaker is OPEN for " + name + " - " + operationName)
                );
            }
        }
        
        return executeOperation(operation, operationName);
    }
    
    /**
     * Executes the operation and handles the result.
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @return CompletableFuture with the operation result
     */
    private <T> CompletableFuture<T> executeOperation(Supplier<CompletableFuture<T>> operation, String operationName) {
        try {
            return operation.get()
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        onFailure(operationName, exception);
                    } else {
                        onSuccess(operationName);
                    }
                });
        } catch (Exception e) {
            onFailure(operationName, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Attempts to reset the circuit breaker from OPEN to HALF_OPEN state.
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @return CompletableFuture with the operation result
     */
    private <T> CompletableFuture<T> attemptReset(Supplier<CompletableFuture<T>> operation, String operationName) {
        if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
            successCount.set(0);
            System.out.println("Circuit breaker for " + name + " moved to HALF_OPEN state");
            return executeOperation(operation, operationName);
        } else {
            // Another thread already moved to HALF_OPEN
            return executeOperation(operation, operationName);
        }
    }
    
    /**
     * Checks if enough time has passed to attempt resetting the circuit breaker.
     * @return true if reset should be attempted
     */
    private boolean shouldAttemptReset() {
        return System.currentTimeMillis() - lastFailureTime.get() > timeoutMs;
    }
    
    /**
     * Handles successful operation execution.
     * @param operationName Name for logging purposes
     */
    private void onSuccess(String operationName) {
        failureCount.set(0);
        
        if (state.get() == State.HALF_OPEN) {
            int currentSuccessCount = successCount.incrementAndGet();
            if (currentSuccessCount >= successThreshold) {
                state.set(State.CLOSED);
                System.out.println("Circuit breaker for " + name + " moved to CLOSED state after " + 
                                 currentSuccessCount + " successful operations");
            }
        }
    }
    
    /**
     * Handles failed operation execution.
     * @param operationName Name for logging purposes
     * @param exception The exception that occurred
     */
    private void onFailure(String operationName, Throwable exception) {
        int currentFailureCount = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        System.out.println("Circuit breaker for " + name + " recorded failure " + currentFailureCount + 
                         "/" + failureThreshold + " for operation " + operationName + 
                         ": " + exception.getMessage());
        
        if (currentFailureCount >= failureThreshold) {
            state.set(State.OPEN);
            System.out.println("Circuit breaker for " + name + " moved to OPEN state after " + 
                             currentFailureCount + " failures");
        }
    }
    
    /**
     * Gets the current state of the circuit breaker.
     * @return Current state
     */
    public State getState() { 
        return state.get(); 
    }
    
    /**
     * Gets the current failure count.
     * @return Current failure count
     */
    public int getFailureCount() { 
        return failureCount.get(); 
    }
    
    /**
     * Gets the current success count (relevant in HALF_OPEN state).
     * @return Current success count
     */
    public int getSuccessCount() { 
        return successCount.get(); 
    }
    
    /**
     * Gets the name of the circuit breaker.
     * @return Circuit breaker name
     */
    public String getName() { 
        return name; 
    }
    
    /**
     * Resets the circuit breaker to CLOSED state.
     * Use with caution - primarily for testing or manual intervention.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
        System.out.println("Circuit breaker for " + name + " manually reset to CLOSED state");
    }
    
    /**
     * Gets circuit breaker statistics.
     * @return Statistics string
     */
    public String getStats() {
        return String.format("CircuitBreaker[%s]: State=%s, Failures=%d/%d, Successes=%d/%d", 
                           name, state.get(), failureCount.get(), failureThreshold, 
                           successCount.get(), successThreshold);
    }
}