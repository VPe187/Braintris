package hu.nye.vpe.nn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hu.nye.vpe.GlobalConfig;

/**
 * Neural network class.
 */
public class NeuralNetwork {
    private static final double CLIP_MIN = GlobalConfig.getInstance().getClipMin();
    private static final double CLIP_MAX = GlobalConfig.getInstance().getClipMax();
    private static final double CLIP_NORM = GlobalConfig.getInstance().getClipNorm();
    private static final double GRADIENT_SCALE = GlobalConfig.getInstance().getGradientScale();
    private static final double INITIAL_LEARNING_RATE = GlobalConfig.getInstance().getInitialLearningRate();
    private static final double INITIAL_Q_LEARNING_RATE = GlobalConfig.getInstance().getInitialQLearningRate();
    private static final double Q_LEARNING_RATE_DECAY = GlobalConfig.getInstance().getQLearningRateDecay();
    private static final double MIN_Q_LEARNING_RATE = GlobalConfig.getInstance().getMinQLearningRate();
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
    private static final int ROTATION_OUTPUTS = 4;
    private static final int MINIMUM_BATCH_SIZE = GlobalConfig.getInstance().getMinimumBatchSize();
    private static final int FEED_DATA_SIZE = GlobalConfig.getInstance().getFeedDataSize();

    private final List<Layer> layers;
    private double learningRate;
    private double qlearningRate;
    private double discountFactor;
    private double epsilon;
    private int episodeCount;
    private final Random random;
    private double maxQ;
    private double nextQ = 0;
    private double lastReward;
    private double bestReward;
    private double[][] lastActivations;
    private final List<Double> recentRewards;
    private double movingAverage;
    private double maxMovingAverage;
    private final ExperienceReplay experienceReplay;
    private final List<double[]> inputBatch;
    private final List<double[]> targetBatch;
    private double rms;
    private double maxRms;
    private final double[] layerMins;
    private final double[] layerMaxs;
    private final double[] layerMeans;
    private double averageWeightChange;
    private double[][][] weightChanges;
    private double[][][] previousWeights;
    int learnCounter = 0;

    private final double[] historicalLayerMins;
    private final double[] historicalLayerMaxs;
    private final double[] historicalLayerSums;
    private final long[] layerActivationCounts;

    private final NetworkPersistence persistence;

    public NeuralNetwork(String[] names, int[] layerSizes, Activation[] activations, WeightInitStrategy[] initStrategies,
                         BatchNormParameters[] batchNormParameters, double[] l2) {

        this.persistence = new NetworkPersistence(
                CLIP_MIN, CLIP_MAX, CLIP_NORM, GRADIENT_SCALE,
                INITIAL_LEARNING_RATE, INITIAL_DISCOUNT_FACTOR, INITIAL_EPSILON
        );

        if (layerSizes.length != activations.length + 1 ||
                layerSizes.length != initStrategies.length + 1 ||
                layerSizes.length != batchNormParameters.length + 1 ||
                layerSizes.length != l2.length) {
            throw new IllegalArgumentException("Invalid configuration: layerSizes length should be one more than " +
                    "the length of activations, initStrategies, and useBatchNorm arrays");
        }

        GradientClipper gradientClipper = new GradientClipper(CLIP_MIN, CLIP_MAX, CLIP_NORM, GRADIENT_SCALE);
        this.layers = new ArrayList<>();
        this.learningRate = INITIAL_LEARNING_RATE;
        this.qlearningRate = INITIAL_Q_LEARNING_RATE;

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
        this.maxMovingAverage = Double.NEGATIVE_INFINITY;
        this.maxQ = 0;
        this.maxRms = 0;

        this.historicalLayerMins = new double[layerSizes.length];
        this.historicalLayerMaxs = new double[layerSizes.length];
        this.historicalLayerSums = new double[layerSizes.length];
        this.layerActivationCounts = new long[layerSizes.length];

        for (int i = 0; i < layerSizes.length; i++) {
            historicalLayerMins[i] = Double.POSITIVE_INFINITY;
            historicalLayerMaxs[i] = Double.NEGATIVE_INFINITY;
            historicalLayerSums[i] = 0.0;
            layerActivationCounts[i] = 0;
        }
    }

