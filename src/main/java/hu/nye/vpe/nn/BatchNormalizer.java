package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

public class BatchNormalizer implements Serializable {
    private static final long serialVersionUID = 5L;
    private final int inputSize;
    private final int outputSize;
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

    public BatchNormalizer(int inputSize, int outputSize, double initialGamma, double initialBeta) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.gamma = new double[inputSize];
        this.beta = new double[inputSize];
        this.runningMean = new double[inputSize];
        this.runningVariance = new double[inputSize];
        this.gammaGradients = new double[inputSize];  // Új sor
        this.betaGradients = new double[inputSize];
        Arrays.fill(gamma, initialGamma);
        Arrays.fill(beta, initialBeta);
    }

    public double[] normalize(double[] inputs, boolean training) {
        double[] normalized = new double[inputSize];
        double[] mean = new double[inputSize];
        double[] variance = new double[inputSize];

        if (training) {
            // Számítsuk ki az egyes neuronok átlagát és varianciáját
            for (int i = 0; i < inputSize; i++) {
                mean[i] = inputs[i];
                variance[i] = Math.pow(inputs[i] - mean[i], 2);
            }

            // Frissítsük a runningMean és runningVariance értékeket neurononként
            for (int i = 0; i < inputSize; i++) {
                runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean[i];
                runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance[i];
            }
        } else {
            // Ha nem tanulunk, a running értékeket használjuk
            mean = runningMean;
            variance = runningVariance;
        }

        // Számítsuk ki az egyes neuronok standard deviációját és normalizáljuk az inputot
        double[] stddev = new double[inputSize];
        for (int i = 0; i < inputSize; i++) {
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
        double[] gradients = new double[outputSize];
        double[] invStd = new double[outputSize];

        // Számoljuk ki az inverz standard deviációt az utolsó varianciából
        for (int i = 0; i < outputSize; i++) {
            invStd[i] = 1.0 / Math.sqrt(lastVariance[i] + epsilon);
        }

        double[] sumDy = new double[outputSize];
        double[] sumDyXmu = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            sumDy[i] = inputGradients[i];
            sumDyXmu[i] = inputGradients[i] * (lastInputs[i] - lastMean[i]);
        }

        this.gammaGradients = new double[outputSize];
        this.betaGradients = new double[outputSize];

        for (int i = 0; i < outputSize; i++) {
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

        for (int i = 0; i < outputSize; i++) {
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

    public int getInputSize() {
        return inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }
}