package hu.nye.vpe.nn;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import hu.nye.vpe.GlobalConfig;

/**
 * Neural network class.
 */
public class NeuralNetwork implements Serializable {
    private static final String FILENAME = GlobalConfig.getInstance().getBrainFilename();
    private static final double CLIP_MIN = GlobalConfig.getInstance().getClipMin();
    private static final double CLIP_MAX = GlobalConfig.getInstance().getClipMax();
    private static final double CLIP_NORM = GlobalConfig.getInstance().getClipNorm();
    private static final double GRADIENT_SCALE = GlobalConfig.getInstance().getGradientScale();
    private static final double INITIAL_LEARNING_RATE = GlobalConfig.getInstance().getInitialLearningRate();
    private static final double LEARNING_RATE_DECAY = GlobalConfig.getInstance().getLearningRateDecay();
    private static final double MIN_LEARNING_RATE = GlobalConfig.getInstance().getMinLearningRate();
    private static final double INITIAL_DISCOUNT_FACTOR = GlobalConfig.getInstance().getInitialDiscountFactor();
    private static final double MAX_DISCOUNT_FACTOR = GlobalConfig.getInstance().getMaxDiscountFactor();
    private static final double DISCOUNT_FACTOR_INCREMENT = GlobalConfig.getInstance().getDiscountFactorIncrement();
    private static final double INITIAL_EPSILON = GlobalConfig.getInstance().getInitialEpsilon();
    private static final double EPSILON_DECAY = GlobalConfig.getInstance().getEpsilonDecay();
    private static final double MIN_EPSILON = GlobalConfig.getInstance().getMinEpsilon();
    private static final double MIN_Q = GlobalConfig.getInstance().getMinQ();
    private static final double MAX_Q = GlobalConfig.getInstance().getMaxQ();
    private static final int MOVING_AVERAGE_WINDOW = GlobalConfig.getInstance().getMovingAverageWindow();
    private static final Boolean USE_EXPERIENCE = GlobalConfig.getInstance().getUseExperience();
    private static final int EXPERIENCE_REPLAY_CAPACITY = GlobalConfig.getInstance().getExperiebceReplayCapacity();
    private static final int EXPERIENCE_BATCH_SIZE = GlobalConfig.getInstance().getExperienceBatchSize();
    private static final int X_COORD_OUTPUTS = 12;
    private static final int ROTATION_OUTPUTS = 3;
    private static final int TOTAL_OUTPUTS = X_COORD_OUTPUTS + ROTATION_OUTPUTS;


    private final List<Layer> layers;
    private double learningRate;
    private double discountFactor;
    private double epsilon;
    private int episodeCount;
    private final Random random;
    private final GradientClipper gradientClipper;
    private double maxQValueX;
    private double maxQValueRotation;
    private double lastReward;
    private double bestReward;
    private double[][] lastActivations;
    private final List<Double> recentRewards;
    private double movingAverage;
    private final ExperienceReplay experienceReplay;
    private List<double[]> inputBatch;
    private List<double[]> targetBatch;
    private double rms;
    private double[] layerMins;
    private double[] layerMaxs;
    private double[] layerMeans;
    private double averageWeightChange;
    private double[][][] weightChanges;
    private double[][][] previousWeights;


