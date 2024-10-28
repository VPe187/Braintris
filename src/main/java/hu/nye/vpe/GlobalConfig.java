package hu.nye.vpe;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.nye.vpe.nn.Activation;
import hu.nye.vpe.nn.BatchNormParameters;
import hu.nye.vpe.nn.WeightInitStrategy;

/**
 * Global config class.
 */
public class GlobalConfig {
    private static final String CONFIG_FILE = "config.properties";
    private static GlobalConfig instance;
    private Properties properties;

    private GlobalConfig() {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     *  Global config get instance.
     *
     * @return instance
     */
    public static GlobalConfig getInstance() {
        if (instance == null) {
            instance = new GlobalConfig();
        }
        return instance;
    }

    private String resolveValue(String value) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = properties.getProperty(propertyName);
            if (propertyValue == null) {
                throw new RuntimeException("Property not found: " + propertyName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(propertyValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    private int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            System.err.println("Warning: Property " + key + " not found. Using default value: " + defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(resolveValue(value));
        } catch (NumberFormatException e) {
            System.err.println("Error parsing int value for key " + key + ": " + value);
            System.err.println("Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Tetris osztály konstansai
    public double getRewardFullRow() {
        return getDouble("REWARD_FULLROW", 100);
    }

    public double getRewardNearlyFullRow() {
        return getDouble("REWARD_NEARLY_FULLROW", 40);
    }

    public double getRewardPlaceWithoutHole() {
        return getDouble("REWARD_PLACE_WITHOUT_HOLE", 60);
    }

    public double getRewardDropLower() {
        return getDouble("REWARD_DROP_LOWER", 20);
    }

    public double getRewardDropHigher() {
        return getDouble("REWARD_DROP_HIGHER", 1);
    }

    public double getRewardDropedElements() {
        return getDouble("REWARD_DROPPED_ELEMENTS", 0.01);
    }

    public double getRewardAvgDensity() {
        return getDouble("REWARD_AVG_DENSITY", 100);
    }

    public double getRewardNumberOfHoles() {
        return getDouble("REWARD_NUMBER_OF_HOLES", 0.1);
    }

    public double getRewardSurroundedHoles() {
        return getDouble("REWARD_SURROUNDED_HOLES", 0.2);
    }

    public double getRewardBlockedRow() {
        return getDouble("REWARD_BLOCKED_ROW", 1.2);
    }

    public double getRewardBumpiness() {
        return getDouble("REWARD_BUMPINESS", 0.3);
    }

    public double getRewardAvgColumnHeight() {
        return getDouble("REWARD_AVG_COLUMN_HEIGHT", 0.5);
    }


    public double getRewardMaximumHeight() {
        return getDouble("REWARD_MAXIMUM_HEIGHT", 0.9);
    }


    // NeuralNetwork osztály konstansai
    public String getBrainFilename() {
        return getString("FILE_NAME", "brain.dat");
    }

    public int getFeedDataSize() {
        return getInt("FEED_DATA_SIZE", 30);
    }

    public Boolean getNormalizeFeedData() {
        return getBoolean("NORMALIZE_FEED_DATA", true);
    }

    /**
     * Get minimum batch size for batch normalization.
     * This ensures we have enough samples for statistical calculations.
     *
     * @return minimum batch size, defaults to 2
     */
    public int getMinimumBatchSize() {
        return getInt("MINIMUM_BATCH_SIZE", 2);
    }

    /**
     * Get actual batch size for batch normalization.
     * Returns the maximum of the configured batch size and minimum batch size.
     *
     * @return actual batch size to use
     */
    public int getActualBatchSize() {
        if (getUseExperience()) {
            return getExperienceBatchSize();
        } else {
            // Experience replay nélkül használjuk a minimum batch size-t
            return getMinimumBatchSize();
        }
    }

    /**
     * Get layer names.
     *
     * @return layer names
     */
    public String[] getLayerNames() {
        String namesString = properties.getProperty("NAMES", "INP,H1,H2,H3,H4,OUT");
        return namesString.split(",");
    }

    /**
     * Get layer sizes.
     *
     * @return layer sizes.
     */
    public int[] getLayerSizes() {
        String sizeString = properties.getProperty("LAYER_SIZES", "${FEED_DATA_SIZE},64,32,16,32,${OUTPUT_NODES}");
        sizeString = resolveValue(sizeString);
        return Arrays.stream(sizeString.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    /**
     * Get layer activations.
     *
     * @return layer activations.
     */
    public Activation[] getLayerActivations() {
        String activationsString = properties.getProperty("LAYER_ACTIVATIONS", "LEAKY_RELU,LEAKY_RELU,LEAKY_RELU,LEAKY_RELU,LINEAR");
        Activation[] activations = Arrays.stream(activationsString.split(","))
                .map(Activation::valueOf)
                .toArray(Activation[]::new);

        int[] layerSizes = getLayerSizes();
        if (activations.length != layerSizes.length - 1) {
            System.err.println("Warning: LAYER_ACTIVATIONS length does not match LAYER_SIZES. Adjusting...");
            Activation[] adjustedActivations = new Activation[layerSizes.length - 1];
            System.arraycopy(activations, 0, adjustedActivations, 0, Math.min(activations.length, adjustedActivations.length));
            for (int i = activations.length; i < adjustedActivations.length; i++) {
                adjustedActivations[i] = Activation.LEAKY_RELU;
            }
            return adjustedActivations;
        }
        return activations;
    }

    /**
     * Get weight init strategies.
     *
     * @return weight init strategies
     */
    public WeightInitStrategy[] getWeightInitStrategies() {
        String strategiesString = properties.getProperty("WEIGHT_INIT_STRATEGIES", "HE,HE,HE,HE,XAVIER");
        WeightInitStrategy[] strategies = Arrays.stream(strategiesString.split(","))
                .map(WeightInitStrategy::valueOf)
                .toArray(WeightInitStrategy[]::new);

        int[] layerSizes = getLayerSizes();
        if (strategies.length != layerSizes.length - 1) {
            System.err.println("Warning: WEIGHT_INIT_STRATEGIES length does not match LAYER_SIZES. Adjusting...");
            WeightInitStrategy[] adjustedStrategies = new WeightInitStrategy[layerSizes.length - 1];
            System.arraycopy(strategies, 0, adjustedStrategies, 0, Math.min(strategies.length, adjustedStrategies.length));
            for (int i = strategies.length; i < adjustedStrategies.length; i++) {
                adjustedStrategies[i] = WeightInitStrategy.HE;
            }
            return adjustedStrategies;
        }
        return strategies;
    }

    /**
     * Get batch normalizers parameters.
     *
     * @return batch normalizers parameters.
     */
    public BatchNormParameters[] getBatchNorms() {
        String batchNormString = properties.getProperty("BATCH_NORMS", "");
        BatchNormParameters[] batchNorms;
        if (batchNormString.isEmpty()) {
            System.err.println("Warning: BATCH_NORMS property is empty. Using default values.");
            batchNorms = new BatchNormParameters[0];
        } else {
            batchNorms = Arrays.stream(batchNormString.split(","))
                    .map(s -> {
                        String[] parts = s.split(":");
                        if (parts.length != 3) {
                            System.err.println("Warning: Invalid batch norm parameter format: " + s + ". Using default values.");
                            return new BatchNormParameters(false, 1.0, 0.0);
                        }
                        try {
                            return new BatchNormParameters(
                                    Boolean.parseBoolean(parts[0]),
                                    Double.parseDouble(parts[1]),
                                    Double.parseDouble(parts[2])
                            );
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Error parsing batch norm parameters: " + s + ". Using default values.");
                            return new BatchNormParameters(false, 1.0, 0.0);
                        }
                    })
                    .toArray(BatchNormParameters[]::new);
        }

        int[] layerSizes = getLayerSizes();
        if (batchNorms.length != layerSizes.length - 1) {
            System.err.println("Warning: USE_BATCH_NORM length does not match LAYER_SIZES. Adjusting...");
            BatchNormParameters[] adjustedBatchNorms = new BatchNormParameters[layerSizes.length - 1];
            System.arraycopy(batchNorms, 0, adjustedBatchNorms, 0, Math.min(batchNorms.length, adjustedBatchNorms.length));
            for (int i = batchNorms.length; i < adjustedBatchNorms.length; i++) {
                adjustedBatchNorms[i] = new BatchNormParameters(false, 1.0, 0.0);
            }
            return adjustedBatchNorms;
        }
        return batchNorms;
    }

    /**
     * Get lambda 2 regularizations.
     *
     * @return regularizations
     */
    public double[] getL2Regularization() {
        return Arrays.stream(properties.getProperty("L2_REGULARIZATION", "").split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    /**
     * Get clipping min.
     *
     * @return clipping min.
     */
    public double getClipMin() {
        return getDouble("CLIP_MIN", -1.0);
    }

    /**
     * Get clipping max.
     *
     * @return clipping max.
     */
    public double getClipMax() {
        return getDouble("CLIP_MAX", 1.0);
    }

    public double getClipNorm() {
        return getDouble("CLIP_NORM", 1.0);
    }

    public double getGradientScale() {
        return getDouble("GRADIENT_SCALE", 1.0);
    }

    public double getInitialLearningRate() {
        return getDouble("INITIAL_LEARNING_RATE", 0.01);
    }

    public double getLearningRateDecay() {
        return getDouble("LEARNING_RATE_DECAY", 0.999);
    }

    public double getMinLearningRate() {
        return getDouble("MIN_LEARNING_RATE", 0.0001);
    }

    public double getInitialDiscountFactor() {
        return getDouble("INITIAL_DISCOUNT_FACTOR", 0.5);
    }

    public double getMaxDiscountFactor() {
        return getDouble("MAX_DISCOUNT_FACTOR", 0.99);
    }

    public double getDiscountFactorIncrement() {
        return getDouble("DISCOUNT_FACTOR_INCREMENT", 0.0001);
    }

    public double getInitialEpsilon() {
        return getDouble("INITIAL_EPSILON", 0.06);
    }

    public double getEpsilonDecay() {
        return getDouble("EPSILON_DECAY", 0.99);
    }

    public double getMinEpsilon() {
        return getDouble("MIN_EPSILON", -500.0);
    }

    public double getMinQ() {
        return getDouble("MIN_Q", -500.0);
    }

    public double getMaxQ() {
        return getDouble("MAX_Q", 500.0);
    }

    public int getMovingAverageWindow() {
        return getInt("MOVING_AVERAGE_WINDOW", 1000);
    }


    public boolean getUseExperience() {
        return getBoolean("USE_EXPERIENCE", false);
    }


    public int getExperiebceReplayCapacity() {
        return getInt("EXPERIENCE_REPLAY_CAPACITY", 1000);
    }

    public int getExperienceBatchSize() {
        return getInt("EXPERIENCE_BATCH_SIZE", 1000);
    }

    public double getBatchEpsilon() {
        return getDouble("BATCH_DEFAULT_EPSILON", 1e-5);
    }

    public double getBatchMomentum() {
        return getDouble("BATCH_DEFAULT_MOMENTUM", 0.99);
    }

    public String getFeedDataNormalizer() {
        return getString("FEED_DATA_NORMALIZER", "MINMAX");
    }

    public double getBeta1Momentum() {
        return getDouble("BETA1_MOMENTUM", 0.99);
    }

    public double getBeta2RmsProp() {
        return getDouble("BETA2_RMSPROP", 0.999);
    }

    public double getAdamMomentum() {
        return getDouble("ADAM_MOMENTUM", 1e-6);
    }
}
