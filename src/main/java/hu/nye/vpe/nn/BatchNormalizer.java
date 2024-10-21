package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

import hu.nye.vpe.GlobalConfig;

/**
 * Batch normalizer class.
 */
public class BatchNormalizer implements Serializable {
    private static final double DEFAULT_EPSILON = GlobalConfig.getInstance().getBatchEpsilon();
    private static final double DEFAULT_MOMENTUM = GlobalConfig.getInstance().getBatchMomentum();

    private final int size;
    private final double epsilon;
    private final double momentum;
    private double learningRate;
    private final double[] runningMean;
    private final double[] runningVariance;
    private final double[] gamma;
    private final double[] beta;
    private double[][] batchInputs;
    private double[][] batchNormalized;
    private int batchIndex;
    private long samplesSeen;

    public BatchNormalizer(int size, int batchSize, double gamma, double beta, double learningRate) {
        this(size, batchSize, gamma, beta, learningRate, DEFAULT_EPSILON, DEFAULT_MOMENTUM);
    }

    public BatchNormalizer(int size, int batchSize, double gamma, double beta, double learningRate, double epsilon, double momentum) {
        this.size = size;
        this.gamma = new double[size];
        this.beta = new double[size];
        Arrays.fill(this.gamma, gamma);
        Arrays.fill(this.beta, beta);
        this.learningRate = learningRate;
        this.epsilon = epsilon;
        this.momentum = momentum;
        this.runningMean = new double[size];
        this.runningVariance = new double[size];
        this.batchInputs = new double[batchSize][size];
        this.batchNormalized = new double[batchSize][size];
        this.batchIndex = 0;
        this.samplesSeen = 0;
    }

    public double[] forward(double[] input, boolean isTraining) {
        return isTraining ? forwardTraining(input) : forwardInference(input);
    }

    private double[] forwardTraining(double[] input) {
        samplesSeen++;
        double[] output = new double[size];

        for (int i = 0; i < size; i++) {
            // Frissítjük a running statisztikákat
            double delta = input[i] - runningMean[i];
            runningMean[i] += (1 - momentum) * delta / samplesSeen;

            if (samplesSeen > 1) {
                runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * delta * delta * (samplesSeen / (samplesSeen - 1));
            } else {
                runningVariance[i] = delta * delta;
            }

            // Normalizáljuk az inputot
            double stdDev = Math.sqrt(runningVariance[i] + epsilon);
            output[i] = ((input[i] - runningMean[i]) / stdDev) * gamma[i] + beta[i];
        }

        // Tároljuk az eredeti és normalizált értékeket a batch-ben
        System.arraycopy(input, 0, batchInputs[batchIndex], 0, size);
        System.arraycopy(output, 0, batchNormalized[batchIndex], 0, size);

        if (++batchIndex == batchInputs.length) {
            processBatch();
            batchIndex = 0;
        }

        return output;
    }

    private void processBatch() {
        int currentBatchSize = batchIndex;
        if (currentBatchSize <= 1) {
            return;
        }
        double[] batchMean = new double[size];
        double[] batchVariance = new double[size];

        // Számoljuk ki a batch statisztikákat
        for (int i = 0; i < size; i++) {
            double sum = 0;
            double sumSquares = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double val = batchInputs[j][i];
                sum += val;
                sumSquares += val * val;
            }
            batchMean[i] = sum / currentBatchSize;
            batchVariance[i] = (sumSquares / currentBatchSize) - (batchMean[i] * batchMean[i]);

            // Finomhangoljuk a running statisztikákat a batch alapján
            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * batchMean[i];
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * batchVariance[i] *
                    currentBatchSize / (currentBatchSize - 1);
        }
    }

    private double[] forwardInference(double[] input) {
        double[] output = new double[size];
        for (int i = 0; i < size; i++) {
            double stdDev = Math.sqrt(runningVariance[i] + epsilon);
            output[i] = ((input[i] - runningMean[i]) / stdDev) * gamma[i] + beta[i];
        }
        return output;
    }

    public double[][] forwardBatch(double[][] inputs, boolean isTraining) {
        return isTraining ? forwardBatchTraining(inputs) : forwardBatchInference(inputs);
    }

    private double[][] forwardBatchTraining(double[][] inputs) {
        int currentBatchSize = inputs.length;
        double[][] outputs = new double[currentBatchSize][size];

        for (int i = 0; i < size; i++) {
            double sum = 0;
            double sumSquares = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double val = inputs[j][i];
                sum += val;
                sumSquares += val * val;
            }
            double mean = sum / currentBatchSize;
            double variance = Math.max((sumSquares / currentBatchSize) - (mean * mean), epsilon);

            double invStd = 1.0 / Math.sqrt(variance + epsilon);

            for (int j = 0; j < currentBatchSize; j++) {
                outputs[j][i] = ((inputs[j][i] - mean) * invStd * gamma[i]) + beta[i];
            }

            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * mean;
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * variance;
        }

        this.batchInputs = inputs;
        this.batchNormalized = outputs;

        return outputs;
    }

    private double[][] forwardBatchInference(double[][] inputs) {
        int currentBatchSize = inputs.length;
        double[][] outputs = new double[currentBatchSize][size];

        for (int i = 0; i < size; i++) {
            double invStd = 1.0 / Math.sqrt(runningVariance[i] + epsilon);
            for (int j = 0; j < currentBatchSize; j++) {
                outputs[j][i] = ((inputs[j][i] - runningMean[i]) * invStd * gamma[i]) + beta[i];
            }
        }

        return outputs;
    }

    /**
     * Backward batch.
     *
     * @param gradOutputs Outputs gradient
     *
     * @return Gradient inputs
     */
    public double[][] backwardBatch(double[][] gradOutputs) {
        int currentBatchSize = gradOutputs.length;
        double[][] gradInputs = new double[currentBatchSize][size];
        double[] gradGamma = new double[size];
        double[] gradBeta = new double[size];

        for (int i = 0; i < size; i++) {
            Arrays.fill(gradGamma, 0);
            Arrays.fill(gradBeta, 0);

            double sum = 0;
            double sumSquares = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double val = batchInputs[j][i];
                sum += val;
                sumSquares += val * val;
            }
            double mean = sum / currentBatchSize;
            double variance = Math.max((sumSquares / currentBatchSize) - (mean * mean), epsilon);
            double invStd = 1.0 / Math.sqrt(variance + epsilon);

            double sumDy = 0;
            double sumDyXn = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double xnorm = (batchInputs[j][i] - mean) * invStd;
                sumDy += gradOutputs[j][i];
                sumDyXn += gradOutputs[j][i] * xnorm;
                gradGamma[i] += gradOutputs[j][i] * xnorm;
                gradBeta[i] += gradOutputs[j][i];
                gradInputs[j][i] = gradOutputs[j][i] * gamma[i] * invStd;
            }

            sumDy = 0;
            sumDyXn = 0;

            for (int j = 0; j < currentBatchSize; j++) {
                double xnorm = (batchInputs[j][i] - mean) * invStd;
                gradInputs[j][i] = gamma[i] * invStd * (gradOutputs[j][i] - (sumDy / currentBatchSize)
                        - xnorm * (sumDyXn / currentBatchSize));
            }

            gamma[i] -= learningRate * gradGamma[i] / currentBatchSize;
            beta[i] -= learningRate * gradBeta[i] / currentBatchSize;
            gradGamma[i] = Math.max(-1.0, Math.min(gradGamma[i], 1.0));
            gradBeta[i] = Math.max(-1.0, Math.min(gradBeta[i], 1.0));
        }

        return gradInputs;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
}