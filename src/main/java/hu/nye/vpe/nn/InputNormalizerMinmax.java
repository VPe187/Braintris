package hu.nye.vpe.nn;

/**
 * Inputnormalizer class.
 */
public class InputNormalizerMinmax {
    private double min;
    private double max;
    private boolean isFitted = false;
    private final int arraySize;

    public InputNormalizerMinmax(int arraySize) {
        this.arraySize = arraySize;
    }

    /***
     * Define min and max for values wich bigger than 1.
     *
     * @param data Data array
     */
    public void fit(double[] data) {
        if (data.length != arraySize) {
            throw new IllegalArgumentException("Data length does not match the expected array size.");
        }

        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        for (double value : data) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
            //if (value > 1) {
            //}
        }

        isFitted = true;
    }

    /***
     * Normalize data, all value which bigger than 1 converted to 0-1 intervall.
     *
     * @param data Data array.
     *
     * @return normalized data
     */
    public double[] transform(double[] data) {
        if (!isFitted) {
            throw new IllegalStateException("Fit the data before transforming.");
        }
        if (data.length != arraySize) {
            throw new IllegalArgumentException("Data length does not match the expected array size.");
        }

        double[] normalizedData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            double value = data[i];
            if (max != min) {
                normalizedData[i] = (value - min) / (max - min);
            } else {
                normalizedData[i] = 0.0;
            }
            //if (value > 1) {
            //} else {
            //    normalizedData[i] = value;
            //}
        }

        return normalizedData;
    }

    /***
     * Method make fit and transform.
     *
     * @param data Data array
     *
     * @return transformet data.
     */
    public double[] fitTransform(double[] data) {
        fit(data);
        return transform(data);
    }

    /***
     * Automaticly normalization.
     *
     * @param data Data array.
     *
     * @return normalized data array.
     */
    public double[] normalizeAutomatically(double[] data) {
        return fitTransform(data);
    }
}
