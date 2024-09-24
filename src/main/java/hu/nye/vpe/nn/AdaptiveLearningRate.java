package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Adaptive learning class.
 */
public class AdaptiveLearningRate implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private double learningRate;
    private double momentum;
    private double previousFitnessDifference;
    private static final double INCREASE_FACTOR = 1.05;
    private static final double DECREASE_FACTOR = 0.95;
    private static final double MIN_LEARNING_RATE = 0.00001;
    private static final double MAX_LEARNING_RATE = 0.1;
    private static final double MIN_MOMENTUM = 0.1;
    private static final double MAX_MOMENTUM = 0.9;

    public AdaptiveLearningRate(double initialLearningRate, double initialMomentum) {
        this.learningRate = initialLearningRate;
        this.momentum = initialMomentum;
        this.previousFitnessDifference = 0;
    }

    /**
     * Update learning rate.
     *
     * @param currentFitness actual fitness
     * @param previousBestFitness previous best fitness
     *
     * @return new learning rate
     */
    public double updateLearningRate(double currentFitness, double previousBestFitness) {
        double fitnessDifference = currentFitness - previousBestFitness;

        if (fitnessDifference > previousFitnessDifference) {
            learningRate = Math.min(learningRate * INCREASE_FACTOR, MAX_LEARNING_RATE);
            momentum = Math.min(momentum * INCREASE_FACTOR, MAX_MOMENTUM);
        } else if (fitnessDifference < previousFitnessDifference) {
            learningRate = Math.max(learningRate * DECREASE_FACTOR, MIN_LEARNING_RATE);
            momentum = Math.max(momentum * DECREASE_FACTOR, MIN_MOMENTUM);
        }
        previousFitnessDifference = fitnessDifference;
        return learningRate;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getMomentum() {
        return momentum;
    }

    @Override
    public AdaptiveLearningRate clone() {
        try {
            return (AdaptiveLearningRate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}