    public NeuralNetwork(String[] names, int[] layerSizes, Activation[] activations, WeightInitStrategy[] initStrategies,
                         BatchNormParameters[] batchNormParameters, double[] l2) {
        if (layerSizes.length != activations.length + 1 ||
                layerSizes.length != initStrategies.length + 1 ||
                layerSizes.length != batchNormParameters.length + 1 ||
                layerSizes.length != l2.length) {
            throw new IllegalArgumentException("Invalid configuration: layerSizes length should be one more than " +
                    "the length of activations, initStrategies, and useBatchNorm arrays");
        }

        this.gradientClipper = new GradientClipper(CLIP_MIN, CLIP_MAX, CLIP_NORM, GRADIENT_SCALE);
        this.layers = new ArrayList<>();
        this.learningRate = INITIAL_LEARNING_RATE;

        for (int i = 0; i < layerSizes.length - 1; i++) {
            int inputSize = layerSizes[i];
            int outputSize = layerSizes[i + 1];

            layers.add(new Layer(names[i], inputSize, outputSize, activations[i], initStrategies[i],
                    gradientClipper, l2[i], batchNormParameters[i], learningRate));
        }

        this.discountFactor = INITIAL_DISCOUNT_FACTOR;
        this.epsilon = INITIAL_EPSILON;
        this.episodeCount = 0;
        this.bestReward = Double.NEGATIVE_INFINITY;
        this.recentRewards = new ArrayList<>();
        this.movingAverage = Double.NEGATIVE_INFINITY;
        this.random = new Random();
        this.experienceReplay = new ExperienceReplay(EXPERIENCE_REPLAY_CAPACITY);
        this.inputBatch = new ArrayList<>();
        this.targetBatch = new ArrayList<>();
        this.layerMins = new double[layerSizes.length];
        this.layerMaxs = new double[layerSizes.length];
        this.layerMeans = new double[layerSizes.length];
        this.previousWeights = null;
        this.averageWeightChange = 0.0;
        this.weightChanges = null;
    }

    /**
     * Forward pass.
     *
     * @param inputs input values
     *
     * @return forwarded input values
     */

    public double[] forward(double[] inputs) {
        double[] currentInput = inputs;
        this.lastActivations = new double[layers.size() + 1][];
        this.lastActivations[0] = inputs;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            currentInput = layer.forward(currentInput, true);
            this.lastActivations[i + 1] = currentInput;
        }

        for (int i = 0; i < currentInput.length; i++) {
            currentInput[i] = Math.max(MIN_Q, Math.min(MAX_Q, currentInput[i]));
        }

