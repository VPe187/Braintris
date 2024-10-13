package hu.nye.vpe.nn;

/**
 * Activation enum.
 */
public enum Activation {
    SIGMOID, TANH, RELU, LEAKY_RELU, ELU, GELU, LINEAR, SWISH, MISH;

    /**
     * Activate method.
     *
     * @param x neuron
     *
     * @param type actviation type
     *
     * @return activated value
     */
    public static double activate(double x, Activation type) {
        return switch (type) {
            case SIGMOID -> 1 / (1 + Math.exp(-x));
            //case TANH -> Math.tanh(x / 5);
            case TANH -> Math.tanh(x);
            case RELU -> Math.max(0, x);
            case LEAKY_RELU -> x > 0 ? x : 0.01 * x;
            case ELU -> x > 0 ? x : 0.01 * (Math.exp(x) - 1);
            case GELU -> 0.5 * x * (1 + Math.tanh(Math.sqrt(2 / Math.PI) * (x + 0.044715 * Math.pow(x, 3))));
            case LINEAR -> x;
            case SWISH -> x * sigmoid(x);
            case MISH -> x * Math.tanh(Math.log(1 + Math.exp(x)));
        };
    }

    /**
     * Derivation method.
     *
     * @param x neuron value
     *
     * @param type activation type
     *
     * @return derivated value
     */
    public static double derivative(double x, Activation type) {
        return switch (type) {
            case SIGMOID -> {
                double sigmoid = activate(x, SIGMOID);
                yield sigmoid * (1 - sigmoid);
            }
            //case TANH -> 1 - Math.pow(Math.tanh(x / 5), 2);
            case TANH -> 1 - Math.pow(Math.tanh(x), 2);
            case RELU -> x > 0 ? 1 : 0;
            case LEAKY_RELU -> x > 0 ? 1 : 0.01;
            case ELU -> x > 0 ? 1 : 0.01 * Math.exp(x);
            case GELU -> {
                double cdf = 0.5 * (1 + Math.tanh(Math.sqrt(2 / Math.PI) * (x + 0.044715 * Math.pow(x, 3))));
                double pdf = Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
                yield cdf + x * pdf * (1 - Math.pow(Math.tanh(Math.sqrt(2 / Math.PI) * (x + 0.044715 * Math.pow(x, 3))), 2));
            }
            case LINEAR -> 1;
            case MISH -> {
                double sp = sigmoid(x);
                double tanhSoftPlus = Math.tanh(Math.log(1 + Math.exp(x)));
                yield tanhSoftPlus + x * sp * (1 - tanhSoftPlus * tanhSoftPlus);
            }
            case SWISH -> {
                double sigmoid = sigmoid(x);
                yield sigmoid + x * sigmoid * (1 - sigmoid);
            }
        };
    }

    // Szigmoid segédfüggvény
    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
}
