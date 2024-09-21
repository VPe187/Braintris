package hu.nye.vpe.nn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

/**
 * Neural network class.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int inputNodes;
    private final int hiddenNodes;
    private final int outputNodes;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;
    private final Random random;
    private double bestScore = Double.NEGATIVE_INFINITY;
    private NeuralNetwork bestNetwork;

    public NeuralNetwork(final int inputNodes, final int hiddenNodes, final int outputNodes) {
        this(inputNodes, hiddenNodes, outputNodes, true);
    }

    private NeuralNetwork(final int inputNodes, final int hiddenNodes, final int outputNodes, boolean initialize) {
        this.inputNodes = inputNodes;
        this.hiddenNodes = hiddenNodes;
        this.outputNodes = outputNodes;
        this.random = new Random();
        if (initialize) {
            loadOrInitialize();
        }
    }

    private void loadOrInitialize() {
        try {
            NeuralNetwork loadedNetwork = loadFromFile("best_network.dat");
            if (loadedNetwork != null &&
                    loadedNetwork.inputNodes == this.inputNodes &&
                    loadedNetwork.hiddenNodes == this.hiddenNodes &&
                    loadedNetwork.outputNodes == this.outputNodes) {

                this.weightsInputHidden = loadedNetwork.weightsInputHidden;
                this.weightsHiddenOutput = loadedNetwork.weightsHiddenOutput;
                this.biasHidden = loadedNetwork.biasHidden;
                this.biasOutput = loadedNetwork.biasOutput;
                this.bestScore = loadedNetwork.bestScore;
                System.out.println("Sikeresen betöltve a mentett hálózat.");
            } else {
                initialize();
                System.out.println("Új hálózat inicializálva.");
            }
        } catch (IOException | ClassNotFoundException e) {
            initialize();
            System.out.println("Hiba történt a betöltés során, új hálózat inicializálva.");
        }
        this.bestNetwork = this;
    }

    private void initialize() {
        weightsInputHidden = new double[ inputNodes ][ hiddenNodes ];
        weightsHiddenOutput = new double[hiddenNodes][outputNodes];
        biasHidden = new double[hiddenNodes];
        biasOutput = new double[outputNodes];

        // Xavier initialization for weights
        double inputScale = Math.sqrt(2.0 / inputNodes);
        double hiddenScale = Math.sqrt(2.0 / hiddenNodes);
        for (int i = 0; i < inputNodes; i++) {
            for (int j = 0; j < hiddenNodes; j++) {
                weightsInputHidden[i][j] = random.nextGaussian() * inputScale;
            }
        }
        for (int i = 0; i < hiddenNodes; i++) {
            for (int j = 0; j < outputNodes; j++) {
                weightsHiddenOutput[i][j] = random.nextGaussian() * hiddenScale;
            }
            biasHidden[i] = random.nextGaussian();
        }
        for (int i = 0; i < outputNodes; i++) {
            biasOutput[i] = random.nextGaussian();
        }
    }

    /**
     * Feed method.
     *
     * @param inputs
     *
     * @return outputs double
     */
    public double[] feedForward(double[] inputs) {
        if (inputs.length != inputNodes) {
            throw new IllegalArgumentException("Input size does not match network input nodes");
        }

        // Calculate hidden layer activations
        double[] hiddenLayer = new double[hiddenNodes];
        for (int i = 0; i < hiddenNodes; i++) {
            double sum = 0;
            for (int j = 0; j < inputNodes; j++) {
                sum += inputs[j] * weightsInputHidden[j][i];
            }
            hiddenLayer[i] = activate(sum + biasHidden[i]);
        }

        // Calculate output layer activations
        double[] outputs = new double[outputNodes];
        for (int i = 0; i < outputNodes; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes; j++) {
                sum += hiddenLayer[j] * weightsHiddenOutput[j][i];
            }
            outputs[i] = activate(sum + biasOutput[i]);
        }

        return outputs;
    }

    private double activate(double x) {
        // ReLU activation function
        return Math.max(0, x);
    }

    // Getter methods for weights and biases (useful for genetic algorithms)
    public double[][] getWeightsInputHidden() {
        return weightsInputHidden;
    }

    public double[][] getWeightsHiddenOutput() {
        return weightsHiddenOutput;
    }

    public double[] getBiasHidden() {
        return biasHidden;
    }

    public double[] getBiasOutput() {
        return biasOutput;
    }

    // Setter methods for weights and biases (useful for genetic algorithms)
    public void setWeightsInputHidden(double[][] weights) {
        this.weightsInputHidden = weights;
    }

    public void setWeightsHiddenOutput(double[][] weights) {
        this.weightsHiddenOutput = weights;
    }

    public void setBiasHidden(double[] bias) {
        this.biasHidden = bias;
    }

    public void setBiasOutput(double[] bias) {
        this.biasOutput = bias;
    }

    public double lerp(double variableA, double variableB, double variablet) {
        return variableA + (variableB - variableA) * variablet;
    }

    /**
     * Mutates the neural network's weights and biases.
     *
     * @param mutationRate The probability of each weight/bias being mutated
     * @param mutationStrength The maximum amount of change during mutation
     */
    public void mutate(double mutationRate, double mutationStrength) {
        Random rand = new Random();

        // Mutate weights from input to hidden layer
        for (int i = 0; i < inputNodes; i++) {
            for (int j = 0; j < hiddenNodes; j++) {
                if (rand.nextDouble() < mutationRate) {
                    double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                    weightsInputHidden[i][j] = lerp(weightsInputHidden[i][j], weightsInputHidden[i][j] + change, rand.nextDouble());
                }
            }
        }

        // Mutate weights from hidden to output layer
        for (int i = 0; i < hiddenNodes; i++) {
            for (int j = 0; j < outputNodes; j++) {
                if (rand.nextDouble() < mutationRate) {
                    double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                    weightsHiddenOutput[i][j] = lerp(weightsHiddenOutput[i][j], weightsHiddenOutput[i][j] + change, rand.nextDouble());
                }
            }
        }

        // Mutate biases for hidden layer
        for (int i = 0; i < hiddenNodes; i++) {
            if (rand.nextDouble() < mutationRate) {
                double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                biasHidden[i] = lerp(biasHidden[i], biasHidden[i] + change, rand.nextDouble());
            }
        }

        // Mutate biases for output layer
        for (int i = 0; i < outputNodes; i++) {
            if (rand.nextDouble() < mutationRate) {
                double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                biasOutput[i] = lerp(biasOutput[i], biasOutput[i] + change, rand.nextDouble());
            }
        }
    }

    /**
     * Evolves the network based on the given score.
     *
     * @param score The score achieved by the current network
     * @param mutationRate The rate of mutation if the network is to be mutated
     * @param mutationStrength The strength of mutation if the network is to be mutated
     */
    public void evolve(double score, double mutationRate, double mutationStrength) {
        if (score > bestScore) {
            bestScore = score;
            NeuralNetwork newBest = this.clone();
            newBest.mutate(mutationRate, mutationStrength);
            bestNetwork = newBest;
            System.out.println("Rekord pontszám: " + score);
            try {
                saveBestToFile("best_network.dat");
            } catch (IOException e) {
                System.err.println("Hiba a hálózat mentése során: " + e.getMessage());
            }
        } else {
            System.out.println("Gyenge teljesítmény, új kezdő hálózat létrehozva.");
            reinitialize();
        }
    }

    /**
     * Reinitializes the network with random weights and biases.
     */
    public void reinitialize() {
        initialize();
    }

    /**
     * Clones the current neural network.
     *
     * @return A deep copy of the current neural network
     */
    public NeuralNetwork clone() {
        NeuralNetwork clone = new NeuralNetwork(inputNodes, hiddenNodes, outputNodes, false);
        clone.weightsInputHidden = deepCopyMatrix(this.weightsInputHidden);
        clone.weightsHiddenOutput = deepCopyMatrix(this.weightsHiddenOutput);
        clone.biasHidden = this.biasHidden.clone();
        clone.biasOutput = this.biasOutput.clone();
        clone.bestScore = this.bestScore;
        clone.bestNetwork = this;
        return clone;
    }

    private double[][] deepCopyMatrix(double[][] matrix) {
        return java.util.Arrays.stream(matrix).map(double[]::clone).toArray(double[][]::new);
    }

    /**
     * Saves the best network to a file.
     *
     * @param fileName The name of the file to save the network to
     * @throws IOException If an I/O error occurs
     */
    public void saveBestToFile(String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(bestNetwork);
        }
    }

    /**
     * Loads a network from a file.
     *
     * @param fileName The name of the file to load the network from
     * @return The loaded NeuralNetwork
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If the class of a serialized object cannot be found
     */
    public static NeuralNetwork loadFromFile(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("A mentett fájl nem létezik, új hálózat lesz inicializálva.");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (NeuralNetwork) ois.readObject();
        }
    }

    /**
     * Gets the best network found so far.
     *
     * @return The best performing neural network
     */
    public NeuralNetwork getBestNetwork() {
        return bestNetwork;
    }
}
