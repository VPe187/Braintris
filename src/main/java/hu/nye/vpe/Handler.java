package hu.nye.vpe;

/**
 * Handler class.
 */
public class Handler {
    private Game game;

    public Handler(Game game) {
        this.game = game;
    }

    public int getWidth() {
        return game.width;
    }

    public int getHeight() {
        return game.height;
    }

    public Game getGame() {
        return game;
    }
}
