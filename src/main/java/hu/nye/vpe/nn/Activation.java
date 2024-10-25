package hu.nye.vpe.nn;

/**
 * Activation enum.
 */
public enum Activation {
    SIGMOID, TANH, RELU, LEAKY_RELU, ELU, GELU, LINEAR, SWISH, MISH, SOFTMAX, SOFTMAX_SPLIT;

    /**
     * Activate method.
     *
     * @param x input value or vector
     * @param type activation type
     * @return activated value or vector
     */
    public static double[] activate(double[] x, Activation type) {
        if (type == SOFTMAX) {
            return activateSoftMax(x);
        } else if (type == SOFTMAX_SPLIT) {
            throw new IllegalArgumentException("SOFTMAX_SPLIT requires a split index. Use activate(double[], Activation, int) instead.");
        } else {
            double[] result = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                result[i] = activateSingle(x[i], type);
            }
            return result;
        }
    }

    /**
     * Activate method for SOFTMAX_SPLIT.
     *
     * @param x input value or vector
     * @param type activation type
     * @param splitIndex index to split the input
     * @return activated value or vector
     */
    public static double[] activate(double[] x, Activation type, int splitIndex) {
        if (type != SOFTMAX_SPLIT) {
            return activate(x, type);
        }
        return activateSoftMaxSplit(x, splitIndex);
    }

    private static double activateSingle(double x, Activation type) {
        return switch (type) {
            case SIGMOID -> 1 / (1 + Math.exp(-x));
            case TANH -> Math.tanh(x);
            case RELU -> Math.max(0, x);
            case LEAKY_RELU -> x > 0 ? x : 0.01 * x;
            case ELU -> x > 0 ? x : 0.01 * (Math.exp(x) - 1);
            case GELU -> 0.5 * x * (1 + Math.tanh(Math.sqrt(2 / Math.PI) * (x + 0.044715 * Math.pow(x, 3))));
            case LINEAR -> x;
            case SWISH -> x * sigmoid(x);
            case MISH -> x * Math.tanh(Math.log(1 + Math.exp(x)));
            default -> throw new IllegalArgumentException("Unsupported activation type for single value: " + type);
        };
    }

    private static double[] activateSoftMax(double[] x) {
        double[] output = new double[x.length];
        double sum = 0.0;
        double max = x[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] > max) {
                max = x[i];
            }
        }
        for (int i = 0; i < x.length; i++) {
            output[i] = Math.exp(x[i] - max);
            sum += output[i];
        }
        for (int i = 0; i < x.length; i++) {
            output[i] /= sum;
        }
        return output;
    }

    private static double[] activateSoftMaxSplit(double[] x, int splitIndex) {
        if (splitIndex <= 0 || splitIndex >= x.length) {
            throw new IllegalArgumentException("Split index must be between 1 and " + (x.length - 1));
        }

        double[] output = new double[x.length];
        double[] firstPart = new double[splitIndex];
        double[] secondPart = new double[x.length - splitIndex];

        System.arraycopy(x, 0, firstPart, 0, splitIndex);
        System.arraycopy(x, splitIndex, secondPart, 0, x.length - splitIndex);

        double[] activatedFirst = activateSoftMax(firstPart);
        double[] activatedSecond = activateSoftMax(secondPart);

        System.arraycopy(activatedFirst, 0, output, 0, splitIndex);
        System.arraycopy(activatedSecond, 0, output, splitIndex, x.length - splitIndex);

        return output;
    }


    /**
     * Derivative method.
     *
     * @param x activated value or vector
     * @param type activation type
     * @return derivative value or vector
     */
    public static double[] derivative(double[] x, Activation type) {
        if (type == SOFTMAX) {
            return derivativeSoftMax(x);
        } else if (type == SOFTMAX_SPLIT) {
            throw new IllegalArgumentException("SOFTMAX_SPLIT requires a split index. Use derivative(double[], Activation, int) instead.");
        } else {
            double[] result = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                result[i] = derivativeSingle(x[i], type);
            }
            return result;
        }
    }

    /**
     * Derivative method for SOFTMAX_SPLIT.
     *
     * @param x activated value or vector
     * @param type activation type
     * @param splitIndex index to split the input
     * @return derivative value or vector
     */
    public static double[] derivative(double[] x, Activation type, int splitIndex) {
        if (type != SOFTMAX_SPLIT) {
            return derivative(x, type);
        }
        return derivativeSoftMaxSplit(x, splitIndex);
    }

    private static double derivativeSingle(double x, Activation type) {
        return switch (type) {
            case SIGMOID -> {
                double sigmoid = activateSingle(x, SIGMOID);
                yield sigmoid * (1 - sigmoid);
            }
            case TANH -> 1 - Math.pow(Math.tanh(x), 2);
            case RELU -> x > 0 ? 1 : 0;
            case LEAKY_RELU -> x > 0 ? 1 : 0.01;
            case ELU -> x > 0 ? 1 : 0.01 * Math.exp(x);
            case GELU -> {
                double x1 = Math.tanh(Math.sqrt(2 / Math.PI) * (x + 0.044715 * Math.pow(x, 3)));
                double cdf = 0.5 * (1 + x1);
                double pdf = Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
                yield cdf + x * pdf * (1 - Math.pow(x1, 2));
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
            default -> throw new IllegalArgumentException("Unsupported activation type for single value derivative: " + type);
        };
    }

    private static double[] derivativeSoftMax(double[] output) {
        double[] jacobian = new double[output.length * output.length];
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output.length; j++) {
                if (i == j) {
                    jacobian[i * output.length + j] = output[i] * (1 - output[i]);
                } else {
                    jacobian[i * output.length + j] = -output[i] * output[j];
                }
            }
        }
        return jacobian;
    }

    private static double[] derivativeSoftMaxSplit(double[] output, int splitIndex) {
        if (splitIndex <= 0 || splitIndex >= output.length) {
            throw new IllegalArgumentException("Split index must be between 1 and " + (output.length - 1));
        }

        double[] jacobian = new double[output.length * output.length];
        for (int i = 0; i < splitIndex; i++) {
            for (int j = 0; j < splitIndex; j++) {
                if (i == j) {
                    jacobian[i * output.length + j] = output[i] * (1 - output[i]);
                } else {
                    jacobian[i * output.length + j] = -output[i] * output[j];
                }
            }
        }

        for (int i = splitIndex; i < output.length; i++) {
            for (int j = splitIndex; j < output.length; j++) {
                if (i == j) {
                    jacobian[i * output.length + j] = output[i] * (1 - output[i]);
                } else {
                    jacobian[i * output.length + j] = -output[i] * output[j];
                }
            }
        }

        return jacobian;
    }

    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
}
