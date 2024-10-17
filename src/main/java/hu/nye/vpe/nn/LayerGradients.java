package hu.nye.vpe.nn;

/**
 * LayerGradients class for batch processing.
 */
public class LayerGradients {
    public final double[][] inputGradients;  // Gradients for each input in the batch
    public final double[][] weightGradients; // Average weight gradients across the batch
    public final double[] biasGradients;     // Average bias gradients across the batch

    public LayerGradients(double[][] inputGradients, double[][] weightGradients, double[] biasGradients) {
        this.inputGradients = inputGradients;
        this.weightGradients = weightGradients;
        this.biasGradients = biasGradients;
    }
}