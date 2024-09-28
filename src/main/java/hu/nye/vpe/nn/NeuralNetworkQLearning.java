package hu.nye.vpe.nn;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class NeuralNetworkQLearning implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int HIDDEN_NODES1 = 32;
    private static final int HIDDEN_NODES2 = 16;
    private static final int HIDDEN_NODES3 = 8;
    private static final double INITIAL_LEARNING_RATE = 0.01;
    private static final double LEARNING_RATE_DECAY = 0.9999;
    private static final double MIN_LEARNING_RATE = 0.0001;
    private static final double INITIAL_DISCOUNT_FACTOR = 0.6;
    private static final double MAX_DISCOUNT_FACTOR = 0.99;
    private static final double DISCOUNT_FACTOR_INCREMENT = 0.0001;
    private static final double INITIAL_EPSILON = 0.6;
    private static final double EPSILON_DECAY = 0.9999;
    private static final double MIN_EPSILON = 0.01;
    private static final double MIN_Q_VALUE = -50;
    private static final double MAX_Q_VALUE = 50;
    private static final double L2_LAMBDA = 0.1;
    private static final double CLIP_MIN = -0.9;
    private static final double CLIP_MAX = 0.9;
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
        loadOrInitialize();
        initializeLastActivations();
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
            System.out.println("Error during loading, new network initialized");
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

    public double[] getQValues(double[] state) {
        if (lastActivations == null) {
            initializeLastActivations();
        }

        double[] hidden1 = calculateLayerOutputs(state, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] hidden3 = calculateLayerOutputs(hidden2, weightsHidden2Hidden3, biasHidden3);
        double[] output = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);

        // Store activations
        System.arraycopy(state, 0, lastActivations[0], 0, state.length);
        System.arraycopy(hidden1, 0, lastActivations[1], 0, hidden1.length);
        System.arraycopy(hidden2, 0, lastActivations[2], 0, hidden2.length);
        System.arraycopy(hidden3, 0, lastActivations[3], 0, hidden3.length);
        System.arraycopy(output, 0, lastActivations[4], 0, output.length);

        for (int i = 0; i < output.length; i++) {
            output[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, output[i]));
        }
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

    public double activate(double x) {
        return activateELU(x);
    }

    public double derivated(double x) {
        return derivativeELU(x);
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
            double[] qValues = getQValues(state);
            return argmax(qValues);
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

    private double[] forwardPass (double[] input) {
        double[] hidden1 = calculateLayerOutputs(input, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] hidden3 = calculateLayerOutputs(hidden2, weightsHidden2Hidden3, biasHidden3);
        double[] output = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);
        for (int i = 0; i < output.length; i++) {
            output[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, output[i]));  // Korlátozott Q-értékek
        }
        return output;
    }

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
        error = Math.max(CLIP_MIN, Math.min(CLIP_MAX, error));  // Gradient clipping
        //error *= 20.0; // Gradient scaling
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
        // Kimeneti réteg kimeneteinek kiszámítása
        double[] outputs = calculateLayerOutputs(hidden3, weightsHidden3Output, biasOutput);

        // Kimeneti réteg (output) delta számítása
        double[] deltaOutput = new double[outputNodes];
        for (int i = 0; i < outputNodes; i++) {
            deltaOutput[i] = (i == action ? error : 0) * derivativeLeakyReLU(outputs[i]);
        }

        // Hidden3 -> Output súlyok frissítése
        for (int i = 0; i < hiddenNodes3; i++) {
            for (int j = 0; j < outputNodes; j++) {
                double delta = learningRate * (deltaOutput[j] * hidden3[i] - L2_LAMBDA * weightsHidden3Output[i][j]);
                weightsHidden3Output[i][j] += delta;
            }
        }

        // Output bias frissítése
        for (int i = 0; i < outputNodes; i++) {
            biasOutput[i] += learningRate * deltaOutput[i] * 0.1;
        }

        // Hidden2 -> Hidden3 delta számítása
        double[] deltaHidden3 = new double[hiddenNodes3];
        for (int i = 0; i < hiddenNodes3; i++) {
            double sum = 0;
            for (int j = 0; j < outputNodes; j++) {
                sum += deltaOutput[j] * weightsHidden3Output[i][j];
            }
            deltaHidden3[i] = sum * derivativeLeakyReLU(hidden3[i]);
        }

        // Hidden2 -> Hidden3 súlyok frissítése
        for (int i = 0; i < hiddenNodes2; i++) {
            for (int j = 0; j < hiddenNodes3; j++) {
                double delta = learningRate * (deltaHidden3[j] * hidden2[i] - L2_LAMBDA * weightsHidden2Hidden3[i][j]);
                weightsHidden2Hidden3[i][j] += delta;
            }
        }

        // Hidden3 bias frissítése
        for (int i = 0; i < hiddenNodes3; i++) {
            biasHidden3[i] += learningRate * deltaHidden3[i] * 0.1;
        }

        // Hidden1 -> Hidden2 delta számítása
        double[] deltaHidden2 = new double[hiddenNodes2];
        for (int i = 0; i < hiddenNodes2; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes3; j++) {
                sum += deltaHidden3[j] * weightsHidden2Hidden3[i][j];
            }
            deltaHidden2[i] = sum * derivativeLeakyReLU(hidden2[i]);
        }

        // Hidden1 -> Hidden2 súlyok frissítése
        for (int i = 0; i < hiddenNodes1; i++) {
            for (int j = 0; j < hiddenNodes2; j++) {
                double delta = learningRate * (deltaHidden2[j] * hidden1[i] - L2_LAMBDA * weightsHidden1Hidden2[i][j]);
                weightsHidden1Hidden2[i][j] += delta;
            }
        }

        // Hidden2 bias frissítése
        for (int i = 0; i < hiddenNodes2; i++) {
            biasHidden2[i] += learningRate * deltaHidden2[i] * 0.1;
        }

        // Input -> Hidden1 delta számítása
        double[] deltaHidden1 = new double[hiddenNodes1];
        for (int i = 0; i < hiddenNodes1; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes2; j++) {
                sum += deltaHidden2[j] * weightsHidden1Hidden2[i][j];
            }
            deltaHidden1[i] = sum * derivativeLeakyReLU(hidden1[i]);
        }

        // Input -> Hidden1 súlyok frissítése
        for (int i = 0; i < inputNodes; i++) {
            for (int j = 0; j < hiddenNodes1; j++) {
                double delta = learningRate * (deltaHidden1[j] * state[i] - L2_LAMBDA * weightsInputHidden1[i][j]);
                weightsInputHidden1[i][j] += delta;
            }
        }

        // Hidden1 bias frissítése
        for (int i = 0; i < hiddenNodes1; i++) {
            biasHidden1[i] += learningRate * deltaHidden1[i] * 0.1;
        }
    }


    public void saveToFile() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            oos.writeObject(this);
        }
    }

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

    private void printWeightSamples() {
        System.out.println("Input -> Hidden1: " + weightsInputHidden1[0][0]);
        System.out.println("Hidden1 -> Hidden2: " + weightsHidden1Hidden2[0][0]);
        System.out.println("Hidden2 -> Hidden3: " + weightsHidden2Hidden3[0][0]);
        System.out.println("Hidden3 -> Output: " + weightsHidden3Output[0][0]);
    }

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

    // Gradiens skálázás hozzáadása
    private void scaleGradients(double[] gradients, double maxNorm) {
        double norm = 0;
        for (double grad : gradients) {
            norm += grad * grad;
        }
        norm = Math.sqrt(norm);

        if (norm > maxNorm) {
            double scale = maxNorm / norm;
            for (int i = 0; i < gradients.length; i++) {
                gradients[i] *= scale;
            }
        }
    }
}
