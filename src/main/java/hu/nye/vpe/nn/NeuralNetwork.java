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
    private static final int X_COORD_OUTPUTS = 12;
    private static final int ROTATION_OUTPUTS = 4;
    private static final int MINIMUM_BATCH_SIZE = GlobalConfig.getInstance().getMinimumBatchSize();
    private static final int FEED_DATA_SIZE = GlobalConfig.getInstance().getFeedDataSize();

    private final List<Layer> layers;
    private double learningRate;
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

    private final double[] historicalLayerMins;
    private final double[] historicalLayerMaxs;
    private final double[] historicalLayerSums;
    private final long[] layerActivationCounts;

    public NeuralNetwork(String[] names, int[] layerSizes, Activation[] activations, WeightInitStrategy[] initStrategies,
                         BatchNormParameters[] batchNormParameters, double[] l2) {
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
                qvalues[i] = forward(metrics, false)[0];
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
            learnWithExperinece(state, action, reward, nextState, gameOver, nextPossibleStates);
        } else {
            learnWithoutExperience(state, reward, gameOver, nextPossibleStates);
        }
        if (gameOver) {
            updateEpsilon();
            updateDiscountFactor();
            updateLearningRate();
            this.episodeCount++;
        } else {
            if (reward > this.bestReward) {
                this.bestReward = reward;
            }
            updateMovingAverage(reward);
            lastReward = reward;
        }
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
        double maxNextQ = Double.NEGATIVE_INFINITY;
        if (!gameOver && nextPossibleStates != null) {
            for (double[] possibleState : nextPossibleStates) {
                double[] metrics = copyToFeedDataSize(possibleState);
                double stateQ = forward(metrics, false)[0];
                maxNextQ = (Math.max(maxNextQ, stateQ));
            }
        }
        if (Double.isNaN(maxNextQ) || Double.isInfinite(maxNextQ)) {
            maxNextQ = 0.0;
        }

        nextQ = maxNextQ;

        double[] stateMetrics = copyToFeedDataSize(state);
        double[] currentOutput = forward(stateMetrics, false);

        double targetQ;
        if (gameOver) {
            targetQ = reward;
        } else {
            //targetQ = reward + discountFactor * maxNextQ * learningRate;
            targetQ = currentOutput[0] + learningRate * (reward + discountFactor * maxNextQ - currentOutput[0]);
        }

        maxQ = Math.max(targetQ, maxQ);

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

    private double[] getCurrentNextState(double[][] possibleStates) {
        if (possibleStates == null || possibleStates.length == 0) {
            return null;
        }

        if (random.nextDouble() < epsilon) {
            // Random választás
            int randIndex = random.nextInt(possibleStates.length);
            return possibleStates[randIndex];
        } else {
            // Legjobb Q érték választása
            double bestQ = Double.NEGATIVE_INFINITY;
            double[] bestState = null;

            for (double[] possibleState : possibleStates) {
                double q = forward(possibleState, false)[0];
                if (q > bestQ) {
                    bestQ = q;
                    bestState = possibleState;
                }
            }
            return bestState;
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

            double maxNextQ = Double.NEGATIVE_INFINITY;
            if (!exp.done && exp.nextPossibleStates != null) {
                for (double[] possibleState : exp.nextPossibleStates) {
                    if (possibleState != null) {
                        double[] metrics = copyToFeedDataSize(possibleState);
                        double stateQ = forward(metrics, false)[0];
                        maxNextQ = Math.max(maxNextQ, stateQ);
                    }
                }
            }
            if (Double.isNaN(maxNextQ) || Double.isInfinite(maxNextQ)) {
                maxNextQ = 0.0;
            }

            double[] stateMetrics = copyToFeedDataSize(exp.state);
            double[] currentOutput = forward(stateMetrics, false);

            nextQ = maxNextQ;
            double targetQ;
            if (exp.done) {
                targetQ = exp.reward;
            } else {
                //targetQ = exp.reward + discountFactor * maxNextQ * learningRate;
                targetQ = currentOutput[0] + learningRate * (exp.reward + discountFactor * maxNextQ - currentOutput[0]);
            }

            maxQ = Math.max(targetQ, maxQ);


            inputBatch.add(stateMetrics);
            targetBatch.add(new double[]{targetQ});
        }

        if (inputBatch.size() >= MINIMUM_BATCH_SIZE) {
            processBatchWithoutExperience();
        }
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
        recentRewards.add(reward);
        if (recentRewards.size() > MOVING_AVERAGE_WINDOW) {
            recentRewards.remove(0);
        }

        double sum = 0;
        for (Double r : recentRewards) {
            sum += r;
        }
        movingAverage = sum / recentRewards.size();

        if (movingAverage > maxMovingAverage) {
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
}