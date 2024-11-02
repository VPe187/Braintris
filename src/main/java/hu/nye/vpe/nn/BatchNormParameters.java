package hu.nye.vpe.nn;

/**
 * Batch normalizer parameters class.
 */
public class BatchNormParameters {
    public final boolean useBatchNorm;
    public final double gamma;
    public final double beta;

    public BatchNormParameters(boolean useBatchNorm, double gamma, double beta) {
        this.useBatchNorm = useBatchNorm;
        this.gamma = gamma;
        this.beta = beta;
    }
}