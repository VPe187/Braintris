package hu.nye.vpe.nn;

// Layer.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer class.
 */
public class Layer implements Serializable {
    private final List<Neuron> neurons;
    private final Activation activation;
    private final WeightInitStrategy initStrategy;
    private BatchNormalizer batchNormalizer;
    private final GradientClipper gradientClipper;
    private double[] lastInputs;
    private String name;

    public Layer(String name, int inputSize, int neuronCount, Activation activation, WeightInitStrategy initStrategy,
                 boolean useBatchNorm, GradientClipper gradientClipper, double lambdaL2) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.gradientClipper = gradientClipper;
        if (useBatchNorm) {
            this.batchNormalizer = new BatchNormalizer(neuronCount, 1.0, 0.0);
        }
        for (int i = 0; i < neuronCount; i++) {
            neurons.add(new Neuron(inputSize, neuronCount, activation, initStrategy, gradientClipper, lambdaL2));
        }
    }

    /**
     * Forward pass.
     *
     * @param inputs input values
     *
     * @param isTraining is training?
     *
     * @return outputs
     */
    public double[] forward(double[] inputs, boolean isTraining) {
        this.lastInputs = inputs.clone();
        double[] outputs = new double[neurons.size()];

        double[] normalizedInputs = inputs.clone();
        if (batchNormalizer != null) {
            normalizedInputs = batchNormalizer.normalize(normalizedInputs, isTraining);
        }

        for (int i = 0; i < neurons.size(); i++) {
            outputs[i] = neurons.get(i).activate(normalizedInputs);
        }

        return outputs;
    }

    /**
     * Backward pass.
     *
     * @param nextLayerDeltas deltas
     *
     * @param learningRate learning rates
     *
     * @return new gradients
     */
    public LayerGradients backward(double[] nextLayerDeltas, double learningRate) {
        double[] deltas = nextLayerDeltas;
        double[] inputGradients = new double[lastInputs.length];

        double[] gammaGradients = null;
        double[] betaGradients = null;
        if (batchNormalizer != null) {
            deltas = batchNormalizer.backprop(deltas);
            gammaGradients = batchNormalizer.getGammaGradients();
            betaGradients = batchNormalizer.getBetaGradients();
        }

        // Számítsuk ki a gradiens visszaterjesztést és frissítsük a súlyokat
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(lastInputs, deltas[i], learningRate);
            double[] weights = neuron.getWeights();
            for (int j = 0; j < lastInputs.length; j++) {
                inputGradients[j] += deltas[i] * weights[j];
            }
        }

        // Skálázás és klippelés az input gradienteken
        inputGradients = gradientClipper.scaleAndClip(inputGradients);

        if (batchNormalizer != null) {
            batchNormalizer.updateParameters(learningRate);
        }

        return new LayerGradients(inputGradients, gammaGradients, betaGradients);
    }

    public int getSize() {
        return neurons.size();
    }

    public String getName() {
        return name;
    }

    public WeightInitStrategy getInitStrategy() {
        return initStrategy;
    }

    public Activation getActivation() {
        return activation;
    }

    public List<Neuron> getNeurons() {
        return neurons;
    }

    public BatchNormalizer getBatchNormalizer() {
        return batchNormalizer;
    }
}
