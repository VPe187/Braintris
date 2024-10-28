package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.List;

/**
 * Adam optimizer.
 */
public class AdamOptimizer implements Serializable {
    private final double learningRate;
    private final double beta1;
    private final double beta2;
    private final double epsilon;
    private double[][] mmean; // Első mozgóátlag a súlyokhoz
    private double[][] vmean; // Második mozgóátlag a súlyokhoz
    private double[] mbias; // Első mozgóátlag a bias-hoz
    private double[] vbias; // Második mozgóátlag a bias-hoz
    private int iter; // Iteráció számláló

    public AdamOptimizer(int outputSize, int inputSize, double learningRate, double beta1, double beta2, double epsilon) {
        this.learningRate = learningRate;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.epsilon = epsilon;
        this.mmean = new double[outputSize][inputSize];
        this.vmean = new double[outputSize][inputSize];
        this.mbias = new double[outputSize];
        this.vbias = new double[outputSize];
        this.iter = 0;
    }

    /**
     * Update weights.
     *
     * @param neurons Neurons.
     *
     * @param weightGradients Weight gradients.
     *
     * @param biasGradients Bias gradients.
     */
    public void updateWeights(List<Neuron> neurons, double[][] weightGradients, double[] biasGradients) {
        iter++;
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            double[] weightUpdates = new double[weightGradients[i].length];
            GradientClipper clipper = neuron.getGradientClipper();

            for (int j = 0; j < weightUpdates.length; j++) {
                mmean[i][j] = clipper.clip(beta1 * mmean[i][j] + (1 - beta1) * weightGradients[i][j]);
                vmean[i][j] = clipper.clip(beta2 * vmean[i][j] + (1 - beta2) * Math.pow(weightGradients[i][j], 2));

                double mhat = mmean[i][j] / (1 - Math.pow(beta1, iter));
                double vhat = vmean[i][j] / (1 - Math.pow(beta2, iter));

                weightUpdates[j] = learningRate * mhat / (Math.sqrt(vhat) + epsilon);
            }

            // Bias update számítása
            mbias[i] = clipper.clip(beta1 * mbias[i] + (1 - beta1) * biasGradients[i]);
            vbias[i] = clipper.clip(beta2 * vbias[i] + (1 - beta2) * Math.pow(biasGradients[i], 2));

            double mhatBias = mbias[i] / (1 - Math.pow(beta1, iter));
            double vhatBias = vbias[i] / (1 - Math.pow(beta2, iter));
            double biasUpdate = learningRate * mhatBias / (Math.sqrt(vhatBias) + epsilon);

            neuron.updateWeightsWithAdam(weightUpdates, biasUpdate);
        }
    }
}
