package hu.nye.vpe;

import java.awt.Graphics2D;

/**
 * State class.
 */
public abstract class State {
    private static State currentState = null;
    protected Handler handler;

    public static void setState(State state) {
        currentState = state;
    }

    public static State getState() {
        return currentState;
    }

    public State(Handler handler) {
        this.handler = handler;
    }

    public abstract void tick();

    public abstract void render(Graphics2D g);
}