package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Batch normalizer class.
 */
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

    /**
     * Normalize.
     *
     * @param inputs values array
     *
     * @param training is training?
     *
     * @return normalized values
     */
    public double[] normalize(double[] inputs, boolean training) {
        double[] normalized = new double[size];
        double[] mean = new double[size];
        double[] variance = new double[size];

        if (training) {
            calculateMeanAndVariance(inputs, mean, variance);
            updateRunningStatistics(mean, variance);
        } else {
            System.arraycopy(runningMean, 0, mean, 0, size);
            System.arraycopy(runningVariance, 0, variance, 0, size);
        }

        normalizeInputs(inputs, mean, variance, normalized);

        // Mentjük az utolsó normalizációhoz használt értékeket a visszaterjesztéshez
        this.lastInputs = Arrays.copyOf(inputs, size);
        this.lastMean = Arrays.copyOf(mean, size);
        this.lastVariance = Arrays.copyOf(variance, size);
        this.lastNormalized = Arrays.copyOf(normalized, size);

        return normalized;
    }

    private void calculateMeanAndVariance(double[] inputs, double[] mean, double[] variance) {
        Arrays.fill(mean, 0);
        Arrays.fill(variance, 0);

        for (int i = 0; i < size; i++) {
            mean[i] = inputs[i];
            variance[i] = Math.pow(inputs[i] - mean[i], 2);
        }
    }

    private void updateRunningStatistics(double[] mean, double[] variance) {
        for (int i = 0; i < size; i++) {
            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean[i];
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance[i];
        }
    }

    private void normalizeInputs(double[] inputs, double[] mean, double[] variance, double[] normalized) {
        for (int i = 0; i < size; i++) {
            int index = i < size ? i : size - 1;  // Prevent index out of bounds
            double stddev = Math.sqrt(variance[index] + epsilon);
            normalized[i] = gamma[i] * ((inputs[index] - mean[index]) / stddev) + beta[i];
        }
    }


    /**
     * Backprop normalization.
     *
     * @param inputGradients gradiens values
     *
     * @return normalized gradiens values
     */
    public double[] backprop(double[] inputGradients) {
        double[] gradients = new double[size];
        double[] invStd = new double[size];

        calculateInverseStandardDeviation(invStd);

        double[] sumDy = calculateSumDy(inputGradients);
        double[] sumDyXmu = calculateSumDyXmu(inputGradients);

        calculateGradients(inputGradients, gradients, invStd, sumDy, sumDyXmu);

        return gradients;
    }

    private void calculateInverseStandardDeviation(double[] invStd) {
        for (int i = 0; i < size; i++) {
            invStd[i] = 1.0 / Math.sqrt(lastVariance[i] + epsilon);
        }
    }

    private double[] calculateSumDy(double[] inputGradients) {
        double[] sumDy = new double[size];
        System.arraycopy(inputGradients, 0, sumDy, 0, size);
        return sumDy;
    }

    private double[] calculateSumDyXmu(double[] inputGradients) {
        double[] sumDyXmu = new double[size];
        for (int i = 0; i < size; i++) {
            int index = i < size ? i : size - 1;  // Prevent index out of bounds
            sumDyXmu[i] = inputGradients[i] * (lastInputs[index] - lastMean[index]);
        }
        return sumDyXmu;
    }

    private void calculateGradients(double[] inputGradients, double[] gradients, double[] invStd, double[] sumDy, double[] sumDyXmu) {
        Arrays.fill(gammaGradients, 0);
        Arrays.fill(betaGradients, 0);

        for (int i = 0; i < size; i++) {
            double xhat = (lastInputs[i] - lastMean[i]) * invStd[i];
            gradients[i] = gamma[i] * invStd[i] * (
                    inputGradients[i] - sumDy[i] - xhat * sumDyXmu[i] * invStd[i]
            );
            gammaGradients[i] = sumDyXmu[i] * invStd[i];
            betaGradients[i] = sumDy[i];
        }
    }

    /**
     * Update normalizer parameters.
     *
     * @param learningRate learning rate
     */
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

    public int getSize() {
        return size;
    }
}