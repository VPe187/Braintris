package hu.nye.vpe.nn;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeuralNetwork implements Serializable {
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

    private static final String FILENAME = "brain.dat";
    private static final double CLIP_MIN = -1.0;
    private static final double CLIP_MAX = 1.0;
    private static final double CLIP_NORM = 1.0;
    private static final double GRADIENT_SCALE = 1.0;

    private static final double INITIAL_LEARNING_RATE = 0.01;
    private static final double LEARNING_RATE_DECAY = 0.999;
    private static final double MIN_LEARNING_RATE = 0.00001;

    private static final double INITIAL_DISCOUNT_FACTOR = 0.95;
    private static final double MAX_DISCOUNT_FACTOR = 0.99;
    private static final double DISCOUNT_FACTOR_INCREMENT = 0.00001;

    private static final double INITIAL_EPSILON = 0.6;
    private static final double EPSILON_DECAY = 0.995;
    private static final double MIN_EPSILON = 0.01;

    private static final double MIN_Q = -1000;
    private static final double MAX_Q = 1000;

    public NeuralNetwork(String[] names, int[] layerSizes, Activation[] activations, WeightInitStrategy[] initStrategies,
                         boolean[] useBatchNorm, double[] l2) {
        if (layerSizes.length != activations.length + 1 ||
                layerSizes.length != initStrategies.length + 1 ||
                layerSizes.length != useBatchNorm.length + 1) {
            throw new IllegalArgumentException("Invalid configuration: layerSizes length should be one more than the length of activations, initStrategies, and useBatchNorm arrays");
        }

        this.gradientClipper = new GradientClipper(CLIP_MIN, CLIP_MAX, CLIP_NORM, GRADIENT_SCALE);
        this.layers = new ArrayList<>();
        for (int i = 1; i < layerSizes.length; i++) {
            layers.add(new Layer(names[i - 1], layerSizes[i - 1], layerSizes[i], activations[i - 1], initStrategies[i - 1],
                    useBatchNorm[i - 1], gradientClipper, l2[i - 1]));
        }

        this.learningRate = INITIAL_LEARNING_RATE;
        this.discountFactor = INITIAL_DISCOUNT_FACTOR;
        this.epsilon = INITIAL_EPSILON;
        this.episodeCount = 0;
        this.bestScore = 0.0;
        this.random = new Random();
    }

    public double[] forward(double[] inputs) {
        double[] currentInput = inputs;
        this.lastActivations = new double[layers.size() + 1][];
        this.lastActivations[0] = inputs;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            currentInput = layer.forward(currentInput, true);
            this.lastActivations[i + 1] = currentInput;
        }

        double[] output = currentInput;

        // Dinamikus Q-érték klippelés a teljesítmény függvényében
        double performanceFactor = Math.exp(-bestScore / 100.0); // Példa: teljesítményhez igazított faktor
        for (int i = 0; i < output.length; i++) {
            //output[i] = Math.max(MIN_Q * performanceFactor, Math.min(MAX_Q * performanceFactor, output[i]));
            output[i] = Math.max(MIN_Q, Math.min(MAX_Q, output[i]));
        }

        updateMaxQValue(currentInput);
        return currentInput;
    }

    private void updateMaxQValue(double[] qValues) {
        double max = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > max) {
                max = qValues[i];
            }
        }
        this.maxQValue = max;
    }

    private void backward(double[] inputs, double[] targets) {
        double[] currentInputs = inputs;
        List<double[]> allOutputs = new ArrayList<>();
        allOutputs.add(inputs);

        // Forward pass to collect all outputs
        for (Layer layer : layers) {
            currentInputs = layer.forward(currentInputs, false);
            allOutputs.add(currentInputs);
        }

        // Backward pass
        double[] deltas = new double[layers.get(layers.size() - 1).getSize()];
        double[] outputs = allOutputs.get(allOutputs.size() - 1);
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = targets[i] - outputs[i];
        }

        // Súlyok frissítése
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer currentLayer = layers.get(i);
            LayerGradients gradients = currentLayer.backward(deltas, learningRate);
            deltas = gradientClipper.scaleAndClip(gradients.inputGradients);
        }
    }

    public int selectAction(double[] state) {
        if (random.nextDouble() < epsilon) {
            return random.nextInt(layers.get(layers.size() - 1).getSize());
        } else {
            double[] qValues = forward(state);
            return argmax(qValues);
        }
    }

    public void learn(double[] state, int action, double reward, double[] nextState, boolean gameOver) {
        double[] currentQValues = forward(state);
        double[] nextQValues = forward(nextState);
        double maxNextQ = max(nextQValues);

        double normalizedReward = reward / Math.sqrt(reward * reward + 1);
        double target = normalizedReward + (gameOver ? 0 : discountFactor * maxNextQ);
        target = Math.max(MIN_Q, Math.min(MAX_Q, target));
        /*
        double target = reward;
        if (!gameOver) {
            target += discountFactor * maxNextQ;
        }
         */

        double[] targetQValues = currentQValues.clone();
        targetQValues[action] = target;

        backward(state, targetQValues);

        lastReward = reward;

        if (gameOver) {
            if (reward > this.bestScore) {
                this.bestScore = reward;
            }
            updateEpsilon();
            updateDiscountFactor();
            updateLearningRate();
            this.episodeCount++;
        }

    }

    private void updateEpsilon() {
        //double performanceFactor = Math.exp(-bestScore / 1000); // Példa alapú dinamikus frissítés
        //epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY * performanceFactor);
        epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
    }

    private void updateLearningRate() {
        //double performanceFactor = Math.exp(-bestScore / 1000); // Példa alapú dinamikus frissítés
        //learningRate = Math.max(MIN_LEARNING_RATE, learningRate * LEARNING_RATE_DECAY * performanceFactor);
        learningRate = Math.max(MIN_LEARNING_RATE, learningRate * LEARNING_RATE_DECAY);
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

    public void saveToFile() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            oos.writeObject(this);
        }
    }

    public static NeuralNetwork loadFromFile() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILENAME))) {
            return (NeuralNetwork) ois.readObject();
        }
    }

    // Getters
    public double[][][] getAllWeights() {
        double[][][] allWeights = new double[layers.size()][][];
        for (int i = 0; i < layers.size(); i++) {
            Layer currentLayer = layers.get(i);
            List<Neuron> neurons = currentLayer.getNeurons();
            allWeights[i] = new double[neurons.size()][];
            for (int j = 0; j < neurons.size(); j++) {
                allWeights[i][j] = neurons.get(j).getWeights();
            }
        }
        return allWeights;
    }

    public int[] getLayerSizes() {
        int[] sizes = new int[layers.size() + 1];
        sizes[0] = layers.get(0).getNeurons().get(0).getWeights().length;
        for (int i = 0; i < layers.size(); i++) {
            sizes[i + 1] = layers.get(i).getNeurons().size();
        }
        return sizes;
    }

    public String[] getLayerNames() {
        String[] names = new String[layers.size() + 1];
        names[0] = "INP";
        for (int i = 0; i < layers.size(); i++) {
            names[i + 1] = layers.get(i).getName();
        }
        return names;
    }

    public double[][] getLastActivations() {
        if (this.lastActivations == null) {
            // Ha még nem történt forward pass, inicializáljunk egy üres aktivációs tömböt
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
}