        updateMaxQValue(currentInput);
        return currentInput;
    }

    private void updateMaxQValue(double[] qvalues) {
        maxQValueX = qvalues[0];
        for (int i = 1; i < X_COORD_OUTPUTS; i++) {
            if (qvalues[i] > maxQValueX) {
                maxQValueX = qvalues[i];
            }
        }

        maxQValueRotation = qvalues[X_COORD_OUTPUTS];
        for (int i = X_COORD_OUTPUTS + 1; i < TOTAL_OUTPUTS; i++) {
            if (qvalues[i] > maxQValueRotation) {
                maxQValueRotation = qvalues[i];
            }
        }
    }

    private void backwardPass(double[][] batchInputs, double[][] batchTargets) {
        int batchSize = batchInputs.length;
        List<double[][]> allLayerOutputs = new ArrayList<>();

        double[][] currentInputs = batchInputs;
        allLayerOutputs.add(currentInputs);

        for (Layer layer : layers) {
            currentInputs = layer.forwardBatch(currentInputs, true);
            allLayerOutputs.add(currentInputs);
        }

        double[][] deltas = new double[batchSize][layers.get(layers.size() - 1).getSize()];
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < deltas[i].length; j++) {
                deltas[i][j] = batchTargets[i][j] - currentInputs[i][j];
            }
        }

        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer currentLayer = layers.get(i);
            LayerGradients gradients = currentLayer.backwardBatch(deltas, allLayerOutputs.get(i));
            deltas = gradients.inputGradients;
        }
    }

    /**
     * Select action from network.
     *
     * @param state metric datas
     *
     * @return action
     */
    public int[] selectAction(double[] state) {
        if (random.nextDouble() < epsilon) {
            return new int[]{
                    random.nextInt(X_COORD_OUTPUTS),
                    random.nextInt(ROTATION_OUTPUTS)
            };
        } else {
            double[] qvalues = forward(state);
            int xAction = argmax(qvalues, 0, X_COORD_OUTPUTS);
            int rotationAction = argmax(qvalues, X_COORD_OUTPUTS, TOTAL_OUTPUTS) - X_COORD_OUTPUTS;
            return new int[]{xAction, rotationAction};
        }
    }

    /**
     * Learn method.
     *
     * @param state current metric data
     *
     * @param action last action
     *
     * @param reward reward
     *
     * @param nextState next state datas
     *
     * @param gameOver game is over?
     */
    public void learn(double[] state, int[] action, double reward, double[] nextState, boolean gameOver) {
        if (USE_EXPERIENCE) {
            Experience experience = new Experience(state, action, reward, nextState, gameOver);
            experienceReplay.add(experience);

            if (experienceReplay.size() >= EXPERIENCE_BATCH_SIZE) {
                List<Experience> batch = experienceReplay.sample(EXPERIENCE_BATCH_SIZE);
                processBatchWithExperience(batch);
            }
        } else {
            double[] currentQValues = forward(state);
            forward(nextState);

            double normalizedReward = reward / Math.sqrt(reward * reward + 1);

            double targetX = normalizedReward + (gameOver ? 0 : discountFactor * maxQValueX);
            double targetRotation = normalizedReward + (gameOver ? 0 : discountFactor * maxQValueRotation);

            targetX = Math.max(MIN_Q, Math.min(MAX_Q, targetX));
            targetRotation = Math.max(MIN_Q, Math.min(MAX_Q, targetRotation));

            double[] targetQValues = currentQValues.clone();

            if (action[0] >= 0 && action[0] < X_COORD_OUTPUTS &&
                    action[1] >= 0 && action[1] < ROTATION_OUTPUTS) {
                targetQValues[action[0]] = targetX;
                targetQValues[X_COORD_OUTPUTS + action[1]] = targetRotation;
            } else {
                System.out.println("Érvénytelen akció: " + Arrays.toString(action));
                return; // Kilépünk a metódusból, ha érvénytelen az akció
            }

            inputBatch.add(state);
            targetBatch.add(targetQValues);

            if (inputBatch.size() == EXPERIENCE_BATCH_SIZE) {
                processBatchWithoutExperience();
            }
        }

        lastReward = reward;

        if (gameOver) {
            if (reward > this.bestReward) {
                this.bestReward = reward;
            }
            updateEpsilon();
            updateDiscountFactor();
            updateLearningRate();
            this.episodeCount++;
            updateMovingAverage(reward);
        }
    }

    private void processBatchWithExperience(List<Experience> batch) {
        double[][] inputs = new double[EXPERIENCE_BATCH_SIZE][];
        double[][] targets = new double[EXPERIENCE_BATCH_SIZE][];

        for (int i = 0; i < EXPERIENCE_BATCH_SIZE; i++) {
            Experience exp = batch.get(i);
            double[] currentQValues = forward(exp.state);
            forward(exp.nextState);

            double normalizedReward = exp.reward / Math.sqrt(exp.reward * exp.reward + 1);

            double targetX = normalizedReward + (exp.done ? 0 : discountFactor * maxQValueX);
            double targetRotation = normalizedReward + (exp.done ? 0 : discountFactor * maxQValueRotation);

            targetX = Math.max(MIN_Q, Math.min(MAX_Q, targetX));
            targetRotation = Math.max(MIN_Q, Math.min(MAX_Q, targetRotation));

            double[] targetQValues = currentQValues.clone();

            targetQValues[exp.action[0]] = targetX;
            targetQValues[X_COORD_OUTPUTS + exp.action[1]] = targetRotation;

            inputs[i] = exp.state;
            targets[i] = targetQValues;
        }

        backwardPass(inputs, targets);
    }

    private void processBatchWithoutExperience() {
        double[][] inputs = inputBatch.toArray(new double[0][]);
        double[][] targets = targetBatch.toArray(new double[0][]);

        backwardPass(inputs, targets);

        inputBatch.clear();
        targetBatch.clear();
    }

    private void updateMovingAverage(double reward) {
        recentRewards.add(reward);
        if (recentRewards.size() > MOVING_AVERAGE_WINDOW) {
            recentRewards.remove(0);
        }

        double sum = 0;
        for (Double r : recentRewards) {
            sum += r;
        }
        movingAverage = sum / recentRewards.size();
    }

    private void updateEpsilon() {
        epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
    }

    private void updateLearningRate() {
        learningRate = Math.max(MIN_LEARNING_RATE, learningRate * LEARNING_RATE_DECAY);
        for (Layer layer : layers) {
            layer.setLearningRate(learningRate);
        }
    }

    private void updateDiscountFactor() {
        discountFactor = Math.min(MAX_DISCOUNT_FACTOR, discountFactor + DISCOUNT_FACTOR_INCREMENT);
    }

    private int argmaxOLD(double[] array) {
        int bestIndex = 0;
        double maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int argmax(double[] array, int start, int end) {
        int bestIndex = start;
        double maxValue = array[start];
        for (int i = start + 1; i < end; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private double max(double[] array) {
        double maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
            }
        }
        return maxValue;
    }

    public void updateStatistics() {
        double[][][] currentWeights = getAllWeights();

        if (this.previousWeights != null) {
            this.averageWeightChange = calculateAverageWeightChange(this.previousWeights, currentWeights);
            this.weightChanges = calculateWeightChanges(this.previousWeights, currentWeights);
        }
        this.previousWeights = deepCopy(currentWeights);

        calculateLayerStatistics();
        this.rms = calculateRMS(lastActivations[lastActivations.length - 1]);
    }

    private void calculateLayerStatistics() {
        for (int i = 0; i < lastActivations.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0;

            for (double activation : lastActivations[i]) {
                min = Math.min(min, activation);
                max = Math.max(max, activation);
                sum += activation;
            }

            layerMins[i] = min;
            layerMaxs[i] = max;
            layerMeans[i] = sum / lastActivations[i].length;
        }
    }

    private double calculateRMS(double[] outputs) {
        double sum = 0;
        for (double output : outputs) {
            sum += output * output;
        }
        return Math.sqrt(sum / outputs.length);
    }

    private double[][][] calculateWeightChanges(double[][][] oldWeights, double[][][] newWeights) {
        double[][][] changes = new double[oldWeights.length][][];
        for (int i = 0; i < oldWeights.length; i++) {
            changes[i] = new double[oldWeights[i].length][];
            for (int j = 0; j < oldWeights[i].length; j++) {
                changes[i][j] = new double[oldWeights[i][j].length];
                for (int k = 0; k < oldWeights[i][j].length; k++) {
                    changes[i][j][k] = newWeights[i][j][k] - oldWeights[i][j][k];
                }
            }
        }
        return changes;
    }

    private double[][][] deepCopy(double[][][] original) {
        double[][][] copy = new double[original.length][][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = new double[original[i].length][];
            for (int j = 0; j < original[i].length; j++) {
                copy[i][j] = original[i][j].clone();
            }
        }
        return copy;
    }

    private double calculateAverageWeightChange(double[][][] oldWeights, double[][][] newWeights) {
        double totalChange = 0.0;
        long weightCount = 0;

        for (int i = 0; i < oldWeights.length; i++) {
            for (int j = 0; j < oldWeights[i].length; j++) {
                for (int k = 0; k < oldWeights[i][j].length; k++) {
                    double change = Math.abs(newWeights[i][j][k] - oldWeights[i][j][k]);
                    totalChange += change;
                    weightCount++;
                }
            }
        }

        return weightCount > 0 ? totalChange / weightCount : 0.0;
    }

    /**
     * Save class or brain data to file.
     *
     * @throws IOException file error
     */
    public void saveToFile() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            oos.writeObject(this);
        }
    }

    /**
     * Load class or brain from file.
     *
     * @return class or brain
     *
     * @throws IOException file error
     *
     * @throws ClassNotFoundException class compatibility error
     */
    public static NeuralNetwork loadFromFile() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILENAME))) {
            return (NeuralNetwork) ois.readObject();
        }
    }

    /**
     * Get all weights for visualization.
     *
     * @return weights
     */
    public double[][][] getAllWeights() {
        double[][][] allWeights = new double[layers.size()][][];
        for (int i = 0; i < layers.size(); i++) {
            Layer currentLayer = layers.get(i);
            List<Neuron> neurons = currentLayer.getNeurons();
            allWeights[i] = new double[neurons.size()][];
            for (int j = 0; j < neurons.size(); j++) {
                allWeights[i][j] = neurons.get(j).getWeights().clone();
            }
        }
        return allWeights;
    }

    /**
     * Get layer sizes.
     *
     * @return int array of layer sizes for visualization
     */
    public int[] getLayerSizes() {
        int[] sizes = new int[layers.size() + 1];
        sizes[0] = layers.get(0).getNeurons().get(0).getWeights().length;
        for (int i = 0; i < layers.size(); i++) {
            sizes[i + 1] = layers.get(i).getNeurons().size();
        }
        return sizes;
    }

    /**
     * Get names of layers.
     *
     * @return String array of names
     */
    public String[] getLayerNames() {
        String[] names = new String[layers.size() + 1];
        for (int i = 0; i < layers.size(); i++) {
            names[i] = layers.get(i).getName();
        }
        names[layers.size()] = "OUT";
        return names;
    }

    /**
     * Get last activations data for visualization.
     *
     * @return fouble matrices of activation data
     */
    public double[][] getLastActivations() {
        if (this.lastActivations == null) {
            this.lastActivations = new double[layers.size() + 1][];
            for (int i = 0; i < layers.size() + 1; i++) {
                this.lastActivations[i] = new double[0];
            }
        }
        return this.lastActivations;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getDiscountFactor() {
        return discountFactor;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public double getBestReward() {
        return bestReward;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public double getMaxQValueX() {
        return maxQValueX;
    }

    public double getMaxQValueRotation() {
        return maxQValueRotation;
    }

    public double getLastReward() {
        return lastReward;
    }

    public double getRMS() {
        return rms;
    }

    public double[] getLayerMins() {
        return layerMins;
    }

    public double[] getLayerMaxs() {
        return layerMaxs;
    }

    public double[] getLayerMeans() {
        return layerMeans;
    }

    public double getAverageWeightChange() {
        return averageWeightChange;
    }

    public double[][][] getWeightChanges() {
        return weightChanges;
    }

    /**
     * Visszaadja a jutalmak mozgóátlagát.
     *
     * @return A jutalmak mozgóátlaga
     */
    public double getMovingAverage() {
        return movingAverage;
    }

    /**
     * Visszaadja a legnagyobb átlagos rewardot a recentRewards listából.
     *
     * @return A legnagyobb átlagos reward
     */
    public double getMaxAverageReward() {
        if (recentRewards.isEmpty()) {
            return Double.NEGATIVE_INFINITY;  // Ha a listában nincsenek elemek, 0-t ad vissza.
        }
        double maxReward = Double.NEGATIVE_INFINITY;
        for (double reward : recentRewards) {
            if (reward > maxReward) {
                maxReward = reward;
            }
        }
        return maxReward;
    }

    /**
     * Visszaadja az utolsó mozgóátlagot a recentRewards listából.
     *
     * @return Az utolsó mozgóátlag
     */
    public double getLastMovingAverage() {
        if (recentRewards.isEmpty()) {
            return Double.NEGATIVE_INFINITY;  // Ha a listában nincsenek elemek, 0-t ad vissza.
        }
        return recentRewards.get(recentRewards.size() - 1);
    }
}