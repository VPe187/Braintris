package hu.nye.vpe.nn;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gradient clipper class for clipping and normalizing gradients.
 */
public class GradientClipper implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final double minValue;
    private final double maxValue;
    private final double clipNorm;
    private final double gradientScale;
    private static final double EPSILON = 1e-5;

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
        for (int i = 0; i < gradients.length; i++) {
            gradients[i] = clip(gradients[i]);
        }
        return gradients;
    }

    /**
     * Scale the gradients based on norm.
     *
     * @param gradients gradients array
     *
     * @return scaled gradients array
     */
    public double[] scaleGradients(double[] gradients) {
        double sumOfSquares = 0;
        for (double grad : gradients) {
            sumOfSquares += grad * grad;
        }
        double norm = Math.sqrt(sumOfSquares + EPSILON);
        double scale = Math.min(1.0, clipNorm / norm) * gradientScale;

        for (int i = 0; i < gradients.length; i++) {
            gradients[i] *= scale;
        }

        return gradients;
    }

    /**
     * Scaling and clipping gradients.
     *
     * @param gradients gradients array
     *
     * @return clipped and scaled gradients array.
     */
    public double[] scaleAndClip(double[] gradients) {
        gradients = scaleGradients(gradients);
        return clipByValue(gradients);
    }

}
