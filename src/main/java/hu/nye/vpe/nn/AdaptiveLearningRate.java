package hu.nye.vpe.nn;

import java.io.Serializable;

public class AdaptiveLearningRate implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private double learningRate;
    private double momentum;
    private double previousDelta;

    public AdaptiveLearningRate(double initialLearningRate, double initialMomentum) {
        this.learningRate = initialLearningRate;
        this.momentum = initialMomentum;
        this.previousDelta = 0;
    }

    public double updateLearningRate(double currentFitness, double previousFitness) {
        double delta = currentFitness - previousFitness;

        if (delta > 0) {
            learningRate *= 0.95;  // Increase learning rate if improving
        } else {
            learningRate *= 1.05;  // Decrease learning rate if not improving
        }

        // Apply momentum
        double momentumTerm = momentum * previousDelta;
        double newDelta = (1 - momentum) * delta + momentumTerm;

        previousDelta = newDelta;

        return Math.max(0.0001, Math.min(learningRate, 1.0));  // Keep learning rate in reasonable bounds
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