package hu.nye.vpe;

/**
 * Main launcher.
 */
public class Launcher {

    public static void main(String[] args) {
        GameEngine game = new GameEngine("TetBrain - NYE - Varga Péter", 800, 780);
        game.start();
    }

}
