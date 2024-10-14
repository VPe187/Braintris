package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Optimized Batch normalizer class.
 */
public class BatchNormalizer implements Serializable {
    private static final double DEFAULT_EPSILON = 1e-6;
    private static final double DEFAULT_MOMENTUM = 0.99;
    private static final double DEFAULT_LEARNING_RATE = 0.01;
    private static final double DEFAULT_GAMMA = 1.5;
    private static final double DEFAULT_BETA = 0.2;

    private final double epsilon;
    private final double momentum;
    private double learningRate;
    private final double[] runningMean;
    private final double[] runningVariance;
    private final double[] gamma;
    private final double[] beta;
    private final int size;

    public BatchNormalizer(int size) {
        this(size, DEFAULT_EPSILON, DEFAULT_MOMENTUM, DEFAULT_LEARNING_RATE);
    }

    public BatchNormalizer(int size, double epsilon, double momentum, double learningRate) {
        this.size = size;
        this.epsilon = epsilon;
        this.momentum = momentum;
        this.learningRate = learningRate;
        this.runningMean = new double[size];
        this.runningVariance = new double[size];
        this.gamma = new double[size];
        this.beta = new double[size];

        Arrays.fill(gamma, DEFAULT_GAMMA);
        Arrays.fill(beta, DEFAULT_BETA);
    }

    /**
     * Forward normalizing.
     *
     * @param input input datas
     *
     * @param isTraining is training?
     *
     * @return output datas
     */
    public double[] forward(double[] input, boolean isTraining) {
        if (input.length != size) {
            throw new IllegalArgumentException("Input size does not match BatchNormalizer size");
        }

        double[] output = new double[size];
        double mean = calculateMean(input);
        double variance = calculateVariance(input, mean);

        if (isTraining) {
            updateRunningStatistics(mean, variance);
        }

        double invStd = 1.0 / Math.sqrt(variance + epsilon);

        for (int i = 0; i < size; i++) {
            double normalized = (input[i] - mean) * invStd;
            output[i] = gamma[i] * normalized + beta[i];
        }

        return output;
    }

    /**
     * Backward normalize.
     *
     * @param input input datas
     *
     * @param gradOutput output gradient
     *
     * @return intput gradient
     */
    public double[] backward(double[] input, double[] gradOutput) {
        if (input.length != size || gradOutput.length != size) {
            throw new IllegalArgumentException("Input or gradOutput size does not match BatchNormalizer size");
        }

        double[] gradInput = new double[size];
        double[] gradGamma = new double[size];
        double[] gradBeta = new double[size];

        double mean = calculateMean(input);
        double variance = calculateVariance(input, mean);
        double invStd = 1.0 / Math.sqrt(variance + epsilon);

        calculateGradients(input, gradOutput, mean, invStd, gradInput, gradGamma, gradBeta);
        updateParameters(gradGamma, gradBeta);

        return gradInput;
    }

    private double calculateMean(double[] input) {
        return Arrays.stream(input).average().orElse(0.0);
    }

    private double calculateVariance(double[] input, double mean) {
        return Arrays.stream(input)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
    }

    private void updateRunningStatistics(double mean, double variance) {
        for (int i = 0; i < size; i++) {
            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean;
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance;
        }
    }

    private void calculateGradients(double[] input, double[] gradOutput, double mean, double invStd,
                                    double[] gradInput, double[] gradGamma, double[] gradBeta) {
        double sumGradOutput = 0;
        double sumGradOutputTimesInput = 0;

        for (int i = 0; i < size; i++) {
            gradGamma[i] = gradOutput[i] * (input[i] - mean) * invStd;
            gradBeta[i] = gradOutput[i];
            sumGradOutput += gradOutput[i];
            sumGradOutputTimesInput += gradOutput[i] * (input[i] - mean);
        }

        double factor = gamma[0] * invStd / size;
        for (int i = 0; i < size; i++) {
            gradInput[i] = factor * (size * gradOutput[i] - sumGradOutput - (input[i] - mean) * invStd * sumGradOutputTimesInput);
        }
    }

    private void updateParameters(double[] gradGamma, double[] gradBeta) {
        for (int i = 0; i < size; i++) {
            gamma[i] -= learningRate * gradGamma[i];
            beta[i] -= learningRate * gradBeta[i];
        }
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
}