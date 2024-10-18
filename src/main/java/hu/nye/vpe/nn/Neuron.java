package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Neuron class.
 */
public class Neuron implements Serializable {
    private double[] weights;
    private double bias;
    private Activation activation;
    private WeightInitStrategy initStrategy;
    private GradientClipper gradientClipper;
    private double lambdaL2;

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
        return Activation.activate(input, activation);
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
            weights[i] -= learningRate * (gradient + lambdaL2 * weights[i]);  // L2 regularizáció
        }

        biasGradient = gradientClipper.clip(biasGradient);
        bias -= learningRate * biasGradient;

    }

    /**
     * Linear transform.
     *
     * @param inputs
     *
     * @return transformed summa
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

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public Activation getActivation() {
        return activation;
    }

    public WeightInitStrategy getInitStrategy() {
        return initStrategy;
    }
}
