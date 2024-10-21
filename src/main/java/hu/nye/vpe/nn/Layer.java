package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Layer class.
 */
public class Layer implements Serializable {
    private static final int BATCH_SIZE = 32;
    private final List<Neuron> neurons;
    private final Activation activation;
    private final WeightInitStrategy initStrategy;
    private final GradientClipper gradientClipper;
    private final String name;
    private BatchNormalizer batchNormalizer;
    private double learningRate;
    private final boolean useBatchNorm;
    private double[][] batchOutputs;

    public Layer(String name, int inputSize, int ouputSize, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2, BatchNormParameters batchNormParameters,
                 double learningRate) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.gradientClipper = gradientClipper;
        this.useBatchNorm = batchNormParameters.useBatchNorm;
        this.learningRate = learningRate;

        for (int i = 0; i < ouputSize; i++) {
            neurons.add(new Neuron(inputSize, ouputSize, activation, initStrategy, gradientClipper, lambdaL2));
        }

        if (useBatchNorm) {
            this.batchNormalizer = new BatchNormalizer(ouputSize, BATCH_SIZE, batchNormParameters.gamma,
                    batchNormParameters.beta, learningRate);
        }
        this.batchOutputs = new double[BATCH_SIZE][ouputSize];
    }

    /**
     * Forward pass.
     *
     * @param inputs input values
     *
     * @param isTraining is training?
     *
     * @return outputs
     */
    public double[] forward(double[] inputs, boolean isTraining) {
        double[] linearOutputs = new double[neurons.size()];

        for (int i = 0; i < neurons.size(); i++) {
            linearOutputs[i] = neurons.get(i).linearTransform(inputs);
        }

        if (useBatchNorm) {
            linearOutputs = batchNormalizer.forward(linearOutputs, isTraining);
        }

        double[] activatedOutputs;
        if (activation == Activation.SOFTMAX) {
            activatedOutputs = Activation.activate(linearOutputs, activation);
        } else {
            activatedOutputs = new double[neurons.size()];
            for (int i = 0; i < neurons.size(); i++) {
                activatedOutputs[i] = neurons.get(i).activate(linearOutputs[i]);
            }
        }

        return activatedOutputs;
    }

    /**
     * Forward batch.
     *
     * @param inputs inputs
     *
     * @param isTraining is training?
     *
     * @return outputs
     */
    public double[][] forwardBatch(double[][] inputs, boolean isTraining) {
        int batchSize = inputs.length;

        double[][] linearOutputs = new double[batchSize][neurons.size()];
        double[][] outputs = new double[batchSize][neurons.size()];

        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < neurons.size(); i++) {
                linearOutputs[b][i] = neurons.get(i).linearTransform(inputs[b]);
            }
        }

        if (useBatchNorm) {
            linearOutputs = batchNormalizer.forwardBatch(linearOutputs, isTraining);
        }

        if (activation == Activation.SOFTMAX) {
            for (int b = 0; b < batchSize; b++) {
                outputs[b] = Activation.activate(linearOutputs[b], activation);
            }
        } else {
            for (int b = 0; b < batchSize; b++) {
                for (int i = 0; i < neurons.size(); i++) {
                    outputs[b][i] = neurons.get(i).activate(linearOutputs[b][i]);
                }
            }
        }

        if (isTraining) {
            this.batchOutputs = outputs;
        }

        return outputs;
    }

    /**
     * Backward batch.
     *
     * @param nextLayerDeltas layer deltas
     *
     * @param inputs inputs
     *
     * @return new layergradients
     */
    public LayerGradients backwardBatch(double[][] nextLayerDeltas, double[][] inputs) {
        int batchSize = nextLayerDeltas.length;
        int inputSize = inputs[0].length;
        int outputSize = neurons.size();

        double[][] inputGradients = new double[batchSize][inputSize];
        double[][] weightGradients = new double[outputSize][inputSize];
        double[] biasGradients = new double[outputSize];

        if (useBatchNorm) {
            nextLayerDeltas = batchNormalizer.backwardBatch(nextLayerDeltas);
        }

        for (int b = 0; b < batchSize; b++) {
            double[] derivativeValues;
            if (activation == Activation.SOFTMAX) {
                derivativeValues = Activation.derivative(batchOutputs[b], activation);
            } else {
                derivativeValues = new double[outputSize];
                for (int i = 0; i < outputSize; i++) {
                    derivativeValues[i] = Activation.derivative(new double[]{batchOutputs[b][i]}, activation)[0];
                }
            }

            if (activation == Activation.SOFTMAX) {
                // For SOFTMAX, we need to handle the Jacobian matrix
                for (int i = 0; i < outputSize; i++) {
                    double neuronDelta = 0;
                    for (int j = 0; j < outputSize; j++) {
                        neuronDelta += nextLayerDeltas[b][j] * derivativeValues[j * outputSize + i];
                    }

                    double[] weights = neurons.get(i).getWeights();
                    for (int j = 0; j < inputSize; j++) {
                        double gradientUpdate = neuronDelta * inputs[b][j];
                        weightGradients[i][j] += gradientUpdate;
                        inputGradients[b][j] += neuronDelta * weights[j];
                    }
                    biasGradients[i] += neuronDelta;
                }
            } else {
                for (int i = 0; i < outputSize; i++) {
                    derivativeValues[i] = Math.max(derivativeValues[i], 1e-10);
                    double neuronDelta = nextLayerDeltas[b][i] * derivativeValues[i];

                    double[] weights = neurons.get(i).getWeights();
                    for (int j = 0; j < inputSize; j++) {
                        double gradientUpdate = neuronDelta * inputs[b][j];
                        weightGradients[i][j] += gradientUpdate;
                        inputGradients[b][j] += neuronDelta * weights[j];
                    }
                    biasGradients[i] += neuronDelta;
                }
            }
        }

        // Average gradients over batch
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] /= batchSize;
            }
            biasGradients[i] /= batchSize;
        }

        // Update weights and biases
        // először klippelünk
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] = gradientClipper.clip(weightGradients[i][j]);
            }
            biasGradients[i] = gradientClipper.clip(biasGradients[i]);
        }
        for (int i = 0; i < outputSize; i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(weightGradients[i], biasGradients[i], this.learningRate);
        }

        for (int b = 0; b < batchSize; b++) {
            inputGradients[b] = gradientClipper.scaleAndClip(inputGradients[b]);
        }

        return new LayerGradients(inputGradients, weightGradients, biasGradients);
    }

    public int getSize() {
        return neurons.size();
    }

    public String getName() {
        return name;
    }

    public WeightInitStrategy getInitStrategy() {
        return initStrategy;
    }

    public Activation getActivation() {
        return activation;
    }

    public List<Neuron> getNeurons() {
        return neurons;
    }

    /**
     * Set learning rate.
     *
     * @param learningRate learningrate
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
        if (useBatchNorm) {
            batchNormalizer.setLearningRate(learningRate);
        }
    }
}
