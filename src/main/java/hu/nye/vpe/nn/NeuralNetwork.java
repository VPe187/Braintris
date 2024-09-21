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
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 3L;
    private static final double INITIAL_MUTATION_RATE = 0.1;
    private static final double MIN_MUTATION_RATE = 0.001;
    private static final double MAX_MUTATION_RATE = 0.5;
    private static final double MUTATION_RATE_CHANGE_FACTOR = 1.05;
    private static final int POPULATION_SIZE = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.7;
    private final int inputNodes;
    private final int hiddenNodes;
    private final int outputNodes;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;
    private final Random random;
    private double fitness;
    private double bestScore = Double.NEGATIVE_INFINITY;
    private NeuralNetwork bestNetwork;
    private int clearedLines;
    private int holes;
    private double score;
    private int maxHeight;
    private double bumpiness;
    private double currentMutationRate;
    private double previousBestFitness;
    private int generationsWithoutImprovement;

    public NeuralNetwork(final int inputNodes, final int hiddenNodes, final int outputNodes) {
        this(inputNodes, hiddenNodes, outputNodes, true);
    }

    private NeuralNetwork(final int inputNodes, final int hiddenNodes, final int outputNodes, boolean initialize) {
        this.inputNodes = inputNodes;
        this.hiddenNodes = hiddenNodes;
        this.outputNodes = outputNodes;
        this.random = new Random();
        this.weightsInputHidden = new double[inputNodes][hiddenNodes];
        this.weightsHiddenOutput = new double[hiddenNodes][outputNodes];
        this.biasHidden = new double[hiddenNodes];
        this.biasOutput = new double[outputNodes];
        this.currentMutationRate = INITIAL_MUTATION_RATE;
        this.previousBestFitness = Double.NEGATIVE_INFINITY;
        this.generationsWithoutImprovement = 0;
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

    /**
     * Adaptively adjust the mutation rate based on performance.
     */
    private void adjustMutationRate() {
        if (fitness > previousBestFitness) {
            currentMutationRate /= MUTATION_RATE_CHANGE_FACTOR;
            generationsWithoutImprovement = 0;
        } else {
            currentMutationRate *= MUTATION_RATE_CHANGE_FACTOR;
            generationsWithoutImprovement++;
        }
        currentMutationRate = Math.max(MIN_MUTATION_RATE, Math.min(currentMutationRate, MAX_MUTATION_RATE));
        previousBestFitness = Math.max(previousBestFitness, fitness);
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
        this.fitness = score + (clearedLines * 100) - (holes * 10) - (maxHeight * 5) - (bumpiness * 2);
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
     * Crossover two NN.
     *
     * @param other
     *
     * @return NN
     */
    public NeuralNetwork crossover(NeuralNetwork other) {
        NeuralNetwork child = new NeuralNetwork(inputNodes, hiddenNodes, outputNodes, false);

        for (int i = 0; i < weightsInputHidden.length; i++) {
            for (int j = 0; j < weightsInputHidden[i].length; j++) {
                child.weightsInputHidden[i][j] = random.nextBoolean() ? this.weightsInputHidden[i][j] : other.weightsInputHidden[i][j];
            }
        }

        for (int i = 0; i < weightsHiddenOutput.length; i++) {
            for (int j = 0; j < weightsHiddenOutput[i].length; j++) {
                child.weightsHiddenOutput[i][j] = random.nextBoolean() ? this.weightsHiddenOutput[i][j] : other.weightsHiddenOutput[i][j];
            }
        }

        for (int i = 0; i < biasHidden.length; i++) {
            child.biasHidden[i] = random.nextBoolean() ? this.biasHidden[i] : other.biasHidden[i];
        }

        for (int i = 0; i < biasOutput.length; i++) {
            child.biasOutput[i] = random.nextBoolean() ? this.biasOutput[i] : other.biasOutput[i];
        }

        return child;
    }

    /**
     * Evolválja a hálózatot az adaptív mutációs ráta használatával.
     *
     * @param score The score achieved by the current network
     * @param clearedLines Number of cleared lines
     * @param holes Number of holes in the game state
     * @param maxHeight Maximum height of the game board
     * @param bumpiness Measure of unevenness of the game board
     * @param mutationStrength The strength of mutation if the network is to be mutated
     */
    public void evolve(double score, int clearedLines, int holes, int maxHeight, double bumpiness,
                       double mutationStrength) {
        calculateFitness(score, clearedLines, holes, maxHeight, bumpiness);
        adjustMutationRate();
        List<NeuralNetwork> population = new ArrayList<>();
        population.add(this);
        for (int i = 1; i < POPULATION_SIZE; i++) {
            NeuralNetwork mutatedNetwork = this.clone();
            mutatedNetwork.mutate(currentMutationRate, mutationStrength);
            population.add(mutatedNetwork);
        }
        List<NeuralNetwork> evolvedPopulation = evolvePopulation(population);
        NeuralNetwork newBestNetwork = evolvedPopulation.get(0);
        if (newBestNetwork.getFitness() > bestScore) {
            bestScore = newBestNetwork.getFitness();
            bestNetwork = newBestNetwork;
            this.weightsInputHidden = newBestNetwork.weightsInputHidden;
            this.weightsHiddenOutput = newBestNetwork.weightsHiddenOutput;
            this.biasHidden = newBestNetwork.biasHidden;
            this.biasOutput = newBestNetwork.biasOutput;
            System.out.println("Új rekord fitness: " + bestScore +
                    " (Pontszám: " + newBestNetwork.getScore() +
                    ", Törölt sorok: " + newBestNetwork.getClearedLines() +
                    ", Lukak: " + newBestNetwork.getHoles() +
                    ", Max magasság: " + newBestNetwork.getMaxHeight() +
                    ", Egyenetlenség: " + newBestNetwork.getBumpiness() +
                    ", Mutációs ráta: " + currentMutationRate + ")");
            try {
                saveBestToFile("best_network.dat");
            } catch (IOException e) {
                System.err.println("Hiba a hálózat mentése során: " + e.getMessage());
            }
        } else {
            System.out.println("Nem sikerült javítani a legjobb fitness-t. Jelenlegi legjobb: " + bestScore +
                    ", Mutációs ráta: " + currentMutationRate);
        }
    }

    private List<NeuralNetwork> evolvePopulation(List<NeuralNetwork> population) {
        List<NeuralNetwork> newPopulation = new ArrayList<>();
        int eliteSize = POPULATION_SIZE / 10;
        population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
        newPopulation.addAll(population.subList(0, eliteSize));
        while (newPopulation.size() < POPULATION_SIZE) {
            NeuralNetwork parent1 = tournamentSelection(population);
            NeuralNetwork parent2 = tournamentSelection(population);
            if (Math.random() < CROSSOVER_RATE) {
                NeuralNetwork child = parent1.crossover(parent2);
                if (Math.random() < MUTATION_RATE) {
                    child.mutate(0.1, 0.2);
                }
                newPopulation.add(child);
            } else {
                newPopulation.add(Math.random() < 0.5 ? parent1.clone() : parent2.clone());
            }
        }
        return newPopulation;
    }

    private NeuralNetwork tournamentSelection(List<NeuralNetwork> population) {
        int tournamentSize = 5;
        NeuralNetwork best = null;
        for (int i = 0; i < tournamentSize; i++) {
            NeuralNetwork contestant = population.get(random.nextInt(population.size()));
            if (best == null || contestant.getFitness() > best.getFitness()) {
                best = contestant;
            }
        }
        return best;
    }

    /**
     * Clones the current neural network.
     *
     * @return A deep copy of the current neural network
     */
    @Override
    public NeuralNetwork clone() {
        NeuralNetwork clone = new NeuralNetwork(inputNodes, hiddenNodes, outputNodes, false);
        clone.weightsInputHidden = deepCopyMatrix(this.weightsInputHidden);
        clone.weightsHiddenOutput = deepCopyMatrix(this.weightsHiddenOutput);
        clone.biasHidden = this.biasHidden.clone();
        clone.biasOutput = this.biasOutput.clone();
        clone.bestScore = this.bestScore;
        clone.fitness = this.fitness;
        clone.score = this.score;
        clone.clearedLines = this.clearedLines;
        clone.holes = this.holes;
        clone.bestNetwork = this;
        clone.maxHeight = this.maxHeight;
        clone.bumpiness = this.bumpiness;
        clone.currentMutationRate = this.currentMutationRate;
        clone.previousBestFitness = this.previousBestFitness;
        clone.generationsWithoutImprovement = this.generationsWithoutImprovement;
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

    public int getMaxHeight() {
        return maxHeight;
    }

    public double getBumpiness() {
        return bumpiness;
    }

    public double getCurrentMutationRate() {
        return currentMutationRate;
    }

    /**
     * Setters.
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

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
}
