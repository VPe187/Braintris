package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

public class BatchNormalizer implements Serializable {
    private static final long serialVersionUID = 5L;
    private final int size;
    private final double epsilon = 1e-5;
    private double[] gamma;
    private double[] beta;
    private double[] runningMean;
    private double[] runningVariance;
    private final double momentum = 0.9;
    private double[] lastInputs;
    private double[] lastMean;
    private double[] lastVariance;
    private double[] lastNormalized;
    private double[] gammaGradients;
    private double[] betaGradients;

    public BatchNormalizer(int size, double initialGamma, double initialBeta) {
        this.size = size;
        this.gamma = new double[size];
        this.beta = new double[size];
        this.runningMean = new double[size];
        this.runningVariance = new double[size];
        this.gammaGradients = new double[size];  // Új sor
        this.betaGradients = new double[size];
        Arrays.fill(gamma, initialGamma);
        Arrays.fill(beta, initialBeta);
    }

    public double[] normalize(double[] inputs, boolean training) {
        double[] normalized = new double[size];
        double[] mean = new double[size];
        double[] variance = new double[size];

        if (training) {
            // Számítsuk ki az egyes neuronok átlagát és varianciáját
            for (int i = 0; i < size; i++) {
                mean[i] = inputs[i];
                variance[i] = Math.pow(inputs[i] - mean[i], 2);
            }

            // Frissítsük a runningMean és runningVariance értékeket neurononként
            for (int i = 0; i < size; i++) {
                runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean[i];
                runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance[i];
            }
        } else {
            // Ha nem tanulunk, a running értékeket használjuk
            mean = runningMean;
            variance = runningVariance;
        }

        // Számítsuk ki az egyes neuronok standard deviációját és normalizáljuk az inputot
        double[] stddev = new double[size];
        for (int i = 0; i < size; i++) {
            stddev[i] = Math.sqrt(variance[i] + epsilon);
            normalized[i] = gamma[i] * ((inputs[i] - mean[i]) / stddev[i]) + beta[i];
        }

        // Mentjük az utolsó normalizációhoz használt értékeket a visszaterjesztéshez
        this.lastInputs = inputs.clone();
        this.lastMean = mean.clone();
        this.lastVariance = variance.clone();
        this.lastNormalized = normalized.clone();

        return normalized;
    }

    public double[] backprop(double[] inputGradients) {
        double[] gradients = new double[size];
        double[] invStd = new double[size];

        // Számoljuk ki az inverz standard deviációt az utolsó varianciából
        for (int i = 0; i < size; i++) {
            invStd[i] = 1.0 / Math.sqrt(lastVariance[i] + epsilon);
        }

        double[] sumDy = new double[size];
        double[] sumDyXmu = new double[size];
        for (int i = 0; i < size; i++) {
            sumDy[i] = inputGradients[i];
            sumDyXmu[i] = inputGradients[i] * (lastInputs[i] - lastMean[i]);
        }

        this.gammaGradients = new double[size];
        this.betaGradients = new double[size];

        for (int i = 0; i < size; i++) {
            double xHat = (lastInputs[i] - lastMean[i]) * invStd[i];
            gradients[i] = gamma[i] * invStd[i] * (
                    inputGradients[i]
                            - sumDy[i]
                            - xHat * sumDyXmu[i] * invStd[i]
            );
            this.gammaGradients[i] = sumDyXmu[i] * invStd[i];
            this.betaGradients[i] = sumDy[i];
        }

        return gradients;
    }

    public void updateParameters(double learningRate) {
        if (gammaGradients == null || betaGradients == null) {
            System.err.println("Warning: Gradients are null, skipping parameter update.");
            return;
        }

        for (int i = 0; i < size; i++) {
            gamma[i] -= learningRate * gammaGradients[i];
            beta[i] -= learningRate * betaGradients[i];
        }
    }

    public double[] getGammaGradients() {
        return gammaGradients;
    }

    public double[] getBetaGradients() {
        return betaGradients;
    }
}