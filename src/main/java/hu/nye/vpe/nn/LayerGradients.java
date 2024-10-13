package hu.nye.vpe.nn;

/**
 * Layergradients class.
 */
public class LayerGradients {
    public final double[] inputGradients;
    public final double[] gammaGradients;
    public final double[] betaGradients;

    public LayerGradients(double[] inputGradients, double[] gammaGradients, double[] betaGradients) {
        this.inputGradients = inputGradients;
        this.gammaGradients = gammaGradients;
        this.betaGradients = betaGradients;
    }
}
