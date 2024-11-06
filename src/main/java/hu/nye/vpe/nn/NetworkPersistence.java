package hu.nye.vpe.nn;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Neural network persistence handler class.
 */
public class NetworkPersistence {
    private final Gson gson;
    private final double clipMin;
    private final double clipMax;
    private final double clipNorm;
    private final double gradientScale;
    private final double initalLearningRate;
    private final double initialDiscountFactor;
    private final double initalEpsilon;

    public NetworkPersistence(double clipMin, double clipMax, double clipNorm, double gradientScale,
                              double initialLearningRate, double initialDiscountFactor, double initialEpsilon) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .serializeSpecialFloatingPointValues()
                .create();
        this.clipMin = clipMin;
        this.clipMax = clipMax;
        this.clipNorm = clipNorm;
        this.gradientScale = gradientScale;
        this.initalLearningRate = initialLearningRate;
        this.initialDiscountFactor = initialDiscountFactor;
        this.initalEpsilon = initialEpsilon;
    }

    /**
     * Save network.
     *
     * @param network Network
     *
     * @param filename File name
     *
     * @throws IOException IOException
     */
    public void saveNetworkStructure(NeuralNetwork network, String filename) throws IOException {
        List<Layer> layers = network.getLayers();
        List<Map<String, Object>> structureData = new ArrayList<>(layers.size());

        for (Layer layer : layers) {
            Map<String, Object> layerData = new HashMap<>();
            List<Neuron> neurons = layer.getNeurons();
            List<Map<String, Object>> neuronDataList = new ArrayList<>(neurons.size());

            layerData.put("name", layer.getName());
            layerData.put("activation", layer.getActivation().toString());

            for (Neuron neuron : neurons) {
                Map<String, Object> neuronData = new HashMap<>(2);
                neuronData.put("weights", neuron.getWeights());
                neuronData.put("bias", neuron.getBias());
                neuronDataList.add(neuronData);
            }

            layerData.put("neurons", neuronDataList);
            structureData.add(layerData);
        }

        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(structureData, writer);
        }
    }

    /**
     * Save training state.
     *
     * @param network Network
     *
     * @param filename File name
     *
     * @throws IOException IOException
     */
    public void saveTrainingState(NeuralNetwork network, String filename) throws IOException {
        Map<String, Object> stateData = new HashMap<>();
        packTrainingState(network, stateData);

        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(stateData, writer);
        }
    }

    /**
     * Load network structure.
     *
     * @param network Network.
     *
     * @param filename File name.
     *
     * @throws IOException IOException
     */
    public void loadNetworkStructure(NeuralNetwork network, String filename) throws IOException {
        try (FileReader reader = new FileReader(filename)) {
            List<Map<String, Object>> structureData = gson.fromJson(reader, List.class);
            loadStructureData(network, structureData);
        }
    }

    /**
     * Load training states.
     *
     * @param network Network
     *
     * @param filename File name
     *
     * @throws IOException IOException
     */
    public void loadTrainingState(NeuralNetwork network, String filename) throws IOException {
        try (FileReader reader = new FileReader(filename)) {
            Map<String, Object> stateData = gson.fromJson(reader, Map.class);
            unpackTrainingState(network, stateData);
        }
    }

    private void packTrainingState(NeuralNetwork network, Map<String, Object> stateData) {
        // Alap tanulási paraméterek
        stateData.put("learningRate", network.getLearningRate());
        stateData.put("discountFactor", network.getDiscountFactor());
        stateData.put("epsilon", network.getEpsilon());
        stateData.put("episodeCount", network.getEpisodeCount());

        // L2 regularizáció
        double[] l2Values = new double[network.getLayers().size()];
        for (int i = 0; i < network.getLayers().size(); i++) {
            Neuron neuron = network.getLayers().get(i).getNeurons().get(0); // Minden neuronnak ugyanaz az L2 értéke egy rétegben
            l2Values[i] = neuron.getL2();
        }
        stateData.put("l2Regularization", l2Values);

        // Jutalmak és átlagok
        stateData.put("bestReward", network.getBestReward());
        stateData.put("lastReward", network.getLastReward());
        stateData.put("movingAverage", network.getMovingAverage());
        stateData.put("maxMovingAverage", network.getMaxMovingAverage());
        stateData.put("recentRewards", network.getRecentRewards());

        // RMS értékek
        stateData.put("rms", network.getRMS());
        stateData.put("maxRms", network.getMaxRMS());

        // Q értékek
        stateData.put("maxQ", network.getMaxQ());
        stateData.put("nextQ", network.getNextQ());

        // Layer statisztikák
        stateData.put("layerMins", network.getLayerMins());
        stateData.put("layerMaxs", network.getLayerMaxs());
        stateData.put("layerMeans", network.getLayerMeans());
        stateData.put("historicalLayerMins", network.getHistoricalLayerMins());
        stateData.put("historicalLayerMaxs", network.getHistoricalLayerMaxs());
        stateData.put("historicalLayerSums", network.getHistoricalLayerSums());
        stateData.put("layerActivationCounts", network.getLayerActivationCounts());

        // Experience Replay adatok
        if (network.getExperienceReplay() != null) {
            stateData.put("experiences", network.getExperienceReplay().getExperiences());
        }

        List<Map<String, Object>> layerOptimizerStates = new ArrayList<>();
        for (Layer layer : network.getLayers()) {
            AdamOptimizer optimizer = layer.getOptimizer();
            Map<String, Object> optimizerState = new HashMap<>();

            optimizerState.put("mmean", optimizer.getMmean());
            optimizerState.put("vmean", optimizer.getVmean());
            optimizerState.put("mbias", optimizer.getMbias());
            optimizerState.put("vbias", optimizer.getVbias());
            optimizerState.put("iter", optimizer.getIter());
            optimizerState.put("beta1", optimizer.getBeta1());
            optimizerState.put("beta2", optimizer.getBeta2());
            optimizerState.put("epsilon", optimizer.getEpsilon());
            optimizerState.put("learningRate", optimizer.getLearningRate());

            layerOptimizerStates.add(optimizerState);
        }
    }

    private void unpackTrainingState(NeuralNetwork network, Map<String, Object> stateData) {
        // Alap tanulási paraméterek
        network.setLearningRate(getDoubleOrDefault(stateData, "learningRate", initalLearningRate));
        network.setDiscountFactor(getDoubleOrDefault(stateData, "discountFactor", initialDiscountFactor));
        network.setEpsilon(getDoubleOrDefault(stateData, "epsilon", initalEpsilon));
        network.setEpisodeCount(getIntOrDefault(stateData, "episodeCount", 0));

        // L2 regularizáció betöltése
        List<Number> l2Values = (List<Number>) stateData.get("l2Regularization");
        if (l2Values != null) {
            for (int i = 0; i < network.getLayers().size() && i < l2Values.size(); i++) {
                double l2Value = l2Values.get(i).doubleValue();
                for (Neuron neuron : network.getLayers().get(i).getNeurons()) {
                    neuron.setL2(l2Value);
                }
            }
        }

        // Jutalmak és átlagok
        network.setBestReward(getDoubleOrDefault(stateData, "bestReward", Double.NEGATIVE_INFINITY));
        network.setLastReward(getDoubleOrDefault(stateData, "lastReward", 0.0));
        network.setMovingAverage(getDoubleOrDefault(stateData, "movingAverage", Double.NEGATIVE_INFINITY));
        network.setMaxMovingAverage(getDoubleOrDefault(stateData, "maxMovingAverage", Double.NEGATIVE_INFINITY));

        // RecentRewards lista betöltése
        List<Double> loadedRewards = (List<Double>) stateData.get("recentRewards");
        if (loadedRewards != null) {
            network.setRecentRewards(new ArrayList<>(loadedRewards));
        }

        // RMS értékek
        network.setRMS(getDoubleOrDefault(stateData, "rms", 0.0));
        network.setMaxRMS(getDoubleOrDefault(stateData, "maxRms", 0.0));

        // Q értékek
        network.setMaxQ(getDoubleOrDefault(stateData, "maxQ", 0.0));
        network.setNextQ(getDoubleOrDefault(stateData, "nextQ", 0.0));

        // Layer statisztikák
        loadDoubleArrayInto(stateData, "layerMins", network.getLayerMins());
        loadDoubleArrayInto(stateData, "layerMaxs", network.getLayerMaxs());
        loadDoubleArrayInto(stateData, "layerMeans", network.getLayerMeans());
        loadDoubleArrayInto(stateData, "historicalLayerMins", network.getHistoricalLayerMins());
        loadDoubleArrayInto(stateData, "historicalLayerMaxs", network.getHistoricalLayerMaxs());
        loadDoubleArrayInto(stateData, "historicalLayerSums", network.getHistoricalLayerSums());

        // Layer activation counts
        List<Number> activationCounts = (List<Number>) stateData.get("layerActivationCounts");
        if (activationCounts != null) {
            long[] counts = network.getLayerActivationCounts();
            for (int i = 0; i < activationCounts.size() && i < counts.length; i++) {
                counts[i] = activationCounts.get(i).longValue();
            }
            network.setLayerActivationCounts(counts);
        }

        // Experience Replay adatok
        List<Map<String, Object>> experiences = (List<Map<String, Object>>) stateData.get("experiences");
        if (experiences != null && network.getExperienceReplay() != null) {
            ExperienceReplay experienceReplay = network.getExperienceReplay();
            experienceReplay.clear();
            for (Map<String, Object> expData : experiences) {
                Experience exp = deserializeExperience(expData);
                if (exp != null) {
                    experienceReplay.add(exp);
                }
            }
        }

        // Update learning rate for all layers
        for (Layer layer : network.getLayers()) {
            layer.setLearningRate(network.getLearningRate());
        }

        List<Map<String, Object>> layerOptimizerStates =
                (List<Map<String, Object>>) stateData.get("adamOptimizerStates");
        if (layerOptimizerStates != null) {
            List<Layer> layers = network.getLayers();
            for (int i = 0; i < layers.size() && i < layerOptimizerStates.size(); i++) {
                Map<String, Object> optimizerState = layerOptimizerStates.get(i);
                AdamOptimizer optimizer = layers.get(i).getOptimizer();

                // Momentum és velocity mátrixok betöltése
                List<List<Number>> mmeanData = (List<List<Number>>) optimizerState.get("mmean");
                List<List<Number>> vmeanData = (List<List<Number>>) optimizerState.get("vmean");
                List<Number> mbiasData = (List<Number>) optimizerState.get("mbias");
                List<Number> vbiasData = (List<Number>) optimizerState.get("vbias");

                // 2D tömb konvertálása mmean és vmean számára
                double[][] mmean = convert2DArrayFromList(mmeanData);
                double[][] vmean = convert2DArrayFromList(vmeanData);

                // 1D tömb konvertálása mbias és vbias számára
                double[] mbias = convert1DArrayFromList(mbiasData);
                double[] vbias = convert1DArrayFromList(vbiasData);

                // Az új setter metódusokkal beállítjuk az állapotokat
                optimizer.setMmean(mmean);
                optimizer.setVmean(vmean);
                optimizer.setMbias(mbias);
                optimizer.setVbias(vbias);
                optimizer.setIter(((Number) optimizerState.get("iter")).intValue());
            }
        }
    }

    private void loadStructureData(NeuralNetwork network, List<Map<String, Object>> structureData) {
        try {
            network.getLayers().clear();

            for (Map<String, Object> layerData : structureData) {
                String name = (String) layerData.get("name");
                Activation activation = Activation.valueOf((String) layerData.get("activation"));
                List<Map<String, Object>> neuronDataList = (List<Map<String, Object>>) layerData.get("neurons");
                Map<String, Object> firstNeuronData = neuronDataList.get(0);
                List<Double> firstWeights = (List<Double>) firstNeuronData.get("weights");
                int inputSize = firstWeights.size();
                int outputSize = neuronDataList.size();

                GradientClipper gradientClipper = new GradientClipper(
                        clipMin,
                        clipMax,
                        clipNorm,
                        gradientScale
                );

                WeightInitStrategy initStrategy = WeightInitStrategy.XAVIER;
                BatchNormParameters batchNormParams = new BatchNormParameters(false, 1.0, 0.0);
                double l2 = 0.0;

                Layer layer = new Layer(
                        name,
                        inputSize,
                        outputSize,
                        activation,
                        initStrategy,
                        gradientClipper,
                        l2,
                        batchNormParams,
                        network.getLearningRate()
                );

                List<Neuron> neurons = layer.getNeurons();
                for (int i = 0; i < neuronDataList.size(); i++) {
                    Map<String, Object> neuronData = neuronDataList.get(i);

                    double[] weights = convertToDoubleArray((List<Number>) neuronData.get("weights"));
                    double bias = ((Number) neuronData.get("bias")).doubleValue();

                    Neuron neuron = neurons.get(i);
                    neuron.setWeights(weights);
                    neuron.setBias(bias);
                }
                network.getLayers().add(layer);
            }
            validateNetworkStructure(network);

        } catch (Exception e) {
            throw new RuntimeException("Error loading network structure: " + e.getMessage(), e);
        }
    }

    private void loadDoubleArray(Map<String, Object> data, String key, double[] target) {
        List<Number> values = (List<Number>) data.get(key);
        if (values != null) {
            for (int i = 0; i < values.size() && i < target.length; i++) {
                target[i] = values.get(i).doubleValue();
            }
        }
    }

    private Experience deserializeExperience(Map<String, Object> expData) {
        try {
            List<Number> stateList = (List<Number>) expData.get("state");
            List<Number> actionList = (List<Number>) expData.get("action");
            List<Number> nextStateList = (List<Number>) expData.get("nextState");

            if (stateList == null || actionList == null) {
                return null;
            }

            double[] state = new double[stateList.size()];
            int[] action = new int[actionList.size()];
            double[] nextState = nextStateList != null ? new double[nextStateList.size()] : null;

            // Manuális konverzió az autoboxing elkerülésére
            for (int i = 0; i < state.length; i++) {
                state[i] = stateList.get(i).doubleValue();
            }
            for (int i = 0; i < action.length; i++) {
                action[i] = actionList.get(i).intValue();
            }
            if (nextState != null) {
                for (int i = 0; i < nextState.length; i++) {
                    nextState[i] = nextStateList.get(i).doubleValue();
                }
            }

            double reward = ((Number) expData.get("reward")).doubleValue();
            boolean done = (Boolean) expData.get("done");

            return new Experience(state, action, reward, nextState, null, done);
        } catch (Exception e) {
            System.out.println("Error deserializing experience: " + e.getMessage());
            return null;
        }
    }

    private double[] convertToDoubleArray(List<Number> list) {
        if (list == null) {
            return null;
        }
        int size = list.size();
        double[] result = new double[size];
        for (int i = 0; i < size; i++) { // size változó cache-elése
            result[i] = list.get(i).doubleValue();
        }
        return result;
    }

    private int[] convertToIntArray(List<Number> list) {
        if (list == null) {
            return null;
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).intValue();
        }
        return result;
    }

    private double getDoubleOrDefault(Map<String, Object> data, String key, double defaultValue) {
        try {
            Object value = data.get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getIntOrDefault(Map<String, Object> data, String key, int defaultValue) {
        try {
            Object value = data.get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long getLongOrDefault(Map<String, Object> data, String key, long defaultValue) {
        try {
            Object value = data.get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getBooleanOrDefault(Map<String, Object> data, String key, boolean defaultValue) {
        try {
            Object value = data.get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void loadDoubleArrayInto(Map<String, Object> data, String key, double[] target) {
        List<Number> values = (List<Number>) data.get(key);
        if (values != null) {
            for (int i = 0; i < values.size() && i < target.length; i++) {
                target[i] = values.get(i).doubleValue();
            }
        }
    }

    /**
     * Validates the loaded network structure.
     *
     * @param network The neural network to validate
     * @throws IllegalStateException if the network structure is invalid
     */
    private void validateNetworkStructure(NeuralNetwork network) {
        List<Layer> layers = network.getLayers();
        int size = layers.size();

        if (size == 0) {
            throw new IllegalStateException("Network has no layers after loading");
        }

        for (int i = 0; i < size - 1; i++) {
            Layer currentLayer = layers.get(i);
            Layer nextLayer = layers.get(i + 1);
            List<Neuron> currentNeurons = currentLayer.getNeurons();
            List<Neuron> nextNeurons = nextLayer.getNeurons();

            if (currentNeurons.isEmpty() || nextNeurons.isEmpty()) {
                throw new IllegalStateException("Layer " + i + " or " + (i + 1) + " has no neurons");
            }

            if (currentNeurons.size() != nextNeurons.get(0).getWeights().length) {
                throw new IllegalStateException(
                        String.format("Layer size mismatch between layers %d and %d: %d != %d",
                                i, i + 1, currentNeurons.size(), nextNeurons.get(0).getWeights().length)
                );
            }
        }
    }

    private double[] convert1DArrayFromList(List<Number> list) {
        if (list == null) {
            return null;
        }

        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).doubleValue();
        }
        return result;
    }

    private double[][] convert2DArrayFromList(List<List<Number>> list) {
        if (list == null) {
            return null;
        }

        double[][] result = new double[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            result[i] = new double[list.get(i).size()];
            for (int j = 0; j < list.get(i).size(); j++) {
                result[i][j] = list.get(i).get(j).doubleValue();
            }
        }
        return result;
    }

}
