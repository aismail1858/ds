package seller;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced failure simulator with realistic distributed system failure patterns.
 * Simulates various failure scenarios including cascading failures, burst failures,
 * and periodic maintenance windows.
 */
public class AdvancedFailureSimulator {
    private final Random random = new Random();
    private final Map<String, Double> failureProbabilities;
    private final Map<String, FailurePattern> patterns;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    
    /**
     * Creates an advanced failure simulator with configuration.
     * @param config Configuration properties
     */
    public AdvancedFailureSimulator(Properties config) {
        this.failureProbabilities = new HashMap<>();
        this.patterns = new HashMap<>();
        
        // Load failure probabilities
        failureProbabilities.put("no_response", 
            Double.parseDouble(config.getProperty("failure.no.response", "0.05")));
        failureProbabilities.put("processing_failure", 
            Double.parseDouble(config.getProperty("failure.processing", "0.10")));
        failureProbabilities.put("out_of_stock", 
            Double.parseDouble(config.getProperty("failure.out.of.stock", "0.15")));
        failureProbabilities.put("network_partition", 
            Double.parseDouble(config.getProperty("failure.network.partition", "0.02")));
        failureProbabilities.put("slow_response", 
            Double.parseDouble(config.getProperty("failure.slow.response", "0.20")));
        failureProbabilities.put("corruption", 
            Double.parseDouble(config.getProperty("failure.corruption", "0.01")));
        
        // Initialize failure patterns
        initializeFailurePatterns(config);
        
        System.out.println("Advanced failure simulator initialized with " + patterns.size() + " patterns");
    }
    
    /**
     * Initializes failure patterns based on configuration.
     * @param config Configuration properties
     */
    private void initializeFailurePatterns(Properties config) {
        // Cascading failure pattern - failures increase probability of more failures
        patterns.put("cascading", new CascadingFailurePattern(
            Double.parseDouble(config.getProperty("pattern.cascading.multiplier", "2.0")),
            Integer.parseInt(config.getProperty("pattern.cascading.max.consecutive", "5"))
        ));
        
        // Periodic failure pattern - simulates maintenance windows
        patterns.put("periodic", new PeriodicFailurePattern(
            Long.parseLong(config.getProperty("pattern.periodic.interval.ms", "3600000")), // 1 hour
            Long.parseLong(config.getProperty("pattern.periodic.duration.ms", "300000"))   // 5 minutes
        ));
        
        // Burst failure pattern - sudden spikes in failures
        patterns.put("burst", new BurstFailurePattern(
            Double.parseDouble(config.getProperty("pattern.burst.probability", "0.8")),
            Long.parseLong(config.getProperty("pattern.burst.duration.ms", "30000"))
        ));
        
        // Recovery pattern - gradual improvement after failures
        patterns.put("recovery", new RecoveryPattern(
            Double.parseDouble(config.getProperty("pattern.recovery.improvement.factor", "0.9")),
            Integer.parseInt(config.getProperty("pattern.recovery.success.threshold", "10"))
        ));
    }
    
    /**
     * Determines if a failure should be simulated for a given operation.
     * @param operationType The type of operation (e.g., "no_response", "processing_failure")
     * @return FailureDecision indicating if and how to fail
     */
    public FailureDecision shouldSimulateFailure(String operationType) {
        // Check patterns first
        for (FailurePattern pattern : patterns.values()) {
            if (pattern.isActive()) {
                double modifiedProbability = pattern.modifyProbability(
                    failureProbabilities.getOrDefault(operationType, 0.0));
                if (random.nextDouble() < modifiedProbability) {
                    recordFailure();
                    return createFailureDecision(operationType, pattern);
                }
            }
        }
        
        // Normal failure simulation
        double baseProbability = failureProbabilities.getOrDefault(operationType, 0.0);
        if (random.nextDouble() < baseProbability) {
            recordFailure();
            return createFailureDecision(operationType, null);
        }
        
        recordSuccess();
        return FailureDecision.NO_FAILURE;
    }
    
