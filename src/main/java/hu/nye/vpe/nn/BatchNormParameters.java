package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Batch normalizer parameters class.
 */
public class BatchNormParameters implements Serializable {
    public final boolean useBatchNorm;
    public final double gamma;
    public final double beta;

    public BatchNormParameters(boolean useBatchNorm, double gamma, double beta) {
        this.useBatchNorm = useBatchNorm;
        this.gamma = gamma;
        this.beta = beta;
    }
}