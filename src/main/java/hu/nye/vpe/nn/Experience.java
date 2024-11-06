package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Experience play class.
 */
public class Experience implements Serializable {
    public final double[] state;
    public final int[] action;
    public final double reward;
    public final double[] nextState;
    public final double[][] nextPossibleStates;
    public final boolean done;
    public double priority;

    public Experience(double[] state, int[] action, double reward, double[] nextState, double[][] nextPossibleStates,
                      boolean done) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.nextPossibleStates = nextPossibleStates;
        this.done = done;
        this.priority = 1.0;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }
}
