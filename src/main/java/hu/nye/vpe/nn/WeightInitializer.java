package hu.nye.vpe.nn;

import java.util.Random;

/**
 * Weight initalizer class.
 */
public class WeightInitializer {
    private static final Random RANDOM = new Random();

    /**
     * Initialize weights.
     *
     * @param inputSize input size
     *
     * @param outputSize output size
     *
     * @param strategy strategy of weight initialization
     *
     * @return weight
     */
    public static double[] initializeWeights(int inputSize, int outputSize, WeightInitStrategy strategy) {
        double[] weights = new double[inputSize];
        for (int i = 0; i < inputSize; i++) {
            weights[i] = initializeWeight(inputSize, outputSize, strategy);
        }
        return weights;
    }

    private static double initializeWeight(int inputSize, int outputSize, WeightInitStrategy strategy) {
        // Súly inicializálás a stratégia alapján
        return switch (strategy) {
            case RANDOM -> RANDOM.nextGaussian() * 0.01; // Véletlenszerű kis értékek
            case XAVIER -> RANDOM.nextGaussian() * Math.sqrt(2.0 / (inputSize + outputSize));
            case HE -> RANDOM.nextGaussian() * Math.sqrt(2.0 / inputSize);
            case UNIFORM -> {
                double limit = Math.sqrt(6.0 / inputSize);
                yield RANDOM.nextDouble() * 2 * limit - limit;
            }
            case ZERO -> 0;
        };
    }

    /**
     * Initialize bias.
     *
     * @param strategy initialization strategy
     *
     * @return initialization data
     */
    public static double initializeBias(WeightInitStrategy strategy) {
        // Bias inicializálás stratégia alapján
        return switch (strategy) {
            case RANDOM -> RANDOM.nextGaussian() * 0.01; // Kis véletlenszerű érték
            case XAVIER -> RANDOM.nextGaussian() * Math.sqrt(1.0 / (1 + 1)); // XAVIER esetén kis véletlenszerű érték
            case HE -> RANDOM.nextGaussian() * Math.sqrt(2.0 / (1 + 1)); // HE kis véletlenszerű szám
            case UNIFORM -> {
                double limit = Math.sqrt(6.0 / 1); // Könnyíthetjük is 6-ra
                yield RANDOM.nextDouble() * 2 * limit - limit;
            }
            case ZERO -> 0;
        };
    }
}
