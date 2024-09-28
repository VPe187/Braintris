package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Adaptive learning class.
 */
public class AdaptiveLearningRate implements Serializable, Cloneable {
    //private static final double MAX_LEARNING_RATE = 0.1;
    private static final double MAX_LEARNING_RATE = 0.1;
    private static final double MIN_LEARNING_RATE = 0.00001;
    private static final double INCREASE_FACTOR = 1.05;
    private static final double DECREASE_FACTOR = 0.95;
    private static final int PATIENCE = 10;
    private double learningRate;
    private final double baseLearningRate;
    private final double improvementThreshold;
    private int patienceCounter;
    private double lastBestFittness;
    private final double momentum;
    private double velocity;


    public AdaptiveLearningRate(double initialLearningRate, double improvementThreshold, double initialMomentum) {
        this.baseLearningRate = initialLearningRate;
        this.learningRate = initialLearningRate;
        this.improvementThreshold = improvementThreshold;
        this.momentum = initialMomentum;
        this.patienceCounter = 0;
        this.lastBestFittness = Double.NEGATIVE_INFINITY;
        this.velocity = 0.0;
    }

    /**
     * Update learning rate.
     *
     * @param currentFitness actual fitness
     * @param gradient gradient
     */
    public void updateLearningRate(double currentFitness, double gradient) {
        if (currentFitness > lastBestFittness + improvementThreshold) {
            // Javulás történt
            lastBestFittness = currentFitness;
            patienceCounter = 0; // Reset patience counter
            // Növeljük a tanulási rátát, de ne lépjük túl a max értéket
            learningRate = Math.min(learningRate * INCREASE_FACTOR, MAX_LEARNING_RATE);
        } else {
            // Nincs javulás
            patienceCounter++;
            if (patienceCounter >= PATIENCE) {
                // Csökkentsük a tanulási rátát, ha nincs javulás egy ideig
                learningRate = Math.max(learningRate * DECREASE_FACTOR, MIN_LEARNING_RATE);
                patienceCounter = 0; // Reset patience counter
            }
        }
        // Momentum alkalmazása a súlyfrissítéseknél
        velocity = momentum * velocity - learningRate * gradient; // Aktualizált sebesség
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getMomentum() {
        return momentum;
    }

    public double getVelocity() {
        return velocity;
    }

    /***
     * Reset learning rate.
     */
    public void resetLearningRate() {
        this.learningRate = baseLearningRate;
        this.velocity = 0.0; // Reset momentum
        this.patienceCounter = 0;
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