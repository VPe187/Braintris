package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Gradient clipper task.
 */
public class GradientClipper implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double minValue;
    private final double maxValue;
    private final double clipNorm;
    private final double gradientScale;
    private final double epsilon = 1e-8;

    public GradientClipper(double minValue, double maxValue, double clipNorm, double gradientScale) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.clipNorm = clipNorm;
        this.gradientScale = gradientScale;
    }

    public double clip(double gradient) {
        double scaledGradient = gradient * gradientScale;
        return Math.max(minValue, Math.min(maxValue, scaledGradient));
    }

    /**
     * Clip by value.
     *
     * @param gradients Gradients array
     *
     * @return clipped gradients array
     */
    public double[] clipByValue(double[] gradients) {
        double[] clippedGradients = new double[gradients.length];
        for (int i = 0; i < gradients.length; i++) {
            clippedGradients[i] = clip(gradients[i]);
        }
        return clippedGradients;
    }

    /**
     * Clip by norm.
     *
     * @param gradients Gradients array
     *
     * @return clipped gradients array
     */
    public double[] clipByNorm(double[] gradients) {
        double norm = 0;
        for (double grad : gradients) {
            norm += grad * grad;
        }
        norm = Math.sqrt(norm);

        if (norm > clipNorm) {
            double scale = clipNorm / norm;
            for (int i = 0; i < gradients.length; i++) {
                gradients[i] *= scale;
            }
        }
        return gradients;
    }

    /**
     * Scaling and clipping cradients.
     *
     * @param gradients gradients array
     *
     * @return clipped and scaled gradients array.
     */
    public double[] scaleAndClip(double[] gradients) {
        double[] scaledGradients = new double[gradients.length];
        for (int i = 0; i < gradients.length; i++) {
            scaledGradients[i] = gradients[i] * gradientScale;
        }
        clipByNorm(scaledGradients);
        return clipByValue(scaledGradients);
    }
}
