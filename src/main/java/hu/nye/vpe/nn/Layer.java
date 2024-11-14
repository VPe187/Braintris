package hu.nye.vpe.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hu.nye.vpe.GlobalConfig;

/**
 * Layer class.
 */
public class Layer {
    private static final double BETA1_MOMENTUM = GlobalConfig.getInstance().getBeta1Momentum();
    private static final double BETA2_RMSPROP = GlobalConfig.getInstance().getBeta2RmsProp();
    private static final double ADAM_MOMENTUM = GlobalConfig.getInstance().getAdamMomentum();
    private static final double BIAS_L2LAMBDA = GlobalConfig.getInstance().getBiasL2Regularization();

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
    private final double dropoutRate;
    private boolean[] dropoutMask;
    private final Random random = new Random();


    public Layer(String name, int inputSize, int outputSize, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2, BatchNormParameters batchNormParameters,
                 double learningRate, double dropoutRate) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.gradientClipper = gradientClipper;
        this.useBatchNorm = batchNormParameters.useBatchNorm;
        this.learningRate = learningRate;
        this.dropoutRate = dropoutRate;
        this.dropoutMask = new boolean[outputSize];

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
                outputSize,
                inputSize,
                learningRate,
                BETA1_MOMENTUM,
                BETA2_RMSPROP,
                ADAM_MOMENTUM,
                BIAS_L2LAMBDA
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