    /**
     * Creates a failure decision based on operation type and pattern.
     * @param operationType The type of operation
     * @param pattern The failure pattern (if any)
     * @return FailureDecision with appropriate configuration
     */
    private FailureDecision createFailureDecision(String operationType, FailurePattern pattern) {
        String patternInfo = pattern != null ? " (" + pattern.getName() + ")" : "";
        
        switch (operationType) {
            case "no_response":
                return new FailureDecision(FailureType.NO_RESPONSE, 
                    "Simulated no response" + patternInfo);
                    
            case "processing_failure":
                return new FailureDecision(FailureType.PROCESSING_FAILURE, 
                    "Simulated processing failure" + patternInfo);
                    
            case "slow_response":
                // Use normal distribution for realistic delay simulation
                long delay = (long) Math.max(100, 1000 + random.nextGaussian() * 2000);
                return new FailureDecision(FailureType.SLOW_RESPONSE, 
                    "Simulated slow response" + patternInfo, delay);
                    
            case "network_partition":
                long partitionDelay = 5000 + random.nextInt(10000);
                return new FailureDecision(FailureType.NETWORK_PARTITION, 
                    "Simulated network partition" + patternInfo, partitionDelay);
                    
            case "out_of_stock":
                return new FailureDecision(FailureType.OUT_OF_STOCK, 
                    "Simulated out of stock" + patternInfo);
                    
            case "corruption":
                return new FailureDecision(FailureType.CORRUPTION, 
                    "Simulated data corruption" + patternInfo);
                    
            default:
                return new FailureDecision(FailureType.PROCESSING_FAILURE, 
                    "Unknown failure type" + patternInfo);
        }
    }
    
    /**
     * Records a successful operation.
     */
    public void reportSuccess() {
        recordSuccess();
    }
    
    /**
     * Records a failure occurrence.
     */
    private void recordFailure() {
        consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        successCount.set(0);
    }
    
    /**
     * Records a successful operation.
     */
    private void recordSuccess() {
        successCount.incrementAndGet();
        // Reset consecutive failures after some successes
        if (successCount.get() > 5) {
            consecutiveFailures.set(0);
        }
    }
    
    /**
     * Gets current failure statistics.
     * @return Map of statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("consecutiveFailures", consecutiveFailures.get());
        stats.put("successCount", successCount.get());
        stats.put("lastFailureTime", lastFailureTime.get());
        
        Map<String, Object> patternStats = new HashMap<>();
        for (Map.Entry<String, FailurePattern> entry : patterns.entrySet()) {
            patternStats.put(entry.getKey(), entry.getValue().getStats());
        }
        stats.put("patterns", patternStats);
        
        return stats;
    }
    
    /**
     * Enumeration of failure types.
     */
    public enum FailureType {
        NO_FAILURE, NO_RESPONSE, PROCESSING_FAILURE, SLOW_RESPONSE, 
        NETWORK_PARTITION, OUT_OF_STOCK, CORRUPTION
    }
    
    /**
     * Represents a failure decision with type, reason, and optional delay.
     */
    public static class FailureDecision {
        public static final FailureDecision NO_FAILURE = new FailureDecision(FailureType.NO_FAILURE, "No failure");
        
        private final FailureType type;
        private final String reason;
        private final long delayMs;
        
        public FailureDecision(FailureType type, String reason) {
            this(type, reason, 0);
        }
        
        public FailureDecision(FailureType type, String reason, long delayMs) {
            this.type = type;
            this.reason = reason;
            this.delayMs = delayMs;
        }
        
        public FailureType getType() { return type; }
        public String getReason() { return reason; }
        public long getDelayMs() { return delayMs; }
        public boolean shouldFail() { return type != FailureType.NO_FAILURE; }
        
        @Override
        public String toString() {
            return String.format("FailureDecision{type=%s, reason='%s', delayMs=%d}", 
                               type, reason, delayMs);
        }
    }
    
    /**
     * Abstract base class for failure patterns.
     */
    private abstract static class FailurePattern {
        protected final String name;
        
        public FailurePattern(String name) {
            this.name = name;
        }
        
        public abstract boolean isActive();
        public abstract double modifyProbability(double baseProbability);
        public String getName() { return name; }
        
