package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

public class BatchNormalizer implements Serializable {
    private static final long serialVersionUID = 5L;
    private final int size;
    private final double epsilon = 1e-3;
    private double[] gamma;
    private double[] beta;
    private double[] runningMean;
    private double[] runningVariance;
    private final double momentum = 0.99;
    private double[] lastInputs;
    private double[] lastMean;
    private double[] lastVariance;
    private double[] lastNormalized;

    public BatchNormalizer(int size, double initialGamma, double initialBeta) {
        this.size = size;
        this.gamma = new double[size];
        this.beta = new double[size];
        this.runningMean = new double[size];
        this.runningVariance = new double[size];
        Arrays.fill(gamma, initialGamma);
        Arrays.fill(beta, initialBeta);
    }

    public double[] normalize(double[] inputs, boolean training) {
        double[] normalized = new double[size];
        double mean;
        double variance;

        if (training) {
            mean = Arrays.stream(inputs).average().orElse(0.0);
            variance = Arrays.stream(inputs).map(x -> Math.pow(x - mean, 2)).average().orElse(0.0);

            // Update running statistics
            for (int i = 0; i < size; i++) {
                runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean;
                runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance;
            }
        } else {
            mean = runningMean[0];
            variance = runningVariance[0];
        }

        double stddev = Math.sqrt(variance + epsilon);
        for (int i = 0; i < size; i++) {
            normalized[i] = gamma[i] * ((inputs[i] - mean) / stddev) + beta[i];
        }

        /*
        System.out.println("Debug - BN input: " + Arrays.toString(inputs));
        System.out.println("Debug - BN output: " + Arrays.toString(normalized));
        System.out.println("Debug - BN gamma: " + Arrays.toString(gamma));
        System.out.println("Debug - BN beta: " + Arrays.toString(beta));
         */

        this.lastInputs = inputs.clone();
        this.lastMean = new double[]{mean};  // Átlag minden neuronra ugyanaz
        this.lastVariance = new double[]{variance};  // Variancia minden neuronra ugyanaz
        this.lastNormalized = normalized.clone();

        return normalized;
    }

    public double[] backprop(double[] inputGradients) {
        double[] gradients = new double[size];
        double mean = lastMean[0];
        double variance = lastVariance[0];
        double invStd = 1.0 / Math.sqrt(variance + epsilon);

        double sumDy = 0;
        double sumDyXmu = 0;
        for (int i = 0; i < size; i++) {
            sumDy += inputGradients[i];
            sumDyXmu += inputGradients[i] * (lastInputs[i] - mean);
        }

        for (int i = 0; i < size; i++) {
            double dx = inputGradients[i] * gamma[i];
            dx = invStd * (dx - sumDy / size - (lastInputs[i] - mean) * sumDyXmu * invStd * invStd / size);
            gradients[i] = dx;
        }

        return gradients;
    }

    public void updateParameters(double learningRate, double[] gradientGamma, double[] gradientBeta) {
        for (int i = 0; i < size; i++) {
            gamma[i] -= learningRate * gradientGamma[i];
            beta[i] -= learningRate * gradientBeta[i];
        }
    }

    public double[] getGamma() {
        return gamma;
    }

    public double[] getBeta() {
        return beta;
    }

    public double[] getLastNormalized() {
        return lastNormalized;
    }
}