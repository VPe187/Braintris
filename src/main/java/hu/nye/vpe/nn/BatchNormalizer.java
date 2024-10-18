package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Batch normalizer class.
 */
public class BatchNormalizer implements Serializable {
    private static final double DEFAULT_EPSILON = 1e-5;
    private static final double DEFAULT_MOMENTUM = 0.99;

    private final int size;
    private final int batchSize;
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


    public BatchNormalizer(int size, int batchSize, double gamma, double beta, double learningRate) {
        this(size, batchSize, gamma, beta, learningRate, DEFAULT_EPSILON, DEFAULT_MOMENTUM);
    }

    public BatchNormalizer(int size, int batchSize, double gamma, double beta, double learningRate, double epsilon, double momentum) {
        this.size = size;
        this.batchSize = batchSize;
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
    }

    /**
     * Forward normalizing for a single input.
     *
     * @param input input data
     * @param isTraining is training?
     * @return output data
     */
    public double[] forward(double[] input, boolean isTraining) {
        if (isTraining) {
            return forwardTraining(input);
        } else {
            return forwardInference(input);
        }
    }

    private double[] forwardTraining(double[] input) {
        System.arraycopy(input, 0, batchInputs[batchIndex], 0, size);
        batchIndex++;

        if (batchIndex == batchSize) {
            processBatch();
            batchIndex = 0;
        }

        return Arrays.copyOf(input, input.length);
    }

    private void processBatch() {
        double[] batchMean = new double[size];
        double[] batchVariance = new double[size];

        // Calculate batch mean
        for (int i = 0; i < size; i++) {
            double sum = 0;
            for (int j = 0; j < batchSize; j++) {
                sum += batchInputs[j][i];
            }
            batchMean[i] = sum / batchSize;
        }

        // Calculate batch variance
        for (int i = 0; i < size; i++) {
            double sum = 0;
            for (int j = 0; j < batchSize; j++) {
                double diff = batchInputs[j][i] - batchMean[i];
                sum += diff * diff;
            }
            batchVariance[i] = sum / batchSize;
        }

        // Normalize and scale
        for (int i = 0; i < size; i++) {
            double invStd = 1.0 / Math.sqrt(batchVariance[i] + epsilon);
            for (int j = 0; j < batchSize; j++) {
                batchNormalized[j][i] = ((batchInputs[j][i] - batchMean[i]) * invStd * gamma[i]) + beta[i];
            }
        }

        // Update running statistics
        for (int i = 0; i < size; i++) {
            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * batchMean[i];
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * batchVariance[i];
        }
    }

    private double[] forwardInference(double[] input) {
        double[] output = new double[size];
        for (int i = 0; i < size; i++) {
            output[i] = ((input[i] - runningMean[i]) / Math.sqrt(runningVariance[i] + epsilon)) * gamma[i] + beta[i];
        }
        return output;
    }

    /**
     * Forward normalizing for a batch of inputs.
     *
     * @param inputs batch of input data
     * @param isTraining is training?
     * @return batch of output data
     */
    public double[][] forwardBatch(double[][] inputs, boolean isTraining) {
        if (isTraining) {
            return forwardBatchTraining(inputs);
        } else {
            return forwardBatchInference(inputs);
        }
    }

    private double[][] forwardBatchTraining(double[][] inputs) {
        int currentBatchSize = inputs.length;
        double[][] outputs = new double[currentBatchSize][size];

        double[] batchMean = new double[size];
        double[] batchVariance = new double[size];

        // Calculate batch mean
        for (int i = 0; i < size; i++) {
            double sum = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                sum += inputs[j][i];
            }
            batchMean[i] = sum / currentBatchSize;
        }

        // Calculate batch variance
        for (int i = 0; i < size; i++) {
            double sum = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double diff = inputs[j][i] - batchMean[i];
                sum += diff * diff;
            }
            batchVariance[i] = sum / currentBatchSize;
        }

        // Normalize and scale
        for (int i = 0; i < size; i++) {
            double invStd = 1.0 / Math.sqrt(batchVariance[i] + epsilon);
            for (int j = 0; j < currentBatchSize; j++) {
                outputs[j][i] = ((inputs[j][i] - batchMean[i]) * invStd * gamma[i]) + beta[i];
            }
        }

        // Update running statistics
        for (int i = 0; i < size; i++) {
            runningMean[i] = momentum * runningMean[i] + (1 - momentum) * batchMean[i];
            runningVariance[i] = momentum * runningVariance[i] + (1 - momentum) * batchVariance[i];
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
     * Backward normalizing.
     *
     * @param gradOutput outputs
     *
     * @return inputs
     */
    public double[] backward(double[] gradOutput) {
        double[][] batchGradOutput = new double[][]{gradOutput};
        double[][] batchGradInput = backwardBatch(batchGradOutput);
        return batchGradInput[0];
    }

    /**
     * Backward normalize for a batch of inputs.
     *
     * @param gradOutputs batch of output gradients
     * @return batch of input gradients
     */
    public double[][] backwardBatch(double[][] gradOutputs) {
        int currentBatchSize = gradOutputs.length;
        double[][] gradInputs = new double[currentBatchSize][size];
        double[] gradGamma = new double[size];
        double[] gradBeta = new double[size];

        // Calculate batch mean and variance
        double[] batchMean = new double[size];
        double[] batchVariance = new double[size];

        for (int i = 0; i < size; i++) {
            double sum = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                sum += batchInputs[j][i];
            }
            batchMean[i] = sum / currentBatchSize;

            double sumSquares = 0;
            for (int j = 0; j < currentBatchSize; j++) {
                double diff = batchInputs[j][i] - batchMean[i];
                sumSquares += diff * diff;
            }
            batchVariance[i] = sumSquares / currentBatchSize;
        }

        // Calculate gradients
        for (int i = 0; i < size; i++) {
            double invStd = 1.0 / Math.sqrt(batchVariance[i] + epsilon);

            for (int j = 0; j < currentBatchSize; j++) {
                double xnorm = (batchInputs[j][i] - batchMean[i]) * invStd;
                gradGamma[i] += gradOutputs[j][i] * xnorm;
                gradBeta[i] += gradOutputs[j][i];
                gradInputs[j][i] = gradOutputs[j][i] * gamma[i] * invStd;
            }
        }

        // Update parameters
        for (int i = 0; i < size; i++) {
            gamma[i] -= learningRate * gradGamma[i] / currentBatchSize;
            beta[i] -= learningRate * gradBeta[i] / currentBatchSize;
        }

        return gradInputs;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
}