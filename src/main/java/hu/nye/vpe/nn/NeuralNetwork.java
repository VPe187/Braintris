package hu.nye.vpe.nn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Neural network class.
 */
public class NeuralNetwork implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 2L;
    private static final double INITIAL_LEARNING_RATE = 0.1;
    private static final double INITIAL_MOMENTUM = 0.9;
    private static final int POPULATION_SIZE = 150;
    private static final double MUTATION_RATE = 0.3;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_STRENGTH = 0.1;
    private static final int GENERATION_WITHOUT_IMPROVED = 30;
    private static final int ELITE_SIZE_DIVIDER = 5;
    private static final int DEFAULT_POOL_SIZE = 200;

    private final int inputNodes;
    private final int hiddenNodes1;
    private final int hiddenNodes2;
    private final int outputNodes;
    private double[][] weightsInputHidden1;
    private double[][] weightsHidden1Hidden2;
    private double[][] weightsHidden2Output;
    private double[][] velocityInputHidden1;
    private double[][] velocityHidden1Hidden2;
    private double[][] velocityHidden2Output;
    private double[] velocityBiasHidden1;
    private double[] velocityBiasHidden2;
    private double[] velocityBiasOutput;
    private double[] biasHidden1;
    private double[] biasHidden2;
    private double[] biasOutput;
    private final Random random;
    private double fitness;
    private double bestFitness = Double.NEGATIVE_INFINITY;
    private NeuralNetwork bestNetwork = null;
    private int clearedLines;
    private int holes;
    private double score;
    private int maxHeight;
    private double bumpiness;
    private double playTime;
    private int generationsWithoutImprovement;
    private AdaptiveLearningRate adaptiveLearningRate;
    private static NeuralNetworkPool pool;
    private static boolean isPoolInitialized = false;
    private static boolean isNetworkInitialized = false;
    private double globalBestFitness = Double.NEGATIVE_INFINITY;
    private NeuralNetwork globalBestNetwork;

    public NeuralNetwork(final int inputNodes, final int hiddenNodes1, final int hiddenNodes2, final int outputNodes, boolean initialize) {
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
        this.velocityInputHidden1 = new double[inputNodes][hiddenNodes1];
        this.velocityHidden1Hidden2 = new double[hiddenNodes1][hiddenNodes2];
        this.velocityHidden2Output = new double[hiddenNodes2][outputNodes];
        this.velocityBiasHidden1 = new double[hiddenNodes1];
        this.velocityBiasHidden2 = new double[hiddenNodes2];
        this.velocityBiasOutput = new double[outputNodes];
        this.adaptiveLearningRate = new AdaptiveLearningRate(INITIAL_LEARNING_RATE, INITIAL_MOMENTUM);
        this.generationsWithoutImprovement = 0;
        if (initialize) {
            loadOrInitialize();
        }
        this.bestFitness = 0;
    }

    private void loadOrInitialize() {
        try {
            NeuralNetwork loadedNetwork = loadFromFile("bestbrain.dat");
            if (loadedNetwork != null &&
                    loadedNetwork.inputNodes == this.inputNodes &&
                    loadedNetwork.hiddenNodes1 == this.hiddenNodes1 &&
                    loadedNetwork.hiddenNodes2 == this.hiddenNodes2 &&
                    loadedNetwork.outputNodes == this.outputNodes) {
                copyFrom(loadedNetwork);
                this.bestFitness = loadedNetwork.bestFitness;
                this.globalBestFitness = loadedNetwork.bestFitness;
                this.bestNetwork = loadedNetwork.clone();
                this.globalBestNetwork = loadedNetwork.clone();
                System.out.println("Saved network loaded successfully. Best fitness: " + this.globalBestFitness);
            } else {
                initializeNewNetwork();
            }
        } catch (IOException | ClassNotFoundException e) {
            initializeNewNetwork();
            System.out.println("Error during loading, new network initialized.");
        }
    }

    private void initializeNewNetwork() {
        if (!isNetworkInitialized) {
            initializeNodes();
            this.bestNetwork = this.clone();
            this.globalBestNetwork = this.clone();
            this.bestFitness = Double.NEGATIVE_INFINITY;
            this.globalBestFitness = Double.NEGATIVE_INFINITY;
            isNetworkInitialized = true;
            System.out.println("New neural network initialized.");
        } else {
            initializeNodes();
            this.bestNetwork = this.clone();
        }


    }

    private void initializeNodes() {
        initializeWeights(weightsInputHidden1, inputNodes);
        initializeWeights(weightsHidden1Hidden2, hiddenNodes1);
        initializeWeights(weightsHidden2Output, hiddenNodes2);
        initializeBiases(biasHidden1);
        initializeBiases(biasHidden2);
        initializeBiases(biasOutput);
        initializeVelocities(velocityInputHidden1);
        initializeVelocities(velocityHidden1Hidden2);
        initializeVelocities(velocityHidden2Output);
        initializeVelocities(velocityBiasHidden1);
        initializeVelocities(velocityBiasHidden2);
        initializeVelocities(velocityBiasOutput);
    }

    private void initializeVelocities(double[][] velocities) {
        for (double[] layer : velocities) {
            Arrays.fill(layer, 0);
        }
    }

    private void initializeVelocities(double[] velocities) {
        Arrays.fill(velocities, 0);
    }

    private void initializeWeights(double[][] weights, int fanIn) {
        double scale = Math.sqrt(2.0 / fanIn);
        for (double[] layer : weights) {
            for (int j = 0; j < layer.length; j++) {
                layer[j] = random.nextGaussian() * scale;
            }
        }
    }

    private void initializeBiases(double[] biases) {
        for (int i = 0; i < biases.length; i++) {
            biases[i] = random.nextGaussian();
        }
    }

    private static void initializePool(int poolSize, int inputNodes, int hiddenNodes1, int hiddenNodes2, int outputNodes) {
        if (pool == null) {
            pool = new NeuralNetworkPool(poolSize, inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
            isPoolInitialized = true;
            System.out.println("Neural Network pool initialized with size: " + poolSize);
        } else if (!isPoolInitialized) {
            System.out.println("Neural Network pool already exists but was not properly initialized. Reinitializing...");
            pool = new NeuralNetworkPool(poolSize, inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
            isPoolInitialized = true;
        }
    }

    private static NeuralNetwork acquireFromPool(int inputNodes, int hiddenNodes1, int hiddenNodes2, int outputNodes) {
        if (pool == null || !isPoolInitialized) {
            initializePool(DEFAULT_POOL_SIZE, inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
        }
        NeuralNetwork network = pool.acquire();
        if (network.inputNodes != inputNodes ||
                network.hiddenNodes1 != hiddenNodes1 ||
                network.hiddenNodes2 != hiddenNodes2 ||
                network.outputNodes != outputNodes) {
            network = new NeuralNetwork(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes, true);
        }
        return network;
    }

    private static void releaseToPool(NeuralNetwork network) {
        if (pool != null) {
            pool.release(network);
        }
    }

    /**
     * Reset network.
     */
    public void reset() {
        initializeNodes();
        this.fitness = 0;
        this.bestFitness = Double.NEGATIVE_INFINITY;
        this.bestNetwork = null;
        this.clearedLines = 0;
        this.holes = 0;
        this.score = 0;
        this.maxHeight = 0;
        this.bumpiness = 0;
        this.playTime = 0;
        this.generationsWithoutImprovement = 0;
    }

    /**
     * Feed method.
     *
     * @param inputs inputArray
     *
     * @return outputs double
     */
    public double[] feedForward(double[] inputs) {
        if (inputs.length != inputNodes) {
            throw new IllegalArgumentException("Input size does not match network input nodes");
        }
        double[] hiddenLayer1 = calculateHiddenLayerOutputs(inputs, weightsInputHidden1, biasHidden1, hiddenNodes1);
        double[] hiddenLayer2 = calculateHiddenLayerOutputs(hiddenLayer1, weightsHidden1Hidden2, biasHidden2, hiddenNodes2);
        return calculateLayerOutputs(hiddenLayer2, weightsHidden2Output, biasOutput, outputNodes);
    }

    private double[] calculateHiddenLayerOutputs(double[] inputs, double[][] weights, double[] biases, int numNodes) {
        double[] outputs = new double[numNodes];
        for (int i = 0; i < numNodes; i++) {
            double sum = biases[i];
            for (int j = 0; j < inputs.length; j++) {
                sum += inputs[j] * weights[j][i];
            }
            outputs[i] = activateLeakyRELU(sum);
        }
        return outputs;
    }

    private double[] calculateLayerOutputs(double[] inputs, double[][] weights, double[] biases, int numNodes) {
        double[] outputs = new double[numNodes];
        for (int i = 0; i < numNodes; i++) {
            double sum = biases[i];
            for (int j = 0; j < inputs.length; j++) {
                sum += inputs[j] * weights[j][i];
            }
            outputs[i] = activateSigmoid(sum);
        }
        return outputs;
    }

    private double activateRELU(double x) {
        return Math.max(0, x);
    }

    private double activateLeakyRELU(double x) {
        return x > 0 ? x : 0.01 * x;
    }

    private double activateSigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private double lerp(double variableA, double variableB, double variablet) {
        return variableA + (variableB - variableA) * variablet;
    }

    private void calculateFitness(double score, int clearedLines, int holes, int maxHeight, double bumpiness, double playTime) {
        this.score = score;
        this.clearedLines = clearedLines;
        this.holes = holes;
        this.maxHeight = maxHeight;
        this.bumpiness = bumpiness;
        this.playTime = playTime / 1000;
        double a = -0.5;
        double b = 0.7;
        double c = -0.3;
        double d = -0.18;
        this.fitness = a * maxHeight + b * clearedLines + c * holes + d * bumpiness;
    }

    private void mutate(double mutationRate, double mutationStrength) {
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

    private void mutateWithES(double sigma) {
        mutateWeightsWithES(weightsInputHidden1, sigma);
        mutateWeightsWithES(weightsHidden1Hidden2, sigma);
        mutateWeightsWithES(weightsHidden2Output, sigma);
        mutateBiasesWithES(biasHidden1, velocityBiasHidden1, sigma);
        mutateBiasesWithES(biasHidden2, velocityBiasHidden2, sigma);
        mutateBiasesWithES(biasOutput, velocityBiasOutput, sigma);
    }

    private void mutateWeightsWithES(double[][] weights, double sigma) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] += random.nextGaussian() * sigma * MUTATION_STRENGTH;  // Gaussian mutation
            }
        }
    }

    private void mutateBiasesWithES(double[] biases, double[] velocities, double sigma) {
        double momentum = adaptiveLearningRate.getMomentum();
        for (int i = 0; i < biases.length; i++) {
            double gaussianNoise = random.nextGaussian() * sigma * MUTATION_STRENGTH;
            velocities[i] = momentum * velocities[i] + (1 - momentum) * gaussianNoise;
            biases[i] += velocities[i];
        }
    }

    private void crossover(NeuralNetwork parent1, NeuralNetwork parent2) {
        this.weightsInputHidden1 = crossoverWeights(parent1.weightsInputHidden1, parent2.weightsInputHidden1);
        this.weightsHidden1Hidden2 = crossoverWeights(parent1.weightsHidden1Hidden2, parent2.weightsHidden1Hidden2);
        this.weightsHidden2Output = crossoverWeights(parent1.weightsHidden2Output, parent2.weightsHidden2Output);
        this.biasHidden1 = crossoverBiases(parent1.biasHidden1, parent2.biasHidden1);
        this.biasHidden2 = crossoverBiases(parent1.biasHidden2, parent2.biasHidden2);
        this.biasOutput = crossoverBiases(parent1.biasOutput, parent2.biasOutput);
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
    public void evolve(double score, int clearedLines, int holes, int maxHeight, double bumpiness, double playTime) {
        calculateFitness(score, clearedLines, holes, maxHeight, bumpiness, playTime);
        double newLearningRate = adaptiveLearningRate.updateLearningRate(fitness, globalBestFitness);
        List<NeuralNetwork> population = new ArrayList<>();
        population.add(this);
        for (int i = 1; i < POPULATION_SIZE; i++) {
            NeuralNetwork mutatedNetwork = acquireFromPool(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
            mutatedNetwork.copyFrom(this);
            //mutatedNetwork.mutate(newLearningRate, MUTATION_STRENGTH);
            mutatedNetwork.mutateWithES(newLearningRate);
            population.add(mutatedNetwork);
        }
        List<NeuralNetwork> evolvedPopulation = evolvePopulation(population);
        NeuralNetwork newBestNetwork = evolvedPopulation.get(0);
        if (newBestNetwork.getFitness() > this.globalBestFitness) {
            this.globalBestFitness = newBestNetwork.getFitness();
            this.bestFitness = newBestNetwork.getFitness();
            this.score = newBestNetwork.getScore();
            this.copyFrom(newBestNetwork);
            this.bestNetwork = this.clone();
            this.globalBestNetwork = this.clone();
            System.out.println("New best network =  " + this.globalBestFitness +
                    " (Score: " + newBestNetwork.getScore() +
                    ", Deleted rows: " + newBestNetwork.getClearedLines() +
                    ", Holes: " + newBestNetwork.getHoles() +
                    ", Max height: " + newBestNetwork.getMaxHeight() +
                    ", Bumpiness: " + newBestNetwork.getBumpiness() +
                    ", Playtime: " + newBestNetwork.getPlayTime() +
                    ", Learning rate: " + newLearningRate + ")");
            try {
                saveBestToFile("bestbrain.dat");
            } catch (IOException e) {
                System.err.println("Error during saving network: " + e.getMessage());
            }
            generationsWithoutImprovement = 0;
        } else {
            //this.mutate(newLearningRate * 2, MUTATION_STRENGTH * 2);
            this.mutateWithES(newLearningRate);
            System.out.println("Failing to improve the best fitness (" + this.fitness + "). Current best fitness: " + this.globalBestFitness +
                    ", Learning rate: " + newLearningRate);
            generationsWithoutImprovement++;
        }
        for (NeuralNetwork network : population) {
            if (network != this) {
                releaseToPool(network);
            }
        }
        randomReinitialization();
    }

    private List<NeuralNetwork> evolvePopulation(List<NeuralNetwork> population) {
        int eliteSize = POPULATION_SIZE / ELITE_SIZE_DIVIDER;
        population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
        List<NeuralNetwork> newPopulation = new ArrayList<>(population.subList(0, eliteSize));
        List<NeuralNetwork> parents = stochasticUniversalSampling(population, POPULATION_SIZE - eliteSize);
        for (int i = 0; i < parents.size(); i += 2) {
            NeuralNetwork parent1 = parents.get(i);
            NeuralNetwork parent2 = (i + 1 < parents.size()) ? parents.get(i + 1) : parents.get(0);
            if (Math.random() < CROSSOVER_RATE) {
                NeuralNetwork child = acquireFromPool(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
                child.crossover(parent1, parent2);
                if (Math.random() < MUTATION_RATE) {
                    //child.mutate(adaptiveLearningRate.getLearningRate(), MUTATION_STRENGTH);
                    mutateWithES(adaptiveLearningRate.getLearningRate());
                }
                newPopulation.add(child);
            } else {
                NeuralNetwork child = acquireFromPool(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes);
                child.copyFrom(Math.random() < 0.5 ? parent1 : parent2);
                newPopulation.add(child);
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
        this.playTime = other.playTime;
        this.generationsWithoutImprovement = other.generationsWithoutImprovement;
    }

    private void randomReinitialization() {
        if (generationsWithoutImprovement > GENERATION_WITHOUT_IMPROVED) {
            initializeNodes();
            generationsWithoutImprovement = 0;
            System.out.println("Network randomly re-initialised.");
        }
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
            clone.velocityInputHidden1 = deepCopyMatrix(this.velocityInputHidden1);
            clone.velocityHidden1Hidden2 = deepCopyMatrix(this.velocityHidden1Hidden2);
            clone.velocityHidden2Output = deepCopyMatrix(this.velocityHidden2Output);
            clone.velocityBiasHidden1 = this.velocityBiasHidden1.clone();
            clone.velocityBiasHidden2 = this.velocityBiasHidden2.clone();
            clone.velocityBiasOutput = this.velocityBiasOutput.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen
        }
    }

    private double[][] deepCopyMatrix(double[][] matrix) {
        return java.util.Arrays.stream(matrix).map(double[]::clone).toArray(double[][]::new);
    }

    private void saveBestToFile(String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(globalBestNetwork);
        }
    }

    private static NeuralNetwork loadFromFile(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("The saved file does not exist, a new network will be initialized.");
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (NeuralNetwork) ois.readObject();
        }
    }

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

    public int getMaxHeight() {
        return maxHeight;
    }

    public double getBumpiness() {
        return bumpiness;
    }

    public double getPlayTime() {
        return playTime;
    }
}