    public void saveNetworkStructure(String filename) throws IOException {
        persistence.saveNetworkStructure(this, filename);
    }

    public void saveTrainingState(String filename) throws IOException {
        persistence.saveTrainingState(this, filename);
    }

    public void loadNetworkStructure(String filename) throws IOException {
        persistence.loadNetworkStructure(this, filename);
    }

    /**
     * Load training state from JSON file.
     *
     * @param filename file name to load from
     * @throws IOException if file cannot be read
     */
    public void loadTrainingState(String filename) throws IOException {
        persistence.loadTrainingState(this, filename);
    }

    /**
     * Forward pass.
     *
     * @param inputs input values
     *
     * @return forwarded input values
     */

    public double[] forward(double[] inputs, boolean isTraining) {
        double[] currentInput = inputs;
        this.lastActivations = new double[layers.size() + 1][];
        this.lastActivations[0] = inputs;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            currentInput = layer.forward(currentInput, isTraining);
            this.lastActivations[i + 1] = currentInput;
        }

        return currentInput;
    }

    private void backwardPass(double[][] batchInputs, double[][] batchTargets) {
        // Forward pass és minden réteg kimenetének tárolása
        List<double[][]> allLayerOutputs = new ArrayList<>();
        double[][] currentInputs = batchInputs;
        allLayerOutputs.add(currentInputs); // Input réteg kimenetek

        for (Layer layer : layers) {
            currentInputs = layer.forwardBatch(currentInputs, true);
            allLayerOutputs.add(currentInputs);
        }

        // Kezdeti értékek számítása az utolsó rétegre
        int batchSize = batchInputs.length;
        double[][] deltas = new double[batchSize][layers.get(layers.size() - 1).getSize()];
        double[][] finalLayerOutput = allLayerOutputs.get(allLayerOutputs.size() - 1);

        /*
        // Gradiens számítás
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < deltas[i].length; j++) {
                deltas[i][j] = batchTargets[i][j] - currentInputs[i][j];
            }
        }
         */

        // MSE loss függvény gradiens számítása
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < deltas[i].length; j++) {
                // dE/dy = 2(y - t) ahol E = MSE, y = predikció, t = target
                deltas[i][j] = 2.0 * (batchTargets[i][j] - finalLayerOutput[i][j]);
                if (Double.isNaN(deltas[i][j]) || Double.isInfinite(deltas[i][j])) {
                    deltas[i][j] = 0.0;
                }
            }
        }

        // Backpropagation rétegenként
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer currentLayer = layers.get(i);
            LayerGradients gradients = currentLayer.backwardBatch(deltas, allLayerOutputs.get(i));
            deltas = gradients.inputGradients;
        }
    }

    /**
     * Select action from network.
     *
     * @param possibleActions metric datas
     *
     * @return action
     */
    public int[] selectAction(double[][] possibleActions) {
        if (random.nextDouble() < epsilon) {
            // Random választás a lehetséges akciókból
            int randomIndex = random.nextInt(possibleActions.length);
            return new int[]{
                    (int) possibleActions[randomIndex][0],  // X koordináta
                    (int) possibleActions[randomIndex][1]   // Forgatás
            };
        } else {
            double[] qvalues = new double[possibleActions.length];
            double maxQ = Double.NEGATIVE_INFINITY;

            // Q-értékek számítása a metrikából
            for (int i = 0; i < possibleActions.length; i++) {
                double[] metrics = copyToFeedDataSize(possibleActions[i]);
                //qvalues[i] = forward(metrics, false)[0];
                qvalues[i] = Math.min(MAX_Q, Math.max(MIN_Q, forward(metrics, false)[0]));
                maxQ = Math.max(maxQ, qvalues[i]);
            }

            // Összes legjobb akció összegyűjtése
            List<Integer> bestActions = new ArrayList<>();
            final double EPSILON_Q = 1e-6;

            for (int i = 0; i < qvalues.length; i++) {
                if (Math.abs(qvalues[i] - maxQ) < EPSILON_Q) {
                    bestActions.add(i);
                }
            }

            // Véletlenszerű választás a legjobb akciók közül
            int bestActionIndex = bestActions.get(random.nextInt(bestActions.size()));
            return new int[]{
                    (int) possibleActions[bestActionIndex][0],  // X koordináta
                    (int) possibleActions[bestActionIndex][1]   // Forgatás
            };
        }
    }

    /**
     * Learn method.
     *
     * @param state     current metric data
     * @param action    last action
     * @param reward    reward
     * @param nextState next state datas
     * @param gameOver  game is over?
     */
    public void learn(double[] state, int[] action, double reward, double[] nextState, boolean gameOver, double[][] nextPossibleStates) {
        if (USE_EXPERIENCE) {
            if (learnCounter % 1000 == 0) {
                experienceReplay.normalizePriorities(0.1, 1.0);
            }
            learnWithExperinece(state, action, reward, nextState, gameOver, nextPossibleStates);
        } else {
            learnWithoutExperience(state, reward, gameOver, nextPossibleStates);
        }
        if (gameOver) {
            updateEpsilon();
            updateDiscountFactor();
            updateLearningRate();
            updateQLearningRate();
            this.episodeCount++;
            if (reward > this.bestReward) {
                this.bestReward = reward;
            }
        }
        updateMovingAverage(reward);
        lastReward = reward;
    }

    private void learnWithExperinece(double[] state, int[] action, double reward, double[] nextState, boolean gameOver,
                                     double[][] nextPossibleStates) {
        Experience experience = new Experience(state, action, reward, nextState, nextPossibleStates, gameOver);
        experienceReplay.add(experience);
        if (experienceReplay.size() >= EXPERIENCE_BATCH_SIZE) {
            List<Experience> batch = experienceReplay.sample(EXPERIENCE_BATCH_SIZE);
            processBatchWithExperience(batch);
        }
    }

    private void learnWithoutExperience(double[] state, double reward, boolean gameOver, double[][] nextPossibleStates) {
        double maxNextQ = calculateMaxNextQ(nextPossibleStates, gameOver);
        double currentQ = forward(copyToFeedDataSize(state), false)[0];
        currentQ = Math.min(MAX_Q, Math.max(MIN_Q, currentQ));

        double targetQ = calculateTargetQ(reward, maxNextQ, currentQ, gameOver);
        maxQ = Math.max(targetQ, maxQ);
        nextQ = maxNextQ;
        double[] stateMetrics = copyToFeedDataSize(state);
        inputBatch.add(stateMetrics);
        targetBatch.add(new double[]{targetQ});

        if (inputBatch.size() >= MINIMUM_BATCH_SIZE) {
            processBatchWithoutExperience();
        } else if (gameOver) {
            if (!inputBatch.isEmpty()) {
                processBatchWithoutExperience();
            }
            inputBatch.clear();
            targetBatch.clear();
        }
    }

    private void processBatchWithExperience(List<Experience> batch) {
        inputBatch.clear();
        targetBatch.clear();
        for (Experience exp : batch) {
            if (exp == null || exp.state == null || exp.action == null ||
                    exp.action[0] < 0 || exp.action[0] >= X_COORD_OUTPUTS ||
                    exp.action[1] < 0 || exp.action[1] >= ROTATION_OUTPUTS) {
                continue;
            }
            double[] stateMetrics = copyToFeedDataSize(exp.state);
            double currentQ = forward(stateMetrics, false)[0];
            currentQ = Math.min(MAX_Q, Math.max(MIN_Q, currentQ));
            double maxNextQ = calculateMaxNextQ(exp.nextPossibleStates, exp.done);
            double targetQ = calculateTargetQ(exp.reward, maxNextQ, currentQ, exp.done);
            updateExperiencePriority(targetQ, currentQ, exp);
            nextQ = maxNextQ - currentQ;
            inputBatch.add(stateMetrics);
            targetBatch.add(new double[]{targetQ});
        }
        if (inputBatch.size() >= MINIMUM_BATCH_SIZE) {
            processBatchWithoutExperience();
        }
    }

    /**
     * Calculate the target Q-value.
     *
     * @param reward Reward received
     * @param maxNextQ Maximum Q-value for the next states
     * @param currentQ Current Q-value for the state-action pair
     * @param done Flag indicating if the episode is done
     * @return targetQ Calculated target Q-value
     */
    private double calculateTargetQ(double reward, double maxNextQ, double currentQ, boolean done) {
        double targetQ;
        if (done) {
            targetQ = reward;
        } else {
            targetQ = currentQ + qlearningRate * (reward + discountFactor * maxNextQ - currentQ);
        }
        if (Double.isNaN(targetQ) || Double.isInfinite(targetQ)) {
            targetQ = 0.0;
        }
        return Math.min(MAX_Q, Math.max(MIN_Q, targetQ));
    }

    /**
     * Calculate the TD-error and update priority for an experience.
     *
     * @param targetQ Target Q-value
     * @param currentQ Current Q-value
     * @param experience Experience instance
     */
    private void updateExperiencePriority(double targetQ, double currentQ, Experience experience) {
        double tdError = Math.abs(targetQ - currentQ);
        experience.setPriority(tdError);
    }

    /**
     * Calculate the maximum Q-value for the next possible states.
     *
     * @param possibleStates Array of possible next states
     * @param isGameOver Flag indicating if the game is over
     * @return maxNextQ The maximum Q-value for the next states
     */
    private double calculateMaxNextQ(double[][] possibleStates, boolean isGameOver) {
        double maxNextQ = Double.NEGATIVE_INFINITY;
        if (!isGameOver && possibleStates != null) {
            for (double[] possibleState : possibleStates) {
                if (possibleState != null) {
                    double[] metrics = copyToFeedDataSize(possibleState);
                    double stateQ = Math.min(MAX_Q, Math.max(MIN_Q, forward(metrics, false)[0]));
                    maxNextQ = Math.max(maxNextQ, stateQ);
                }
            }
        }
        if (Double.isNaN(maxNextQ) || Double.isInfinite(maxNextQ)) {
            maxNextQ = 0.0;
        }
        return maxNextQ;
    }

    private void processBatchWithoutExperience() {
        double[][] inputs = inputBatch.toArray(new double[0][]);
        double[][] targets = targetBatch.toArray(new double[0][]);
        zeroGradients();
        backwardPass(inputs, targets);
        inputBatch.clear();
        targetBatch.clear();
    }

    private void updateMovingAverage(double reward) {
        if (Double.isNaN(reward)) {
            return;
        }

        recentRewards.add(reward);
        if (recentRewards.size() > MOVING_AVERAGE_WINDOW) {
            recentRewards.remove(0);
        }

        double sum = 0;
        for (Double r : recentRewards) {
            sum += r;
        }
        movingAverage = sum / recentRewards.size();

        if (!Double.isNaN(movingAverage) && movingAverage > maxMovingAverage) {
            maxMovingAverage = movingAverage;
        }
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

    private void updateQLearningRate() {
        qlearningRate = Math.max(MIN_Q_LEARNING_RATE, qlearningRate * Q_LEARNING_RATE_DECAY);
    }

    private void updateDiscountFactor() {
        discountFactor = Math.min(MAX_DISCOUNT_FACTOR, discountFactor + DISCOUNT_FACTOR_INCREMENT);
    }

    /**
     * Update network statistic.
     */
    public void updateStatistics() {
        double[][][] currentWeights = getAllWeights();

        if (this.previousWeights != null) {
            this.averageWeightChange = calculateAverageWeightChange(this.previousWeights, currentWeights);
            this.weightChanges = calculateWeightChanges(this.previousWeights, currentWeights);
        }
        this.previousWeights = deepCopy(currentWeights);

        calculateLayerStatistics();
        rms = calculateRMS(lastActivations[lastActivations.length - 1]);
        if (rms > maxRms) {
            maxRms = rms;
        }
    }

    private void calculateLayerStatisticsActive() {
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

    private void calculateLayerStatistics() {
        for (int i = 0; i < lastActivations.length; i++) {
            // Az aktuális aktivációk statisztikái
            double currentMin = Double.POSITIVE_INFINITY;
            double currentMax = Double.NEGATIVE_INFINITY;
            double sum = 0;

            for (double activation : lastActivations[i]) {
                currentMin = Math.min(currentMin, activation);
                currentMax = Math.max(currentMax, activation);
                sum += activation;
            }

            // Történeti értékek frissítése
            historicalLayerMins[i] = Math.min(historicalLayerMins[i], currentMin);
            historicalLayerMaxs[i] = Math.max(historicalLayerMaxs[i], currentMax);
            historicalLayerSums[i] += sum;
            layerActivationCounts[i] += lastActivations[i].length;

            // A megjelenítendő értékek frissítése
            layerMins[i] = historicalLayerMins[i];
            layerMaxs[i] = historicalLayerMaxs[i];
            layerMeans[i] = historicalLayerSums[i] / layerActivationCounts[i];
        }
    }

    /**
     * Reset layer statistic.
     */
    public void resetLayerStatistics() {
        for (int i = 0; i < historicalLayerMins.length; i++) {
            historicalLayerMins[i] = Double.POSITIVE_INFINITY;
            historicalLayerMaxs[i] = Double.NEGATIVE_INFINITY;
            historicalLayerSums[i] = 0.0;
            layerActivationCounts[i] = 0;
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

    private void zeroGradients() {
        for (Layer layer : layers) {
            for (Neuron neuron : layer.getNeurons()) {
                neuron.zeroGradients();
            }
        }
    }

    private static double[] copyToFeedDataSize(double[] source) {
        double[] target = new double[FEED_DATA_SIZE];
        System.arraycopy(source, 2, target, 0, FEED_DATA_SIZE);
        return target;
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

    public List<Layer> getLayers() {
        return layers;
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

    public double getMaxQ() {
        return maxQ;
    }

    public double getLastReward() {
        return lastReward;
    }

    public double getRMS() {
        return rms;
    }

    public double getMaxRMS() {
        return maxRms;
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

    public double getNextQ() {
        return nextQ;
    }

    /**
     * Visszaadja a legnagyobb átlagos rewardot a recentRewards listából.
     *
     * @return A legnagyobb átlagos reward
     */
    public double getMaxMovingAverage() {
        return maxMovingAverage;
    }

    /**
     * Visszaadja az utolsó mozgóátlagot a recentRewards listából.
     *
     * @return Az utolsó mozgóátlag
     */
    public double getLastMovingAverage() {
        if (recentRewards.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return recentRewards.get(recentRewards.size() - 1);
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public List<Double> getRecentRewards() {
        return new ArrayList<>(recentRewards);
    }

    public double[] getHistoricalLayerMins() {
        return historicalLayerMins.clone();
    }

    public double[] getHistoricalLayerMaxs() {
        return historicalLayerMaxs.clone();
    }

    public double[] getHistoricalLayerSums() {
        return historicalLayerSums.clone();
    }

    public long[] getLayerActivationCounts() {
        return layerActivationCounts.clone();
    }

    public ExperienceReplay getExperienceReplay() {
        return experienceReplay;
    }

    public double getQlearningRate() {
        return qlearningRate;
    }

    public void setDiscountFactor(double discountFactor) {
        this.discountFactor = discountFactor;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public void setBestReward(double bestReward) {
        this.bestReward = bestReward;
    }

    public void setLastReward(double lastReward) {
        this.lastReward = lastReward;
    }

    public void setMovingAverage(double movingAverage) {
        this.movingAverage = movingAverage;
    }

    public void setMaxMovingAverage(double maxMovingAverage) {
        this.maxMovingAverage = maxMovingAverage;
    }

    public void setRecentRewards(List<Double> recentRewards) {
        this.recentRewards.clear();
        this.recentRewards.addAll(recentRewards);
    }

    public void setRMS(double rms) {
        this.rms = rms;
    }

    public void setMaxRMS(double maxRms) {
        this.maxRms = maxRms;
    }

    public void setMaxQ(double maxQ) {
        this.maxQ = maxQ;
    }

    public void setNextQ(double nextQ) {
        this.nextQ = nextQ;
    }

    public void setLayerActivationCounts(long[] counts) {
        System.arraycopy(counts, 0, this.layerActivationCounts, 0, Math.min(counts.length, this.layerActivationCounts.length));
    }
}