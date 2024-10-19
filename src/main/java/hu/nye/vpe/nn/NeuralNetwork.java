package hu.nye.vpe.nn;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
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

    private final List<Layer> layers;
    private double learningRate;
    private double discountFactor;
    private double epsilon;
    private int episodeCount;
    private double bestScore;
    private final Random random;
    private final GradientClipper gradientClipper;
    private double maxQValue;
    private double lastReward;
    private double[][] lastActivations;
    private final List<Double> recentRewards;
    private double movingAverage;
    private final ExperienceReplay experienceReplay;
    private List<double[]> inputBatch;
    private List<double[]> targetBatch;

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
        this.bestScore = 0.0;
        this.recentRewards = new ArrayList<>();
        this.movingAverage = 0.0;
        this.random = new Random();
        this.experienceReplay = new ExperienceReplay(EXPERIENCE_REPLAY_CAPACITY);
        this.inputBatch = new ArrayList<>();
        this.targetBatch = new ArrayList<>();
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
        double max = qvalues[0];
        for (int i = 1; i < qvalues.length; i++) {
            if (qvalues[i] > max) {
                max = qvalues[i];
            }
        }
        this.maxQValue = max;
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
    public int selectAction(double[] state) {
        if (random.nextDouble() < epsilon) {
            return random.nextInt(layers.get(layers.size() - 1).getSize());
        } else {
            double[] qvalues = forward(state);
            return argmax(qvalues);
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
    public void learn(double[] state, int action, double reward, double[] nextState, boolean gameOver) {
        if (USE_EXPERIENCE) {
            Experience experience = new Experience(state, action, reward, nextState, gameOver);
            experienceReplay.add(experience);

            if (experienceReplay.size() >= EXPERIENCE_BATCH_SIZE) {
                List<Experience> batch = experienceReplay.sample(EXPERIENCE_BATCH_SIZE);
                processBatchWithExperience(batch);
            }
        } else {
            double[] currentQValues = forward(state);
            double[] nextQValues = forward(nextState);
            double maxNextQ = max(nextQValues);

            double normalizedReward = reward / Math.sqrt(reward * reward + 1);
            double target = normalizedReward + (gameOver ? 0 : discountFactor * maxNextQ);
            target = Math.max(MIN_Q, Math.min(MAX_Q, target));

            double[] targetQValues = currentQValues.clone();
            targetQValues[action] = target;

            inputBatch.add(state);
            targetBatch.add(targetQValues);

            if (inputBatch.size() == EXPERIENCE_BATCH_SIZE) {
                processBatchWithoutExperience();
            }
        }

        lastReward = reward;

        if (gameOver) {
            if (reward > this.bestScore) {
                this.bestScore = reward;
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
            double[] nextQValues = forward(exp.nextState);
            double maxNextQ = max(nextQValues);

            double normalizedReward = exp.reward / Math.sqrt(exp.reward * exp.reward + 1);
            double target = normalizedReward + (exp.done ? 0 : discountFactor * maxNextQ);
            target = Math.max(MIN_Q, Math.min(MAX_Q, target));

            double[] targetQValues = currentQValues.clone();
            targetQValues[exp.action] = target;

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

    private int argmax(double[] array) {
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

    private double max(double[] array) {
        double maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
            }
        }
        return maxValue;
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

    public double getBestScore() {
        return bestScore;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public double getMaxQValue() {
        return maxQValue;
    }

    public double getLastReward() {
        return lastReward;
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
     * Activate method for SoftMax (vector input).
     *
     * @param x vector of input values
     *
     * @return activated values
     */
    public static double[] activateSoftMax(double[] x) {
        double[] output = new double[x.length];
        double sum = 0.0;
        double max = x[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] > max) {
                max = x[i];
            }
        }
        for (int i = 0; i < x.length; i++) {
            output[i] = Math.exp(x[i] - max);
            sum += output[i];
        }
        for (int i = 0; i < x.length; i++) {
            output[i] /= sum;
        }
        return output;
    }

    /**
     * Derivative method for SoftMax (vector input and output).
     *
     * @param output the output of the SoftMax function
     * @param index the index of the output for which we're computing the derivative
     * @return the partial derivatives
     */
    public static double[] derivativeSoftMax(double[] output, int index) {
        double[] derivatives = new double[output.length];
        for (int i = 0; i < output.length; i++) {
            if (i == index) {
                derivatives[i] = output[i] * (1 - output[i]);
            } else {
                derivatives[i] = -output[index] * output[i];
            }
        }
        return derivatives;
    }

}