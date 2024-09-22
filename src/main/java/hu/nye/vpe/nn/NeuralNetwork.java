package hu.nye.vpe.nn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Neural network class.
 */
public class NeuralNetwork implements Serializable, Cloneable {
    private static final long serialVersionUID = 2L;
    private static final double INITIAL_LEARNING_RATE = 0.1;
    private static final double INITIAL_MOMENTUM = 0.9;
    private static final int POPULATION_SIZE = 160;
    private static final double MUTATION_RATE = 0.3;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_STRENGTH = 0.7;
    private static final int GENERATION_WITHOUT_IMPROVED = 30;
    private static final int ELITE_SIZE_DIVIDER = 5;
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
    private double fitness;
    private double bestFitness = Double.NEGATIVE_INFINITY;
    private NeuralNetwork bestNetwork;
    private int clearedLines;
    private int holes;
    private double score;
    private int maxHeight;
    private double bumpiness;
    private int generationsWithoutImprovement;
    private AdaptiveLearningRate adaptiveLearningRate;

    public NeuralNetwork(final int inputNodes, final int hiddenNodes1, final int hiddenNodes2, final int outputNodes) {
        this(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes, true);
    }

    private NeuralNetwork(final int inputNodes, final int hiddenNodes1, final int hiddenNodes2, final int outputNodes, boolean initialize) {
        this.inputNodes = inputNodes;
        this.hiddenNodes1 = hiddenNodes1;
        this.hiddenNodes2 = hiddenNodes2;
        this.outputNodes = outputNodes;
        this.random = new Random();
        this.weightsInputHidden1 = new double[inputNodes][this.hiddenNodes1];
        this.weightsHidden1Hidden2 = new double[hiddenNodes1][hiddenNodes2];
        this.weightsHidden2Output = new double[this.hiddenNodes2][outputNodes];
        this.biasHidden1 = new double[this.hiddenNodes1];
        this.biasHidden2 = new double[this.hiddenNodes2];
        this.biasOutput = new double[outputNodes];
        this.adaptiveLearningRate = new AdaptiveLearningRate(INITIAL_LEARNING_RATE, INITIAL_MOMENTUM);
        this.generationsWithoutImprovement = 0;
        if (initialize) {
            loadOrInitialize();
        }
        this.bestFitness = 0;
    }

    private void loadOrInitialize() {
        try {
            NeuralNetwork loadedNetwork = loadFromFile("best_network.dat");
            if (loadedNetwork != null &&
                    loadedNetwork.inputNodes == this.inputNodes &&
                    loadedNetwork.hiddenNodes1 == this.hiddenNodes1 &&
                    loadedNetwork.outputNodes == this.outputNodes) {
                this.weightsInputHidden1 = loadedNetwork.weightsInputHidden1;
                this.weightsHidden2Output = loadedNetwork.weightsHidden2Output;
                this.biasHidden1 = loadedNetwork.biasHidden1;
                this.biasOutput = loadedNetwork.biasOutput;
                System.out.println("Saved network loaded succesfully.");
            } else {
                initialize();
                System.out.println("New network initialized.");
            }
        } catch (IOException | ClassNotFoundException e) {
            initialize();
            System.out.println("Error during loading, new network initialized.");
        }
        this.bestNetwork = this;
    }