        // Apply Dropout during training
        if (dropoutRate > 0) {
            if (isTraining) {
                // Generate new dropout mask for each forward pass during training
                for (int i = 0; i < dropoutMask.length; i++) {
                    dropoutMask[i] = random.nextDouble() > dropoutRate;
                    if (dropoutMask[i]) {
                        // Scale up the outputs that survive dropout by 1/(1-dropoutRate)
                        activatedOutputs[i] *= (1.0 / (1.0 - dropoutRate));
                    } else {
                        activatedOutputs[i] = 0.0;
                    }
                }
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

        // Apply Dropout during training
        if (dropoutRate > 0) {
            if (isTraining) {
                // Generate new dropout mask for each batch
                for (int i = 0; i < dropoutMask.length; i++) {
                    dropoutMask[i] = random.nextDouble() > dropoutRate;
                }

                // Apply mask to each instance in the batch
                for (int b = 0; b < outputs.length; b++) {
                    for (int i = 0; i < outputs[b].length; i++) {
                        if (dropoutMask[i]) {
                            // Scale up the outputs that survive dropout by 1/(1-dropoutRate)
                            outputs[b][i] *= (1.0 / (1.0 - dropoutRate));
                        } else {
                            outputs[b][i] = 0.0;
                        }
                    }
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

        // Batch iteráció
        for (int b = 0; b < batchSize; b++) {
            double[] derivativeValues = computeActivationDerivatives(b, neurons.size());
            if (activation == Activation.SOFTMAX || activation == Activation.SOFTMAX_SPLIT) {
                // Softmax speciális kezelése - teljes Jacobian mátrix
                processSoftmaxGradients(b, inputSize, outputSize, inputs,
                        nextLayerDeltas, derivativeValues,
                        weightGradients, inputGradients);
            } else {
                // Egyéb aktivációs függvények gradiens számítása
                processStandardGradients(b, inputSize, outputSize, inputs,
                        nextLayerDeltas, derivativeValues,
                        weightGradients, inputGradients, biasGradients);
            }

            // Apply dropout mask to gradients during backpropagation
            if (dropoutRate > 0) {
                for (int i = 0; i < outputSize; i++) {
                    if (!dropoutMask[i]) {
                        // Zero out gradients for dropped neurons
                        for (int j = 0; j < inputSize; j++) {
                            weightGradients[i][j] = 0.0;
                        }
                        biasGradients[i] = 0.0;
                        inputGradients[b][i] = 0.0;
                    } else {
                        // Scale gradients for active neurons
                        for (int j = 0; j < inputSize; j++) {
                            weightGradients[i][j] *= (1.0 / (1.0 - dropoutRate));
                        }
                        biasGradients[i] *= (1.0 / (1.0 - dropoutRate));
                        inputGradients[b][i] *= (1.0 / (1.0 - dropoutRate));
                    }
                }
            }
        }

        // Batch átlagolás és gradiens clipping
        normalizeAndClipGradients(batchSize, outputSize, inputSize, weightGradients, biasGradients);

        /*
        // Súlyok frissítése normál módon
        for (int i = 0; i < outputSize; i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(weightGradients[i], biasGradients[i], this.learningRate);
        }
         */

        // Súlyok frissítése adam optimizerrel
        optimizer.updateWeights(neurons, weightGradients, biasGradients);

        // Input gradiensek végső skálázása
        for (int b = 0; b < batchSize; b++) {
            inputGradients[b] = gradientClipper.scaleAndClip(inputGradients[b]);
        }

        return new LayerGradients(inputGradients, weightGradients, biasGradients);
    }

    // Aktivációs függvény deriváltak számítása
    private double[] computeActivationDerivatives(int batchIndex, int outputSize) {
        if (activation == Activation.SOFTMAX) {
            return Activation.derivative(batchOutputs[batchIndex], activation);
        } else if (activation == Activation.SOFTMAX_SPLIT) {
            return Activation.derivative(batchOutputs[batchIndex], activation, splitIndex);
        } else {
            double[] derivativeValues = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                derivativeValues[i] = Activation.derivative(
                        new double[]{batchOutputs[batchIndex][i]}, activation)[0];
            }
            return derivativeValues;
        }
    }

    // Softmax gradiensek feldolgozása
    private void processSoftmaxGradients(int batchIndex, int inputSize, int outputSize,
                                         double[][] inputs, double[][] nextLayerDeltas,
                                         double[] derivativeValues, double[][] weightGradients,
                                         double[][] inputGradients) {
        // Softmax teljes Jacobian mátrix használata
        for (int i = 0; i < outputSize; i++) {
            double neuronDelta = 0;
            for (int j = 0; j < outputSize; j++) {
                // Jacobian mátrix elem: derivativeValues[j * outputSize + i]
                neuronDelta += nextLayerDeltas[batchIndex][j] *
                        derivativeValues[j * outputSize + i];
            }

            double[] weights = neurons.get(i).getWeights();
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] += neuronDelta * inputs[batchIndex][j];
                inputGradients[batchIndex][j] += neuronDelta * weights[j];
            }
        }
    }

    // Standard gradiensek feldolgozása
    private void processStandardGradients(int batchIndex, int inputSize, int outputSize,
                                          double[][] inputs, double[][] nextLayerDeltas,
                                          double[] derivativeValues, double[][] weightGradients,
                                          double[][] inputGradients, double[] biasGradients) {
        for (int i = 0; i < outputSize; i++) {
            // Neuron delta számítása
            double neuronDelta = nextLayerDeltas[batchIndex][i] * derivativeValues[i];

            // NaN és Inf ellenőrzés
            if (Double.isNaN(neuronDelta) || Double.isInfinite(neuronDelta)) {
                neuronDelta = 0.0;
            }
            neuronDelta = gradientClipper.clip(neuronDelta);

            // Gradiens akkumulálás
            double[] weights = neurons.get(i).getWeights();
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] += neuronDelta * inputs[batchIndex][j];
                inputGradients[batchIndex][j] += neuronDelta * weights[j];
            }
            biasGradients[i] += neuronDelta;
        }
    }

    // Gradiensek normalizálása és clippelése
    private void normalizeAndClipGradients(int batchSize, int outputSize, int inputSize,
                                           double[][] weightGradients, double[] biasGradients) {
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] = processGradient(weightGradients[i][j], batchSize);
            }
            biasGradients[i] = processGradient(biasGradients[i], batchSize);
        }
    }

    private double processGradient(double gradient, int batchSize) {
        if (Double.isNaN(gradient) || Double.isInfinite(gradient)) {
            return 0.0;
        }
        return gradientClipper.clip(gradient / batchSize);
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

    public Activation getActivation() {
        return activation;
    }

    public GradientClipper getGradientClipper() {
        return gradientClipper;
    }

    public BatchNormalizer getBatchNormalizer() {
        return batchNormalizer;
    }

    public boolean isUseBatchNorm() {
        return useBatchNorm;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double[][] getBatchOutputs() {
        return batchOutputs;
    }

    public int getSplitIndex() {
        return splitIndex;
    }

    public AdamOptimizer getOptimizer() {
        return optimizer;
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
