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
    private final GradientClipper gradientClipper;
    private double[] lastInputs;
    private String name;

    public Layer(String name, int inputSize, int neuronCount, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.gradientClipper = gradientClipper;
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

        for (int i = 0; i < neurons.size(); i++) {
            outputs[i] = neurons.get(i).activate(inputs);
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
        double[] inputGradients = new double[lastInputs.length];

        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(lastInputs, nextLayerDeltas[i], learningRate);
            double[] weights = neuron.getWeights();
            for (int j = 0; j < lastInputs.length; j++) {
                inputGradients[j] += nextLayerDeltas[i] * weights[j];
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
}
