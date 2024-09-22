package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Adaptive learning class.
 */
public class AdaptiveLearningRate implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private static final double MAX_LEARNING_RATE = 1.0;
    private static final double MIN_LEARNING_RATE = 0.0001;
    private static final double MAX_STAGNANT_EPOCH = 10;
        private double learningRate;
    private double momentum;
    private double previousDelta;
    private int stagnantEpochs;

    public AdaptiveLearningRate(double initialLearningRate, double initialMomentum) {
        this.learningRate = initialLearningRate;
        this.momentum = initialMomentum;
        this.previousDelta = 0;
        this.stagnantEpochs = 0;
    }

    /**
     * Update learning rate.
     *
     * @param currentFitness current fitness
     * @param previousFitness previous fitness
     *
     * @return learningRate
     */
    public double updateLearningRate(double currentFitness, double previousFitness) {
        double delta = currentFitness - previousFitness;
        if (delta > 0) {
            learningRate *= 0.95;
            stagnantEpochs = 0;
        } else {
            learningRate *= 1.05;
            stagnantEpochs++;
        }
        double momentumTerm = momentum * previousDelta;
        double newDelta = (1 - momentum) * delta + momentumTerm;
        if (stagnantEpochs > MAX_STAGNANT_EPOCH) {
            learningRate = Math.min(learningRate * 2, MAX_LEARNING_RATE);
            stagnantEpochs = 0;
        }
        previousDelta = newDelta;
        return Math.max(MIN_LEARNING_RATE, Math.min(learningRate, MAX_LEARNING_RATE));
    }

    public double getLearningRate() {
        return learningRate;
    }

    @Override
    public AdaptiveLearningRate clone() {
        try {
            return (AdaptiveLearningRate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}