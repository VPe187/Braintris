package hu.nye.vpe.nn;

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
    private final GradientClipper gradientClipper;
    private double[] lastInputs;
    private double[] lastOutputs;
    private double[] lastNormalizedOutputs;
    private String name;
    private BatchNormParameters batchNormaparameters;
    private BatchNormalizer batchNormalizer;
    private double learningRate;
    private boolean useBatchNorm;

    public Layer(String name, int inputSize, int neuronCount, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2, BatchNormParameters batchNormParameters, double learningRate) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.gradientClipper = gradientClipper;
        this.useBatchNorm = batchNormParameters.useBatchNorm;
        this.learningRate = learningRate;

        for (int i = 0; i < neuronCount; i++) {
            neurons.add(new Neuron(inputSize, neuronCount, activation, initStrategy, gradientClipper, lambdaL2));
        }

        if (useBatchNorm) {
            this.batchNormalizer = new BatchNormalizer(neuronCount, batchNormParameters.gamma, batchNormParameters.beta);
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

        for (int i = 0; i < neurons.size(); i++) {
            outputs[i] = neurons.get(i).activate(inputs);
        }

        if (useBatchNorm) {
            outputs = batchNormalizer.forward(outputs, isTraining);
            this.lastNormalizedOutputs = outputs.clone();
        }

        this.lastOutputs = outputs.clone();

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
        double[] inputGradients = new double[lastInputs.length];
        double[] deltas = nextLayerDeltas.clone();

        if (useBatchNorm) {
            deltas = batchNormalizer.backward(lastOutputs, deltas);
        }

        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            double neuronDelta = deltas[i];
            if (lastNormalizedOutputs != null) {
                neuronDelta *= Activation.derivative(lastNormalizedOutputs[i], activation);
            } else {
                neuronDelta *= Activation.derivative(lastOutputs[i], activation);
            }
            neuron.updateWeights(lastInputs, neuronDelta, learningRate);
            double[] weights = neuron.getWeights();
            for (int j = 0; j < lastInputs.length; j++) {
                inputGradients[j] += neuronDelta * weights[j];
            }
        }

        inputGradients = gradientClipper.scaleAndClip(inputGradients);

        return new LayerGradients(inputGradients, null, null);
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

    /**
     * Set learning rate.
     *
     * @param learningRate learningrate
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
        if (useBatchNorm) {
            batchNormalizer.setLearningRate(learningRate);
        }
    }
}