    private void initialize() {
        // Xavier initialization for weights
        double inputScale = Math.sqrt(2.0 / inputNodes);
        double hidden1Scale = Math.sqrt(2.0 / hiddenNodes1);
        double hidden2Scale = Math.sqrt(2.0 / hiddenNodes2);

        for (int i = 0; i < inputNodes; i++) {
            for (int j = 0; j < hiddenNodes1; j++) {
                weightsInputHidden1[i][j] = random.nextGaussian() * inputScale;
            }
        }
        for (int i = 0; i < hiddenNodes1; i++) {
            for (int j = 0; j < hiddenNodes2; j++) {
                weightsHidden1Hidden2[i][j] = random.nextGaussian() * hidden1Scale;
            }
            biasHidden1[i] = random.nextGaussian();
        }
        for (int i = 0; i < hiddenNodes2; i++) {
            for (int j = 0; j < outputNodes; j++) {
                weightsHidden2Output[i][j] = random.nextGaussian() * hidden2Scale;
            }
            biasHidden2[i] = random.nextGaussian();
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
        // Calculate first hidden layer activations
        double[] hiddenLayer1 = new double[hiddenNodes1];
        for (int i = 0; i < hiddenNodes1; i++) {
            double sum = 0;
            for (int j = 0; j < inputNodes; j++) {
                sum += inputs[j] * weightsInputHidden1[j][i];
            }
            hiddenLayer1[i] = activate(sum + biasHidden1[i]);
        }

        // Calculate second hidden layer activations
        double[] hiddenLayer2 = new double[hiddenNodes2];
        for (int i = 0; i < hiddenNodes2; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes1; j++) {
                sum += hiddenLayer1[j] * weightsHidden1Hidden2[j][i];
            }
            hiddenLayer2[i] = activate(sum + biasHidden2[i]);
        }

        // Calculate output layer activations
        double[] outputs = new double[outputNodes];
        for (int i = 0; i < outputNodes; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenNodes2; j++) {
                sum += hiddenLayer2[j] * weightsHidden2Output[j][i];
            }
            outputs[i] = activate(sum + biasOutput[i]);
        }

