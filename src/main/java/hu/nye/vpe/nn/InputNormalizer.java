package hu.nye.vpe.nn;

import java.util.Arrays;

public class InputNormalizer {
    private double min;
    private double max;
    private boolean isFitted = false;
    private final int arraySize;

    // Konstruktor, amely beállítja a tömb méretét
    public InputNormalizer(int arraySize) {
        this.arraySize = arraySize;
    }

    // A min és max értékek meghatározása csak azokra az értékekre, amelyek 1-nél nagyobbak
    public void fit(double[] data) {
        if (data.length != arraySize) {
            throw new IllegalArgumentException("Data length does not match the expected array size.");
        }

        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;

        for (double value : data) {
            if (value > 1) {
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
            }
        }
        isFitted = true;
    }

    // Normalizálás, amely a 1-nél nagyobb értékeket 0 és 1 közé alakítja
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
            if (value > 1) {
                if (max != min) {
                    normalizedData[i] = (value - min) / (max - min); // Normalizálás 0 és 1 közé
                } else {
                    normalizedData[i] = 0.0; // Ha max és min azonos, minden érték 0-ra normalizálódik
                }
            } else {
                normalizedData[i] = value; // 1-nél kisebb vagy egyenlő értékek nem változnak
            }
        }
        return normalizedData;
    }

    // Kényelmi metódus, amely egy lépésben elvégzi a fit és transform műveletet
    public double[] fitTransform(double[] data) {
        fit(data);
        return transform(data);
    }

    // Új metódus, amely automatikusan végigmegy és normalizálja az adatokat
    public double[] normalizeAutomatically(double[] data) {
        return fitTransform(data);
    }

    public static void main(String[] args) {
        // Konstruktor, amely beállítja a tömb méretét
        InputNormalizer normalizer = new InputNormalizer(8);

        // Példa adat
        double[] data = { 0, 1, 0.5, 2, 3, 0.8, 4, 5 };

        // Automatikus normalizálás
        double[] normalizedData = normalizer.normalizeAutomatically(data);

        // Normalizált adatok kiírása
        System.out.println("Normalized Data: " + Arrays.toString(normalizedData));
    }
}
