package hu.nye.vpe.nn;

/**
 * Neuron class.
 */
public class Neuron {
    private double[] weights;
    private double bias;
    private final Activation activation;
    private final WeightInitStrategy initStrategy;
    private final GradientClipper gradientClipper;
    private double lambdaL2;
    private double[] weightGradients; // New field
    private double biasGradient;

    public Neuron(int inputSize, int outputSize, Activation activation, WeightInitStrategy initStrategy,
                  GradientClipper gradientClipper, Double lambdaL2) {
        this.gradientClipper = gradientClipper;
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.lambdaL2 = lambdaL2;
        initializeWeightsAndBias(inputSize, outputSize);
        this.weightGradients = new double[inputSize];
        this.biasGradient = 0.0;
    }

    /**
     * Zerogradient, set gradients to zero.
     */
    public void zeroGradients() {
        for (int i = 0; i < weightGradients.length; i++) {
            weightGradients[i] = 0.0;
        }
        biasGradient = 0.0;
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
        this.biasGradient = gradientClipper.clip(biasGradient);
        bias -= learningRate * (biasGradient + lambdaL2 * bias);
        //bias -= learningRate * biasGradient;
    }

    /**
     * Update neuron weight with adam.
     *
     * @param weightUpdates Weight updates.
     *
     * @param biasUpdate Bias updates.
     */
    public void updateWeightsWithAdam(double[] weightUpdates, double biasUpdate) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] += weightUpdates[i] - lambdaL2 * weights[i];
        }
        bias += biasUpdate - lambdaL2 * bias;
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

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double getBias() {
        return bias;
    }

    public double getL2() {
        return lambdaL2;
    }

    public void setL2(double lambdaL2) {
        this.lambdaL2 = lambdaL2;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public GradientClipper getGradientClipper() {
        return gradientClipper;
    }
}
