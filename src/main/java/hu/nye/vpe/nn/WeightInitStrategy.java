package hu.nye.vpe.nn;

import java.util.Random;

/**
 * Weight initializations strategy.
 */
public enum WeightInitStrategy {
    RANDOM, XAVIER, HE, UNIFORM, ZERO;

    private static final Random random = new Random();

    /**
     * Initalize data.
     *
     * @param fanIn start
     *
     * @param fanOut end
     *
     * @param strategy initialization strategy
     *
     * @return weight data
     */
    public static double initialize(int fanIn, int fanOut, WeightInitStrategy strategy) {
        return switch (strategy) {
            case RANDOM -> random.nextGaussian() * 0.01;
            case XAVIER -> random.nextGaussian() * Math.sqrt(1.0 / (fanIn + fanOut));
            case HE -> random.nextGaussian() * Math.sqrt(2.0 / fanIn);
            case UNIFORM -> {
                double limit = Math.sqrt(6.0 / (fanIn + fanOut));
                yield random.nextDouble() * 2 * limit - limit;
            }
            case ZERO -> 0;
        };
    }
}
