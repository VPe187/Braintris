package hu.nye.vpe.nn;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * Neural network for Q-Learning.
 */
public class NeuralNetworkQLearning implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int HIDDEN_NODES1 = 16;
    private static final int HIDDEN_NODES2 = 12;
    private static final int HIDDEN_NODES3 = 8;
    private static final double INITIAL_LEARNING_RATE = 0.01;
    private static final double LEARNING_RATE_DECAY = 0.9999;
    private static final double MIN_LEARNING_RATE = 0.0001;
    private static final double INITIAL_DISCOUNT_FACTOR = 0.6;
    private static final double MAX_DISCOUNT_FACTOR = 0.99;
    private static final double DISCOUNT_FACTOR_INCREMENT = 0.0001;
    private static final double INITIAL_EPSILON = 0.6;
    private static final double EPSILON_DECAY = 0.999;
    private static final double MIN_EPSILON = 0.01;
    private static final double MIN_Q_VALUE = -50;
    private static final double MAX_Q_VALUE = 50;
    private static final double L2_LAMBDA = 0.1;
    private static final double GRADIENT_CLIP_MIN = -1;
    private static final double GRADIENT_CLIP_MAX = 1;
    private static final double GRADIENT_SCALE = 1.1;
    private static final String FILENAME = "brain.dat";
    private final int inputNodes;
    private final int hiddenNodes1;
    private final int hiddenNodes2;
    private final int hiddenNodes3;
    private final int outputNodes;
    private double[][] weightsInputHidden1;
    private double[][] weightsHidden1Hidden2;
    private double[][] weightsHidden2Hidden3;
    private double[][] weightsHidden3Output;
    private double[] biasHidden1;
    private double[] biasHidden2;
    private double[] biasHidden3;
    private double[] biasOutput;
    private final Random random;
    private double learningRate;
    private double discountFactor;
    private double epsilon;
    private int episodeCount;
    private double bestScore = Double.NEGATIVE_INFINITY;
    private double rewardValue;
    private double maxNextQValue;
    private double[][] lastActivations;

    /**
     * QLearning neural network.
     *
     * @param inputNodes Number of input nodes
     * @param outputNodes Number of output nodes
     */
    public NeuralNetworkQLearning(int inputNodes, int outputNodes) {
        this.inputNodes = inputNodes;
        this.hiddenNodes1 = HIDDEN_NODES1;
        this.hiddenNodes2 = HIDDEN_NODES2;
        this.hiddenNodes3 = HIDDEN_NODES3;
        this.outputNodes = outputNodes;
        this.random = new Random();
        this.learningRate = INITIAL_LEARNING_RATE;
        this.discountFactor = INITIAL_DISCOUNT_FACTOR;
        this.epsilon = INITIAL_EPSILON;
        initializeLastActivations();
        loadOrInitialize();
    }

    private void initializeLastActivations() {
        this.lastActivations = new double[5][];  // 5 layers: input, hidden1, hidden2, hidden3, output
        this.lastActivations[0] = new double[inputNodes];
        this.lastActivations[1] = new double[HIDDEN_NODES1];
        this.lastActivations[2] = new double[HIDDEN_NODES2];
        this.lastActivations[3] = new double[HIDDEN_NODES3];
        this.lastActivations[4] = new double[outputNodes];
    }

    private void loadOrInitialize() {
        try {
            NeuralNetworkQLearning loadedNetwork = loadFromFile();
            if (loadedNetwork != null &&
                    loadedNetwork.inputNodes == this.inputNodes &&
                    loadedNetwork.hiddenNodes1 == this.hiddenNodes1 &&
                    loadedNetwork.hiddenNodes2 == this.hiddenNodes2 &&
                    loadedNetwork.outputNodes == this.outputNodes) {
                System.out.println("Saved network loaded successfully");
            } else {
                initializeNetwork();
            }
        } catch (IOException | ClassNotFoundException e) {
            initializeNetwork();
            System.out.println(e.getMessage());
        }
    }

    private void initializeNetwork() {
        weightsInputHidden1 = initializeWeightsHE(inputNodes, hiddenNodes1);
        weightsHidden1Hidden2 = initializeWeightsHE(hiddenNodes1, hiddenNodes2);
        weightsHidden2Hidden3 = initializeWeightsHE(hiddenNodes2, hiddenNodes3);
        weightsHidden3Output = initializeWeightsHE(hiddenNodes3, outputNodes);
        biasHidden1 = initializeBiasesReLU(hiddenNodes1);
        biasHidden2 = initializeBiasesReLU(hiddenNodes2);
        biasHidden3 = initializeBiasesReLU(hiddenNodes3);
        biasOutput = initializeBiasesReLU(outputNodes);
    }

    private double[][] initializeWeights(int fromSize, int toSize) {
        double[][] weights = new double[fromSize][toSize];
        for (int i = 0; i < fromSize; i++) {
            for (int j = 0; j < toSize; j++) {
                weights[i][j] = random.nextGaussian() * Math.sqrt(2.0 / (fromSize + toSize));
                weights[i][j] = Math.max(-1.0, Math.min(1.0, weights[i][j]));
            }
        }
        return weights;
    }

    private double[][] initializeWeightsXavier(int fromSize, int toSize) {
        double limit = Math.sqrt(6.0 / (fromSize + toSize));  // Xavier inicializálási határ
        double[][] weights = new double[fromSize][toSize];
        for (int i = 0; i < fromSize; i++) {
            for (int j = 0; j < toSize; j++) {
                weights[i][j] = random.nextDouble() * 2 * limit - limit;  // Egyenletes eloszlás [-limit, limit]
            }
        }
        return weights;
    }

    private double[][] initializeWeightsUniform(int fromSize, int toSize) {
        double limit = Math.sqrt(6.0 / (fromSize + toSize));  // Alapértelmezett határ
        double[][] weights = new double[fromSize][toSize];
        for (int i = 0; i < fromSize; i++) {
            for (int j = 0; j < toSize; j++) {
                weights[i][j] = random.nextDouble() * 2 * limit - limit;  // Egyenletes eloszlás [-limit, limit]
            }
        }
        return weights;
    }

    private double[][] initializeWeightsHE(int fromSize, int toSize) {
        double stdDev = Math.sqrt(2.0 / fromSize);  // He-inicializálás standard deviációja
        double[][] weights = new double[fromSize][toSize];
        for (int i = 0; i < fromSize; i++) {
            for (int j = 0; j < toSize; j++) {
                weights[i][j] = random.nextGaussian() * stdDev;  // Normális eloszlás
                //weights[i][j] = Math.max(-0.1, Math.min(0.1, weights[i][j])); // Korlátozzuk a kezdeti súlyokat
            }
        }
        return weights;
    }

    private double[] initializeBiases(int size) {
        double[] biases = new double[size];
        for (int i = 0; i < size; i++) {
            biases[i] = random.nextGaussian();
        }
        return biases;
    }

    private double[] initializeBiasesZero(int size) {
        return new double[size];  // Biasok nullára inicializálása
    }

    private double[] initializeSmall(int size) {
        double[] biases = new double[size];
        for (int i = 0; i < size; i++) {
            biases[i] = random.nextDouble() * 0.02 - 0.01;  // Kis véletlenszerű számok [-0.01, 0.01]
        }
        return biases;
    }

    private double[] initializeBiasesReLU(int size) {
        double[] biases = new double[size];
        // Kis pozitív érték, például 0.01, hogy segítsük a ReLU-t
        Arrays.fill(biases, 0.01);
        return biases;
    }

    public double activate(double x) {
        return activateReLU(x);
    }

    public double derivated(double x) {
        return derivativeReLU(x);
    }

    private double activateLeakyReLU(double x) {
        return x > 0 ? x : 0.01 * x;
    }

    private double derivativeLeakyReLU(double x) {
        return x > 0 ? 1 : 0.01;
    }

    private double activateReLU(double x) {
        return Math.max(0, x);
    }

    private double derivativeReLU(double x) {
        return x > 0 ? 1 : 0;
    }

    private double activateSigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private double derivativeSigmoid(double x) {
        double sigmoid = activateSigmoid(x);
        return sigmoid * (1 - sigmoid);
    }

    // ELU (Exponential Linear Unit) aktivációs függvény
    private double activateELU(double x) {
        return x > 0 ? x : 0.01 * (Math.exp(x) - 1);
    }

    private double derivativeELU(double x) {
        return x > 0 ? 1 : 0.01 * Math.exp(x);
    }

    private double scaleAndClipGradient(double grad) {
        double scaledGrad = grad * GRADIENT_SCALE;
        return Math.max(GRADIENT_CLIP_MIN, Math.min(GRADIENT_CLIP_MAX, scaledGrad));
    }

    /**
     * GetQ values, output layer values.
     *
     * @param state State data.
     *
     * @return output values.
     */
    public double[] getQValues(double[] state) {
        if (lastActivations == null) {
            initializeLastActivations();
        }
        double[] hidden1 = calculateLayerOutputs(state, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] hidden3 = calculateLayerOutputs(hidden2, weightsHidden2Hidden3, biasHidden3);
        double[] output = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);
        // Store activations for Visualization
        System.arraycopy(state, 0, lastActivations[0], 0, state.length);
        System.arraycopy(hidden1, 0, lastActivations[1], 0, hidden1.length);
        System.arraycopy(hidden2, 0, lastActivations[2], 0, hidden2.length);
        System.arraycopy(hidden3, 0, lastActivations[3], 0, hidden3.length);
        System.arraycopy(output, 0, lastActivations[4], 0, output.length);
        for (int i = 0; i < output.length; i++) {
            output[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, output[i]));
        }
        //return batchNormalize(output);
        return output;
    }

    private double[] calculateLayerOutputs(double[] inputs, double[][] weights, double[] biases) {
        double[] outputs = new double[weights[0].length];
        for (int i = 0; i < outputs.length; i++) {
            double sum = biases[i];
            for (int j = 0; j < inputs.length; j++) {
                double product = inputs[j] * weights[j][i];
                sum += product;
            }
            outputs[i] = activate(sum);
        }
        return batchNormalize(outputs);
    }

    /**
     * Select move action.
     *
     * @param state gameState
     *
     * @return selected movement
     */
    public int selectAction(double[] state) {
        if (random.nextDouble() < epsilon) {
            return random.nextInt(outputNodes);
        } else {
            double[] qvalues = getQValues(state);
            return argmax(qvalues);
        }
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

    /**
     * Neural network learning method.
     *
     * @param state game metric state
     * @param action action
     * @param reward reward points
     * @param nextState next game metric
     * @param gameEnded the game is ended
     */
    public void learn(double[] state, int action, double reward, double[] nextState, boolean gameEnded) {
        // Előre átviteli számítások egyszer
        double[] hidden1 = calculateLayerOutputs(state, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] hidden3 = calculateLayerOutputs(hidden2, weightsHidden2Hidden3, biasHidden3);
        double[] currentQValues = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);
        for (int i = 0; i < currentQValues.length; i++) {
            currentQValues[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, currentQValues[i]));  // Korlátozott Q-értékek
        }
        // Következő állapot Q-értékeinek kiszámítása
        double[] nextHidden1 = calculateLayerOutputs(nextState, weightsInputHidden1, biasHidden1);
        double[] nextHidden2 = calculateLayerOutputs(nextHidden1, weightsHidden1Hidden2, biasHidden2);
        double[] nextHidden3 = calculateLayerOutputs(nextHidden2, weightsHidden2Hidden3, biasHidden3);
        double[] nextQValues = calculateLayerOutputs(nextHidden3, weightsHidden3Output, biasOutput);
        for (int i = 0; i < nextQValues.length; i++) {
            nextQValues[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, nextQValues[i]));  // Korlátozott Q-értékek
        }
        double maxNextQ = max(nextQValues);
        maxNextQValue = maxNextQ;
        double target = reward + discountFactor * maxNextQ;
        target = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, target));
        double currentQ = currentQValues[action];
        double error = target - currentQ;
        error = scaleAndClipGradient(error);
        backpropagate(state, action, error, hidden1, hidden2, hidden3);
        updateEpsilon();
        if (gameEnded) {
            updateDiscountFactor();
            updateLearningrate();
            episodeCount++;
            if (reward > bestScore) {
                bestScore = reward;
            }
        }
        this.rewardValue = reward;
    }

    private double[] batchNormalize(double[] inputs) {
        double mean = 0;
        for (double input : inputs) {
            mean += input;
        }
        mean /= inputs.length;
        double variance = 0;
        for (double input : inputs) {
            variance += Math.pow(input - mean, 2);
        }
        variance /= inputs.length;
        double[] normalized = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            normalized[i] = (inputs[i] - mean) / Math.sqrt(variance + 1e-5);
        }
        return normalized;
    }

    private void updateEpsilon() {
        if (episodeCount % 1000 == 0) {
            epsilon = Math.min(INITIAL_EPSILON, epsilon * 2);
        } else {
            epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
        }
    }

    private void updateLearningrate() {
        learningRate = Math.max(MIN_LEARNING_RATE, learningRate * LEARNING_RATE_DECAY);
    }

    private void updateDiscountFactor() {
        discountFactor = Math.min(MAX_DISCOUNT_FACTOR, discountFactor + DISCOUNT_FACTOR_INCREMENT);
    }

    /**
     * Get maximum value from array.
     *
     * @param array
     *
     * @return max output value
     */
    public double max(double[] array) {
        double maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
            }
        }
        return maxValue;
    }

    private void backpropagate(double[] state, int action, double error, double[] hidden1, double[] hidden2, double[] hidden3) {
        double[] outputs = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);
        double[] deltaOutput = calculateOutputDelta(outputs, action, error);
        updateWeightsAndBiases(weightsHidden3Output, biasOutput, deltaOutput, hidden3, outputNodes);
        double[] deltaHidden3 = calculateHiddenDelta(deltaOutput, weightsHidden3Output, hidden3, hiddenNodes3);
        updateWeightsAndBiases(weightsHidden2Hidden3, biasHidden3, deltaHidden3, hidden2, hiddenNodes3);
        double[] deltaHidden2 = calculateHiddenDelta(deltaHidden3, weightsHidden2Hidden3, hidden2, hiddenNodes2);
        updateWeightsAndBiases(weightsHidden1Hidden2, biasHidden2, deltaHidden2, hidden1, hiddenNodes2);
        double[] deltaHidden1 = calculateHiddenDelta(deltaHidden2, weightsHidden1Hidden2, hidden1, hiddenNodes1);
        updateWeightsAndBiases(weightsInputHidden1, biasHidden1, deltaHidden1, state, hiddenNodes1);
    }

    private double[] calculateOutputDelta(double[] outputs, int action, double error) {
        double[] delta = new double[outputNodes];
        for (int i = 0; i < outputNodes; i++) {
            delta[i] = (i == action ? error : 0) * derivativeLeakyReLU(outputs[i]);
            delta[i] = scaleAndClipGradient(delta[i]);
        }
        return delta;
    }

    private double[] calculateHiddenDelta(double[] nextLayerDelta, double[][] weights, double[] layerOutputs, int layerSize) {
        double[] delta = new double[layerSize];
        for (int i = 0; i < layerSize; i++) {
            double sum = 0;
            for (int j = 0; j < nextLayerDelta.length; j++) {
                sum += nextLayerDelta[j] * weights[i][j];
            }
            delta[i] = sum * derivativeLeakyReLU(layerOutputs[i]);
            delta[i] = scaleAndClipGradient(delta[i]);
        }
        return delta;
    }

    private void updateWeightsAndBiases(double[][] weights, double[] biases, double[] delta, double[] inputs, int layerSize) {
        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < layerSize; j++) {
                double weightDelta = learningRate * (delta[j] * inputs[i] - L2_LAMBDA * weights[i][j]);
                weightDelta = scaleAndClipGradient(weightDelta);
                weights[i][j] += learningRate * weightDelta;
            }
        }
        for (int i = 0; i < layerSize; i++) {
            biases[i] += learningRate * delta[i] * 0.1;
        }
    }

    /***
     * Save network object data to file.
     *
     * @throws IOException exception
     */
    public void saveToFile() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            oos.writeObject(this);
        }
    }

    /***
     * Load network object from file.
     *
     * @return loaded network object.
     * @throws IOException exception
     * @throws ClassNotFoundException exception
     */
    public static NeuralNetworkQLearning loadFromFile() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILENAME))) {
            System.out.println("Network loaded from file");
            return (NeuralNetworkQLearning) ois.readObject();
        }
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

    /***
     * Get all weight data for visualization.
     *
     * @return all weight array in array
     */
    public double[][][] getAllWeights() {
        return new double[][][] {
                weightsInputHidden1,
                weightsHidden1Hidden2,
                weightsHidden2Hidden3,
                weightsHidden3Output
        };
    }

    public double getReward() {
        return rewardValue;
    }

    public double getMaxNextQValue() {
        return maxNextQValue;
    }

    public double getBestScoreValue() {
        return bestScore;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    /**
     * Get last activation data for visualization.
     *
     * @return activation data
     */
    public double[][] getLastActivations() {
        if (lastActivations == null) {
            initializeLastActivations();
        }
        return lastActivations;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeLastActivations();
    }



}
