package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.Arrays;
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

        double beta1Correction = 1.0 / (1.0 - Math.pow(beta1, iter));
        double beta2Correction = 1.0 / (1.0 - Math.pow(beta2, iter));

        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            double[] weightUpdates = new double[weightGradients[i].length];
            GradientClipper clipper = neuron.getGradientClipper();

            // Súlyok frissítése
            for (int j = 0; j < weightUpdates.length; j++) {
                double clippedGradient = clipper.clip(weightGradients[i][j]);

                // Momentum update
                mmean[i][j] = beta1 * mmean[i][j] + (1.0 - beta1) * clippedGradient;
                // RMSprop update
                vmean[i][j] = beta2 * vmean[i][j] + (1.0 - beta2) * clippedGradient * clippedGradient;
                // Bias correction
                double mhat = mmean[i][j] * beta1Correction;
                double vhat = vmean[i][j] * beta2Correction;

                if (Double.isNaN(mhat) || Double.isInfinite(mhat)) {
                    mhat = 0.0;
                }
                if (Double.isNaN(vhat) || Double.isInfinite(vhat)) {
                    vhat = epsilon;
                }

                // Súlyok átírása
                weightUpdates[j] = clipper.clip(learningRate * mhat / (Math.sqrt(vhat) + epsilon));
            }

            // Bias update számítása
            mbias[i] = beta1 * mbias[i] + (1 - beta1) * biasGradients[i];
            vbias[i] = beta2 * vbias[i] + (1 - beta2) * (biasGradients[i] * biasGradients[i]);

            double mhatBias = mbias[i] * beta1Correction;
            double vhatBias = vbias[i] * beta2Correction;

            if (Double.isNaN(mhatBias) || Double.isInfinite(mhatBias)) {
                mhatBias = 0.0;
            }
            if (Double.isNaN(vhatBias) || Double.isInfinite(vhatBias)) {
                vhatBias = epsilon;
            }

            //double biasUpdate = clipper.clip(learningRate * mhatBias / (Math.sqrt(vhatBias) + epsilon));
            double biasUpdate = clipper.clip(learningRate * mhatBias / (Math.sqrt(Math.max(vhatBias, epsilon)) + epsilon));

            neuron.updateWeightsWithAdam(weightUpdates, biasUpdate);
        }
    }
}