        return outputs;
    }

    private double activate(double x) {
        // ReLU activation function
        //return Math.max(0, x);
        // Leaky ReLU
        return x > 0 ? x : 0.01 * x;
    }

    private double lerp(double variableA, double variableB, double variablet) {
        return variableA + (variableB - variableA) * variablet;
    }

    /**
     * Calculates the fitness of the network based on score, cleared lines, and holes.
     *
     * @param score The game score
     * @param clearedLines Number of cleared lines
     * @param holes Number of holes in the game state
     */
    public void calculateFitness(double score, int clearedLines, int holes, int maxHeight, double bumpiness) {
        this.score = score;
        this.clearedLines = clearedLines;
        this.holes = holes;
        this.maxHeight = maxHeight;
        this.bumpiness = bumpiness;
        this.fitness = score * 5 + (clearedLines * 1000) - (holes * 20) - (maxHeight * 10) - (bumpiness * 30);
    }

    /**
     * Mutates the neural network's weights and biases.
     *
     * @param mutationRate The probability of each weight/bias being mutated
     * @param mutationStrength The maximum amount of change during mutation
     */
    public void mutate(double mutationRate, double mutationStrength) {
        Random rand = new Random();
        mutateWeights(weightsInputHidden1, mutationRate, mutationStrength, rand);
        mutateWeights(weightsHidden1Hidden2, mutationRate, mutationStrength, rand);
        mutateWeights(weightsHidden2Output, mutationRate, mutationStrength, rand);
        mutateBiases(biasHidden1, mutationRate, mutationStrength, rand);
        mutateBiases(biasHidden2, mutationRate, mutationStrength, rand);
        mutateBiases(biasOutput, mutationRate, mutationStrength, rand);
    }

    private void mutateWeights(double[][] weights, double mutationRate, double mutationStrength, Random rand) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                if (rand.nextDouble() < mutationRate) {
                    double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                    weights[i][j] = lerp(weights[i][j], weights[i][j] + change, rand.nextDouble());
                }
            }
        }
    }

    private void mutateBiases(double[] biases, double mutationRate, double mutationStrength, Random rand) {
        for (int i = 0; i < biases.length; i++) {
            if (rand.nextDouble() < mutationRate) {
                double change = (rand.nextDouble() * 2 - 1) * mutationStrength;
                biases[i] = lerp(biases[i], biases[i] + change, rand.nextDouble());
            }
        }
    }

    /**
     * Crossover two NN.
     *
     * @param other
     *
     * @return NN
     */
    public NeuralNetwork crossover(NeuralNetwork other) {
        NeuralNetwork child = new NeuralNetwork(inputNodes, hiddenNodes1, hiddenNodes1, outputNodes, false);
        child.weightsInputHidden1 = crossoverWeights(this.weightsInputHidden1, other.weightsInputHidden1);
        child.weightsHidden1Hidden2 = crossoverWeights(this.weightsHidden1Hidden2, other.weightsHidden1Hidden2);
        child.weightsHidden2Output = crossoverWeights(this.weightsHidden2Output, other.weightsHidden2Output);
        child.biasHidden1 = crossoverBiases(this.biasHidden1, other.biasHidden1);
        child.biasHidden2 = crossoverBiases(this.biasHidden2, other.biasHidden2);
        child.biasOutput = crossoverBiases(this.biasOutput, other.biasOutput);
        return child;
    }

    private double[][] crossoverWeights(double[][] weights1, double[][] weights2) {
        double[][] childWeights = new double[weights1.length][weights1[0].length];
        for (int i = 0; i < weights1.length; i++) {
            for (int j = 0; j < weights1[i].length; j++) {
                childWeights[i][j] = random.nextBoolean() ? weights1[i][j] : weights2[i][j];
            }
        }
        return childWeights;
    }

    private double[] crossoverBiases(double[] biases1, double[] biases2) {
        double[] childBiases = new double[biases1.length];
        for (int i = 0; i < biases1.length; i++) {
            childBiases[i] = random.nextBoolean() ? biases1[i] : biases2[i];
        }
        return childBiases;
    }

    /**
     * Evolválja a hálózatot az adaptív mutációs ráta használatával.
     *
     * @param score The score achieved by the current network
     * @param clearedLines Number of cleared lines
     * @param holes Number of holes in the game state
     * @param maxHeight Maximum height of the game board
     * @param bumpiness Measure of unevenness of the game board
     */
    public void evolve(double score, int clearedLines, int holes, int maxHeight, double bumpiness) {
        calculateFitness(score, clearedLines, holes, maxHeight, bumpiness);
        double newLearningRate = adaptiveLearningRate.updateLearningRate(fitness, bestFitness);
        List<NeuralNetwork> population = new ArrayList<>();
        population.add(this);
        for (int i = 1; i < POPULATION_SIZE; i++) {
            NeuralNetwork mutatedNetwork = this.clone();
            mutatedNetwork.mutate(newLearningRate, MUTATION_STRENGTH);
            population.add(mutatedNetwork);
        }
        List<NeuralNetwork> evolvedPopulation = evolvePopulation(population);
        NeuralNetwork newBestNetwork = evolvedPopulation.get(0);
        if (newBestNetwork.getFitness() > bestFitness) {
            this.bestFitness = newBestNetwork.getFitness();
            this.score = newBestNetwork.getScore();
            this.copyFrom(newBestNetwork);
            System.out.println("New best network =  " + this.bestFitness +
                    " (Score: " + newBestNetwork.getScore() +
                    ", Deleted rows: " + newBestNetwork.getClearedLines() +
                    ", Holes: " + newBestNetwork.getHoles() +
                    ", Max height: " + newBestNetwork.getMaxHeight() +
                    ", Bumpiness: " + newBestNetwork.getBumpiness() +
                    ", Learning rate: " + newLearningRate + ")");
            try {
                saveBestToFile("best_network.dat");
            } catch (IOException e) {
                System.err.println("Error during saving network: " + e.getMessage());
            }
            generationsWithoutImprovement = 0;
        } else {
            this.mutate(newLearningRate * 2, MUTATION_STRENGTH * 2);
            System.out.println("Failing to improve the best fitness. Current best fitness: " + this.bestFitness +
                    ", Learning rate: " + newLearningRate);
            generationsWithoutImprovement++;
        }
        randomReinitialization();
    }

    private void copyFrom(NeuralNetwork other) {
        this.weightsInputHidden1 = deepCopyMatrix(other.weightsInputHidden1);
        this.weightsHidden1Hidden2 = deepCopyMatrix(other.weightsHidden1Hidden2);
        this.weightsHidden2Output = deepCopyMatrix(other.weightsHidden2Output);
        this.biasHidden1 = other.biasHidden1.clone();
        this.biasHidden2 = other.biasHidden2.clone();
        this.biasOutput = other.biasOutput.clone();
        this.fitness = other.fitness;
        this.score = other.score;
        this.clearedLines = other.clearedLines;
        this.holes = other.holes;
        this.maxHeight = other.maxHeight;
        this.bumpiness = other.bumpiness;
        this.generationsWithoutImprovement = other.generationsWithoutImprovement;
    }

    /**
     * Random reinitialization.
     */
    public void randomReinitialization() {
        if (generationsWithoutImprovement > GENERATION_WITHOUT_IMPROVED) {
            initialize();
            generationsWithoutImprovement = 0;
            System.out.println("Network randomly re-initialised.");
        }
    }

    private List<NeuralNetwork> evolvePopulation(List<NeuralNetwork> population) {
        List<NeuralNetwork> newPopulation = new ArrayList<>();
        int eliteSize = POPULATION_SIZE / ELITE_SIZE_DIVIDER;
        population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
        newPopulation.addAll(population.subList(0, eliteSize));
        List<NeuralNetwork> parents = stochasticUniversalSampling(population, POPULATION_SIZE - eliteSize);

        for (int i = 0; i < parents.size(); i += 2) {
            NeuralNetwork parent1 = parents.get(i);
            NeuralNetwork parent2 = (i + 1 < parents.size()) ? parents.get(i + 1) : parents.get(0);

            if (Math.random() < CROSSOVER_RATE) {
                NeuralNetwork child = parent1.crossover(parent2);
                if (Math.random() < MUTATION_RATE) {
                    child.mutate(adaptiveLearningRate.getLearningRate(), MUTATION_STRENGTH);
                }
                newPopulation.add(child);
            } else {
                newPopulation.add(Math.random() < 0.5 ? parent1.clone() : parent2.clone());
            }
        }
        return newPopulation;
    }

    private List<NeuralNetwork> stochasticUniversalSampling(List<NeuralNetwork> population, int numSelect) {
        double totalFitness = population.stream().mapToDouble(NeuralNetwork::getFitness).sum();
        double step = totalFitness / numSelect;
        double start = Math.random() * step;

        List<NeuralNetwork> selected = new ArrayList<>();
        double currentSum = 0;
        int currentIndex = 0;

        for (int i = 0; i < numSelect; i++) {
            double pointer = start + i * step;
            while (currentSum < pointer) {
                currentSum += population.get(currentIndex).getFitness();
                currentIndex = (currentIndex + 1) % population.size();
            }
            selected.add(population.get(currentIndex));
        }

        return selected;
    }

    /**
     * Clones the current neural network.
     *
     * @return A deep copy of the current neural network
     */
    @Override
    public NeuralNetwork clone() {
        try {
            NeuralNetwork clone = (NeuralNetwork) super.clone();
            clone.weightsInputHidden1 = deepCopyMatrix(this.weightsInputHidden1);
            clone.weightsHidden1Hidden2 = deepCopyMatrix(this.weightsHidden1Hidden2);
            clone.weightsHidden2Output = deepCopyMatrix(this.weightsHidden2Output);
            clone.biasHidden1 = this.biasHidden1.clone();
            clone.biasHidden2 = this.biasHidden2.clone();
            clone.biasOutput = this.biasOutput.clone();
            clone.adaptiveLearningRate = this.adaptiveLearningRate.clone();
            clone.bestNetwork = this;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen
        }
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
            System.out.println("The saved file does not exist, a new network will be initialized.");
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

    /**
     * Getters.
     */
    public double getFitness() {
        return fitness;
    }

    public double getScore() {
        return score;
    }

    public int getClearedLines() {
        return clearedLines;
    }

    public int getHoles() {
        return holes;
    }

    public double[][] getWeightsInputHidden1() {
        return weightsInputHidden1;
    }

    public double[][] getWeightsHidden2Output() {
        return weightsHidden2Output;
    }

    public double[] getBiasHidden1() {
        return biasHidden1;
    }

    public double[] getBiasOutput() {
        return biasOutput;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public double getBumpiness() {
        return bumpiness;
    }

    /**
     * Setters.
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public void setWeightsInputHidden1(double[][] weights) {
        this.weightsInputHidden1 = weights;
    }

    public void setWeightsHidden2Output(double[][] weights) {
        this.weightsHidden2Output = weights;
    }

    public void setBiasHidden1(double[] bias) {
        this.biasHidden1 = bias;
    }

    public void setBiasOutput(double[] bias) {
        this.biasOutput = bias;
    }
}