        public Map<String, Object> getStats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", name);
            stats.put("active", isActive());
            return stats;
        }
    }
    
    /**
     * Cascading failure pattern - failures increase probability of more failures.
     */
    private class CascadingFailurePattern extends FailurePattern {
        private final double multiplier;
        private final int maxConsecutive;
        
        public CascadingFailurePattern(double multiplier, int maxConsecutive) {
            super("cascading");
            this.multiplier = multiplier;
            this.maxConsecutive = maxConsecutive;
        }
        
        @Override
        public boolean isActive() {
            return consecutiveFailures.get() > 0;
        }
        
        @Override
        public double modifyProbability(double baseProbability) {
            int failures = Math.min(consecutiveFailures.get(), maxConsecutive);
            return Math.min(0.9, baseProbability * Math.pow(multiplier, failures));
        }
        
        @Override
        public Map<String, Object> getStats() {
            Map<String, Object> stats = super.getStats();
            stats.put("consecutiveFailures", consecutiveFailures.get());
            stats.put("multiplier", multiplier);
            return stats;
        }
    }
    
    /**
     * Periodic failure pattern - simulates maintenance windows.
     */
    private static class PeriodicFailurePattern extends FailurePattern {
        private final long intervalMs;
        private final long durationMs;
        
        public PeriodicFailurePattern(long intervalMs, long durationMs) {
            super("periodic");
            this.intervalMs = intervalMs;
            this.durationMs = durationMs;
        }
        
        @Override
        public boolean isActive() {
            long currentTime = System.currentTimeMillis();
            long phase = currentTime % intervalMs;
            return phase < durationMs;
        }
        
        @Override
        public double modifyProbability(double baseProbability) {
            return Math.min(0.9, baseProbability * 5.0); // 5x higher during maintenance
        }
        
        @Override
        public Map<String, Object> getStats() {
            Map<String, Object> stats = super.getStats();
            stats.put("intervalMs", intervalMs);
            stats.put("durationMs", durationMs);
            return stats;
        }
    }
    
    /**
     * Burst failure pattern - sudden spikes in failures.
     */
    private static class BurstFailurePattern extends FailurePattern {
        private final double burstProbability;
        private final long burstDurationMs;
        private volatile long burstStartTime = 0;
        
        public BurstFailurePattern(double burstProbability, long burstDurationMs) {
            super("burst");
            this.burstProbability = burstProbability;
            this.burstDurationMs = burstDurationMs;
        }
        
        @Override
        public boolean isActive() {
            long currentTime = System.currentTimeMillis();
            
            // Check if we're in a burst period
            if (burstStartTime > 0 && currentTime - burstStartTime < burstDurationMs) {
                return true;
            }
            
            // Check if we should start a new burst
            if (burstStartTime == 0 || currentTime - burstStartTime > burstDurationMs * 10) {
                if (new Random().nextDouble() < 0.01) { // 1% chance to start burst
                    burstStartTime = currentTime;
                    return true;
                }
            }
            
            return false;
        }
        
        @Override
        public double modifyProbability(double baseProbability) {
            return burstProbability;
        }
        
        @Override
        public Map<String, Object> getStats() {
            Map<String, Object> stats = super.getStats();
            stats.put("burstProbability", burstProbability);
            stats.put("burstStartTime", burstStartTime);
            return stats;
        }
    }
    
    /**
     * Recovery pattern - gradual improvement after failures.
     */
    private class RecoveryPattern extends FailurePattern {
        private final double improvementFactor;
        private final int successThreshold;
        
        public RecoveryPattern(double improvementFactor, int successThreshold) {
            super("recovery");
            this.improvementFactor = improvementFactor;
            this.successThreshold = successThreshold;
        }
        
        @Override
        public boolean isActive() {
            return consecutiveFailures.get() > 0 && successCount.get() < successThreshold;
        }
        
        @Override
        public double modifyProbability(double baseProbability) {
            // Gradually improve success rate
            int successes = Math.min(successCount.get(), successThreshold);
            double factor = Math.pow(improvementFactor, successes);
            return Math.max(0.01, baseProbability * factor);
        }
        
        @Override
        public Map<String, Object> getStats() {
            Map<String, Object> stats = super.getStats();
            stats.put("successCount", successCount.get());
            stats.put("improvementFactor", improvementFactor);
            return stats;
        }
    }
}