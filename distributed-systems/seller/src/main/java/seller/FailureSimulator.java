package seller;

import java.util.Properties;
import java.util.Random;

public class FailureSimulator {
    private final Random random = new Random();
    private final double noResponseProbability;
    private final double processingFailureProbability;
    private final double outOfStockProbability;

    public FailureSimulator(Properties config) {
        this.noResponseProbability = Double.parseDouble(config.getProperty("failure.no.response", "0.0"));
        this.processingFailureProbability = Double.parseDouble(config.getProperty("failure.processing", "0.0"));
        this.outOfStockProbability = Double.parseDouble(config.getProperty("failure.out.of.stock", "0.0"));
    }

    public boolean shouldSimulateNoResponse() {
        return random.nextDouble() < noResponseProbability;
    }

    public boolean shouldSimulateProcessingFailure() {
        return random.nextDouble() < processingFailureProbability;
    }

    public boolean shouldSimulateOutOfStock() {
        return random.nextDouble() < outOfStockProbability;
    }
}
