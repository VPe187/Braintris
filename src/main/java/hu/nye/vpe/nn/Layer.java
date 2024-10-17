package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Layer implements Serializable {
    private static final int BATCH_SIZE = 32;
    private final List<Neuron> neurons;
    private final Activation activation;
    private final WeightInitStrategy initStrategy;
    private final GradientClipper gradientClipper;
    private double[] lastInputs;
    private double[] lastOutputs;
    private double[] lastNormalizedOutputs;
    private String name;
    private BatchNormParameters batchNormParameters;
    private BatchNormalizer batchNormalizer;
    private double learningRate;
    private boolean useBatchNorm;
    private double[][] batchInputs;
    private double[][] batchOutputs;

    public Layer(String name, int inputSize, int neuronCount, Activation activation, WeightInitStrategy initStrategy,
                 GradientClipper gradientClipper, double lambdaL2, BatchNormParameters batchNormParameters, double learningRate) {
        this.name = name;
        this.neurons = new ArrayList<>();
        this.activation = activation;
        this.initStrategy = initStrategy;
        this.gradientClipper = gradientClipper;
        this.useBatchNorm = batchNormParameters.useBatchNorm;
        this.learningRate = learningRate;
        this.batchNormParameters = batchNormParameters;

        for (int i = 0; i < neuronCount; i++) {
            neurons.add(new Neuron(inputSize, neuronCount, activation, initStrategy, gradientClipper, lambdaL2));
        }

        if (useBatchNorm) {
            this.batchNormalizer = new BatchNormalizer(neuronCount, BATCH_SIZE, batchNormParameters.gamma, batchNormParameters.beta, learningRate);
        }
        this.batchInputs = new double[BATCH_SIZE][inputSize];
        this.batchOutputs = new double[BATCH_SIZE][neuronCount];
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
        double[] outputs = new double[neurons.size()];

        for (int i = 0; i < neurons.size(); i++) {
            outputs[i] = neurons.get(i).activate(inputs);
        }

        if (useBatchNorm) {
            outputs = batchNormalizer.forward(outputs, isTraining);
        }

        if (isTraining) {
            // Itt most nem tárolunk batch adatokat, mert ez az egyes példányok forward pass-e
        }

        this.lastInputs = inputs;
        this.lastOutputs = outputs;

        return outputs;
    }

    public double[][] forwardBatch(double[][] inputs, boolean isTraining) {
        int batchSize = inputs.length;
        double[][] outputs = new double[batchSize][neurons.size()];

        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < neurons.size(); i++) {
                outputs[b][i] = neurons.get(i).activate(inputs[b]);
            }
        }

        if (useBatchNorm) {
            outputs = batchNormalizer.forwardBatch(outputs, isTraining);
        }

        if (isTraining) {
            this.batchInputs = inputs;
            this.batchOutputs = outputs;
        }

        return outputs;
    }

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
            for (int i = 0; i < outputSize; i++) {
                Neuron neuron = neurons.get(i);
                double neuronDelta = nextLayerDeltas[b][i];

                double derivativeValue = Activation.derivative(batchOutputs[b][i], activation);
                neuronDelta *= derivativeValue;

                double[] weights = neuron.getWeights();
                for (int j = 0; j < inputSize; j++) {
                    double gradientUpdate = neuronDelta * inputs[b][j];
                    weightGradients[i][j] += gradientUpdate;
                    inputGradients[b][j] += neuronDelta * weights[j];
                }
                biasGradients[i] += neuronDelta;
            }
        }

        // Átlagoljuk a gradienseket a batch méretével
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] /= batchSize;
            }
            biasGradients[i] /= batchSize;
        }

        // Frissítjük minden neuron súlyait és bias értékét
        for (int i = 0; i < outputSize; i++) {
            Neuron neuron = neurons.get(i);
            neuron.updateWeights(weightGradients[i], biasGradients[i], this.learningRate);
        }

        // Gradiens vágás alkalmazása az inputGradients-re
        for (int b = 0; b < batchSize; b++) {
            inputGradients[b] = gradientClipper.scaleAndClip(inputGradients[b]);
        }

        // Debug információ
        /*
        System.out.println("Layer " + name + " - Average weight gradient: " +
                Arrays.stream(weightGradients).flatMapToDouble(Arrays::stream).average().orElse(0.0));

         */

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
