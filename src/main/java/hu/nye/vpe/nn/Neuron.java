package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Neuron class.
 */
public class Neuron implements Serializable {
    private double[] weights;
    private double bias;
    private final Activation activation;
    private final WeightInitStrategy initStrategy;
    private final GradientClipper gradientClipper;
    private final double lambdaL2;

    public Neuron(int inputSize, int outputSize, Activation activation, WeightInitStrategy initStrategy,
                  GradientClipper gradientClipper, Double lambdaL2) {
        this.gradientClipper = gradientClipper;
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.lambdaL2 = lambdaL2;
        initializeWeightsAndBias(inputSize, outputSize);
    }

    private void initializeWeightsAndBias(int inputSize, int outputSize) {
        this.weights = WeightInitializer.initializeWeights(inputSize, outputSize, initStrategy);
        this.bias = WeightInitializer.initializeBias(initStrategy);
    }

    /**
     * Activate neuron.
     *
     * @param input data of inputs
     *
     * @return activated data of inputs
     */
    public double activate(double input) {
        return Activation.activate(new double[]{input}, activation)[0];
    }

    /**
     * Update weights.
     *
     * @param weightGradients gradients weight
     *
     * @param biasGradient gradients biases
     *
     * @param learningRate learning rate
     */
    public void updateWeights(double[] weightGradients, double biasGradient, double learningRate) {
        for (int i = 0; i < weights.length; i++) {
            double gradient = gradientClipper.clip(weightGradients[i]);
            weights[i] -= learningRate * (gradient + lambdaL2 * weights[i]);
        }

        biasGradient = gradientClipper.clip(biasGradient);
        bias -= learningRate * biasGradient;
    }

    /**
     * Linear transform.
     *
     * @param inputs All inputs.
     *
     * @return Transformed summa.
     */
    public double linearTransform(double[] inputs) {
        double sum = bias;
        for (int i = 0; i < inputs.length; i++) {
            sum += inputs[i] * weights[i];
        }
        return sum;
    }

    public double[] getWeights() {
        return weights;
    }
}
