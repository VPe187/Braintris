package hu.nye.vpe.nn;

/**
 * LayerGradients class for batch processing.
 */
public class LayerGradients {
    public final double[][] inputGradients;
    public final double[][] weightGradients;
    public final double[] biasGradients;

    public LayerGradients(double[][] inputGradients, double[][] weightGradients, double[] biasGradients) {
        this.inputGradients = inputGradients;
        this.weightGradients = weightGradients;
        this.biasGradients = biasGradients;
    }
}