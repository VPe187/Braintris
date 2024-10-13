package hu.nye.vpe.nn;

import java.io.Serializable;

/**
 * Experience play class.
 */
public class Experience implements Serializable {
    public final double[] state;
    public final int action;
    public final double reward;
    public final double[] nextState;
    public final boolean done;

    public Experience(double[] state, int action, double reward, double[] nextState, boolean done) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }
}
