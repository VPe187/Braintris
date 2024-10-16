package hu.nye.vpe.nn;

import java.util.Arrays;

/**
 * InputNormalizerZ class for z-score normalization.
 */
public class InputNormalizerZScore {
    private double mean;
    private double standardDeviation;
    private boolean isFitted = false;
    private final int arraySize;

    public InputNormalizerZScore(int arraySize) {
        this.arraySize = arraySize;
    }

    /***
     * Calculate mean and standard deviation for the values in the data array.
     *
     * @param data Data array
     */
    public void fit(double[] data) {
        if (data.length != arraySize) {
            throw new IllegalArgumentException("Data length does not match the expected array size.");
        }

        mean = Arrays.stream(data).average().orElse(0.0);
        double variance = Arrays.stream(data)
                .map(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        standardDeviation = Math.sqrt(variance);
        isFitted = true;
    }

    /***
     * Normalize data using z-score normalization.
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
            if (standardDeviation != 0) {
                normalizedData[i] = (value - mean) / standardDeviation;
            } else {
                normalizedData[i] = 0.0;
            }
        }

        return normalizedData;
    }

    /***
     * Method to fit and transform the data.
     *
     * @param data Data array
     *
     * @return transformed data
     */
    public double[] fitTransform(double[] data) {
        fit(data);
        return transform(data);
    }

    /***
     * Automatically normalize the data.
     *
     * @param data Data array.
     *
     * @return normalized data array.
     */
    public double[] normalizeAutomatically(double[] data) {
        return fitTransform(data);
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }
}
