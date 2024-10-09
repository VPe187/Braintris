package hu.nye.vpe.nn;

import java.io.Serializable;

public class BatchNormalizer implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int size;
    private final double epsilon = 1e-5;
    private double gamma;
    private double beta;
    private double[] runningMean;
    private double[] runningVariance;
    private final double momentum = 0.99;

    public BatchNormalizer(int size, double initialGamma, double initialBeta) {
        this.size = size;
        this.gamma = initialGamma;
        this.beta = initialBeta;
        this.runningMean = new double[size];
        this.runningVariance = new double[size];
    }

    public double[] normalize(double[] inputs, boolean training) {
        double[] normalized = new double[size];

        if (training) {
            double mean = 0;
            double variance = 0;

            // Számoljuk ki az átlagot és a varianciát
            for (double input : inputs) {
                mean += input;
            }
            mean /= size;

            for (double input : inputs) {
                variance += Math.pow(input - mean, 2);
            }
            variance = Math.max(variance / size, epsilon);

            // Frissítsük a futó statisztikákat
            for (int i = 0; i < size; i++) {
                runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean;
                runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance;
            }

            // Normalizáljuk az inputokat
            for (int i = 0; i < size; i++) {
                normalized[i] = gamma * ((inputs[i] - mean) / Math.sqrt(variance + epsilon)) + beta;
            }
        } else {
            // Tesztelés közben használjuk a futó statisztikákat
            for (int i = 0; i < size; i++) {
                normalized[i] = gamma * ((inputs[i] - runningMean[i]) / Math.sqrt(runningVariance[i] + epsilon)) + beta;
            }
        }

        return normalized;
    }

    public void updateParameters(double learningRate, double[] gradientGamma, double[] gradientBeta) {
        for (int i = 0; i < size; i++) {
            gamma -= learningRate * gradientGamma[i];
            beta -= learningRate * gradientBeta[i];
        }
    }

    // Getter metódusok
    public double getGamma() {
        return gamma;
    }

    public double getBeta() {
        return beta;
    }
}
