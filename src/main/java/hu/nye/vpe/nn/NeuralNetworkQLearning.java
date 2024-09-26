package hu.nye.vpe.nn;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class NeuralNetworkQLearning implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final double INITIAL_LEARNING_RATE = 0.006;
    private static final double LEARNING_RATE_DECAY = 0.9999;
    private static final double MIN_LEARNING_RATE = 0.0005;

    private static final double INITIAL_DISCOUNT_FACTOR = 0.8;

    private static final double INITIAL_EPSILON = 0.8;
    private static final double EPSILON_DECAY = 0.9999;
    private static final double MIN_EPSILON = 0.01;

    private static final double MIN_Q_VALUE = -500;
    private static final double MAX_Q_VALUE = 500;
    private static final double CLIP_MIN = -10;
    private static final double CLIP_MAX = 10;
    private static final String FILENAME = "brain.dat";

    private final int inputNodes;
    private final int hiddenNodes1;
    private final int hiddenNodes2;
    private final int outputNodes;
    private double[][] weightsInputHidden1;
    private double[][] weightsHidden1Hidden2;
    private double[][] weightsHidden2Output;
    private double[] biasHidden1;
    private double[] biasHidden2;
    private double[] biasOutput;
    private final Random random;
    private double learningRate;
    private final double discountFactor;
    private double epsilon;
    private double bestScore = Double.NEGATIVE_INFINITY;

    public NeuralNetworkQLearning(int inputNodes, int hiddenNodes1, int hiddenNodes2, int outputNodes) {
        this.inputNodes = inputNodes;
        this.hiddenNodes1 = hiddenNodes1;
        this.hiddenNodes2 = hiddenNodes2;
        this.outputNodes = outputNodes;
        this.random = new Random();
        this.learningRate = INITIAL_LEARNING_RATE;
        this.discountFactor = INITIAL_DISCOUNT_FACTOR;
        this.epsilon = INITIAL_EPSILON;
        loadOrInitialize();
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
        weightsHidden2Output = initializeWeightsHE(hiddenNodes2, outputNodes);
        biasHidden1 = initializeBiasesReLU(hiddenNodes1);
        biasHidden2 = initializeBiasesReLU(hiddenNodes2);
        biasOutput = initializeBiasesReLU(outputNodes);
        normalizeWeights();
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
                weights[i][j] = Math.max(-0.1, Math.min(0.1, weights[i][j])); // Korlátozzuk a kezdeti súlyokat
            }
        }
        return weights;
    }

    private double[] initializeBiases(int size) {
        double[] biases = new double[size];
        for (int i = 0; i < size; i++) {
            biases[i] = random.nextGaussian();
            //biases[i] = Math.max(-1.0, Math.min(1.0, biases[i]));
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
        double[] hidden1 = calculateLayerOutputs(state, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] output = calculateLayerOutputs(hidden2, weightsHidden2Output, biasOutput);
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
            outputs[i] = activateLeakyReLU(sum);
        }
        return outputs;
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

    public int selectAction(double[] state) {

        if (random.nextDouble() < epsilon) {
            return random.nextInt(outputNodes);
        } else {
            double[] qValues = getQValues(state);
            return argmax(qValues);
        }
        /*
        double[] qValues = getQValues(state);
        for (int i = 0; i < qValues.length; i++) {
            System.out.print(qValues[i] + " ");
        }
        System.out.println();
        return 1;
        */
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

    public void learn(double[] state, int action, double reward, double[] nextState, boolean gameEnded) {
        // Előre átviteli számítások egyszer
        double[] hidden1 = calculateLayerOutputs(state, weightsInputHidden1, biasHidden1);
        double[] hidden2 = calculateLayerOutputs(hidden1, weightsHidden1Hidden2, biasHidden2);
        double[] currentQValues = calculateLayerOutputs(hidden2, weightsHidden2Output, biasOutput);
        for (int i = 0; i < currentQValues.length; i++) {
            currentQValues[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, currentQValues[i]));  // Korlátozott Q-értékek
        }
        // Következő állapot Q-értékeinek kiszámítása
        double[] nextHidden1 = calculateLayerOutputs(nextState, weightsInputHidden1, biasHidden1);
        double[] nextHidden2 = calculateLayerOutputs(nextHidden1, weightsHidden1Hidden2, biasHidden2);
        double[] nextQValues = calculateLayerOutputs(nextHidden2, weightsHidden2Output, biasOutput);
        //double[] nextQValues = getQValues(nextState);
        for (int i = 0; i < nextQValues.length; i++) {
            nextQValues[i] = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, nextQValues[i]));  // Korlátozott Q-értékek
        }
        double maxNextQ = max(nextQValues);
        double target = reward + discountFactor * maxNextQ;
        target = Math.max(MIN_Q_VALUE, Math.min(MAX_Q_VALUE, target));
        double currentQ = currentQValues[action];
        double error = target - currentQ;
        error = Math.max(CLIP_MIN, Math.min(CLIP_MAX, error));  // Gradient clipping
        backpropagate(state, action, error, hidden1, hidden2);
        decreaseLearningrate();
        decreaseEpsilon();
        if (gameEnded) {
            System.out.printf("Reward: %.4f: Record: %.4f, Learning rate: %.4f, Epsylon: %.4f, Q: %.4f", reward, bestScore, learningRate, epsilon, maxNextQ);
            if (reward > bestScore) {
                bestScore = reward;
                System.out.print(" * New record");
            }
            System.out.println();
        }
    }

    private void decreaseEpsilon() {
        epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
    }

    private void decreaseLearningrate() {
        learningRate = Math.max(MIN_LEARNING_RATE, learningRate * LEARNING_RATE_DECAY);
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

    private void normalizeWeights() {
        normalizeWeightMatrixLess(weightsInputHidden1);
        normalizeWeightMatrixLess(weightsHidden1Hidden2);
        normalizeWeightMatrixLess(weightsHidden2Output);
    }

    private void normalizeWeightMatrix(double[][] weights) {
        for (double[] row : weights) {
            double sum = 0;
            for (double weight : row) {
                sum += Math.abs(weight);
            }
            if (sum > 1e-8) {
                for (int i = 0; i < row.length; i++) {
                    row[i] /= sum;
                }
            } else {
                System.out.println("Súlyok nem normalizálódnak, mert a sum túl kicsi vagy nulla: " + sum);
            }
        }
    }

    private void normalizeWeightMatrixLess(double[][] weights) {
        for (double[] row : weights) {
            double sum = 0;
            for (double weight : row) {
                sum += Math.abs(weight);
            }
            if (sum > 1e-8) {
                for (int i = 0; i < row.length; i++) {
                    row[i] /= (sum * 2); // Enyhébb normalizálás
                }
            }
        }
    }

    private void backpropagate(double[] state, int action, double error, double[] hidden1, double[] hidden2) {
        double[] outputs = calculateLayerOutputs(hidden2, weightsHidden2Output, biasOutput);
        // Hidden2 -> Output
        double[] deltaOutput = new double[outputNodes];
        deltaOutput[action] = error * derivativeLeakyReLU(outputs[action]);
        for (int i = 0; i < hiddenNodes2; i++) {
            for (int j = 0; j < outputNodes; j++) {
                double delta = learningRate * deltaOutput[j] * hidden2[i];
                delta = Math.max(CLIP_MIN, Math.min(CLIP_MAX, delta));  // Gradient clipping
                weightsHidden2Output[i][j] += delta;
                if (Double.isNaN(weightsHidden2Output[i][j])) {
                    weightsHidden2Output[i][j] = random.nextDouble() * 0.01;
                }
            }
        }
        for (int i = 0; i < outputNodes; i++) {
            biasOutput[i] += learningRate * deltaOutput[i] * 0.1;
            biasOutput[i] = Math.max(-1.0, Math.min(1.0, biasOutput[i]));  // Biasok korlátozása
        }

        // Hidden1 -> Hidden2
        double[] deltaHidden2 = new double[hiddenNodes2];
        for (int i = 0; i < hiddenNodes2; i++) {
            double sum = 0;
            for (int j = 0; j < outputNodes; j++) {
                sum += deltaOutput[j] * weightsHidden2Output[i][j];
            }
            deltaHidden2[i] = sum * derivativeLeakyReLU(hidden2[i]);
        }
        for (int i = 0; i < hiddenNodes1; i++) {
            for (int j = 0; j < hiddenNodes2; j++) {
                double delta = learningRate * deltaHidden2[j] * hidden1[i];
                delta = Math.max(CLIP_MIN, Math.min(CLIP_MAX, delta));  // Gradient clipping
                weightsHidden1Hidden2[i][j] += delta;
            }
        }
        for (int i = 0; i < hiddenNodes2; i++) {
            biasHidden2[i] += learningRate * deltaHidden2[i] * 0.1;
            biasHidden2[i] = Math.max(-1.0, Math.min(1.0, biasHidden2[i]));
        }

        // Input -> Hidden1
        double[] deltaHidden1 = new double[hiddenNodes1];
        for (int i = 0; i < hiddenNodes1; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes2; j++) {
                sum += deltaHidden2[j] * weightsHidden1Hidden2[i][j];
            }
            deltaHidden1[i] = sum * derivativeLeakyReLU(hidden1[i]);
        }
        for (int i = 0; i < inputNodes; i++) {
            for (int j = 0; j < hiddenNodes1; j++) {
                double delta = learningRate * deltaHidden1[j] * state[i];
                delta = Math.max(CLIP_MIN, Math.min(CLIP_MAX, delta));  // Gradient clipping
                weightsInputHidden1[i][j] += delta;
            }
        }
        for (int i = 0; i < hiddenNodes1; i++) {
            biasHidden1[i] += learningRate * deltaHidden1[i] * 0.1;
            biasHidden1[i] = Math.max(-1.0, Math.min(1.0, biasHidden1[i]));  // Biasok korlátozása
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
}
