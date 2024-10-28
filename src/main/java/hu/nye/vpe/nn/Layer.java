package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hu.nye.vpe.GlobalConfig;

/**
 * Layer class.
 */
public class Layer implements Serializable {
    private static final double BETA1_MOMENTUM = GlobalConfig.getInstance().getBeta1Momentum();
    private static final double BETA2_RMSPROP = GlobalConfig.getInstance().getBeta2RmsProp();
    private static final double ADAM_MOMENTUM = GlobalConfig.getInstance().getAdamMomentum();

    private final List<Neuron> neurons;
    private final Activation activation;
    private final GradientClipper gradientClipper;
    private final String name;
    private BatchNormalizer batchNormalizer;
    private double learningRate;
    private final boolean useBatchNorm;
    private double[][] batchOutputs;
    private int splitIndex;
    private final AdamOptimizer optimizer;

    public Layer(String name, int inputSize, int outputSize, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2, BatchNormParameters batchNormParameters,
                 double learningRate) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.gradientClipper = gradientClipper;
        this.useBatchNorm = batchNormParameters.useBatchNorm;
        this.learningRate = learningRate;

        for (int i = 0; i < outputSize; i++) {
            neurons.add(new Neuron(inputSize, outputSize, activation, initStrategy, gradientClipper, lambdaL2));
        }

        int actualBatchSize;
        if (GlobalConfig.getInstance().getUseExperience()) {
            actualBatchSize = GlobalConfig.getInstance().getExperienceBatchSize();
        } else {
            actualBatchSize = GlobalConfig.getInstance().getMinimumBatchSize();
        }

        if (useBatchNorm) {
            this.batchNormalizer = new BatchNormalizer(outputSize, actualBatchSize, batchNormParameters.gamma,
                    batchNormParameters.beta, learningRate);
        }

        this.batchOutputs = new double[actualBatchSize][outputSize];

        if (activation == Activation.SOFTMAX_SPLIT) {
            this.splitIndex = 12;
        }

        this.optimizer = new AdamOptimizer(
                outputSize, // neuronok száma
                inputSize, // bemenetek száma
                learningRate, // kezdeti tanulási ráta
                BETA1_MOMENTUM, // beta1 (momentum) - PyTorch default
                BETA2_RMSPROP, // beta2 (RMSprop) - PyTorch default
                ADAM_MOMENTUM // epsilon - PyTorch default
        );
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
        } else if (activation == Activation.SOFTMAX_SPLIT) {
            activatedOutputs = Activation.activate(linearOutputs, activation, splitIndex);
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
        } else if (activation == Activation.SOFTMAX_SPLIT) {
            for (int b = 0; b < batchSize; b++) {
                outputs[b] = Activation.activate(linearOutputs[b], activation, splitIndex);
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

    private void zeroGradients(double[][] weightGradients, double[] biasGradients) {
        for (int i = 0; i < neurons.size(); i++) {
            for (int j = 0; j < weightGradients[i].length; j++) {
                weightGradients[i][j] = 0.0;
            }
            biasGradients[i] = 0.0;
        }
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

        zeroGradients(weightGradients, biasGradients);

        if (useBatchNorm) {
            nextLayerDeltas = batchNormalizer.backwardBatch(nextLayerDeltas);
        }

        for (int b = 0; b < batchSize; b++) {
            double[] derivativeValues;
            if (activation == Activation.SOFTMAX) {
                derivativeValues = Activation.derivative(batchOutputs[b], activation);
            } else if (activation == Activation.SOFTMAX_SPLIT) {
                derivativeValues = Activation.derivative(batchOutputs[b], activation, splitIndex);
            } else {
                derivativeValues = new double[outputSize];
                for (int i = 0; i < outputSize; i++) {
                    derivativeValues[i] = Activation.derivative(new double[]{batchOutputs[b][i]}, activation)[0];
                }
            }

            if (activation == Activation.SOFTMAX || activation == Activation.SOFTMAX_SPLIT) {
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
                }

            } else {
                for (int i = 0; i < outputSize; i++) {
                    double neuronDelta = nextLayerDeltas[b][i] * derivativeValues[i];
                    if (Double.isNaN(neuronDelta) || Double.isInfinite(neuronDelta)) {
                        neuronDelta = 0.0;
                    }
                    neuronDelta = gradientClipper.clip(neuronDelta);

                    double[] weights = neurons.get(i).getWeights();
                    for (int j = 0; j < inputSize; j++) {
                        double gradientUpdate = neuronDelta * inputs[b][j];
                        weightGradients[i][j] += gradientUpdate;
                        inputGradients[b][j] += neuronDelta * weights[j] / batchSize;
                    }
                    biasGradients[i] += neuronDelta;
                }
            }
        }

        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                if (Double.isNaN(weightGradients[i][j]) || Double.isInfinite(weightGradients[i][j])) {
                    weightGradients[i][j] = 0.0;
                }
                weightGradients[i][j] /= batchSize;
                weightGradients[i][j] = gradientClipper.clip(weightGradients[i][j]);
            }
            if (Double.isNaN(biasGradients[i]) || Double.isInfinite(biasGradients[i])) {
                biasGradients[i] = 0.0;
            }
            biasGradients[i] /= batchSize;
            biasGradients[i] = gradientClipper.clip(biasGradients[i]);
        }

        /*
        for (int i = 0; i < outputSize; i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(weightGradients[i], biasGradients[i], this.learningRate);
        }
         */

        optimizer.updateWeights(neurons, weightGradients, biasGradients);

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
