package hu.nye.vpe;

import hu.nye.vpe.gaming.GameEngine;

/**
 * Main launcher.
 */
public class Launcher {

    public static final boolean LEARNING = false;

    public static void main(String[] args) {
        GameEngine game = new GameEngine("TetBrain - NYE - Varga Péter", 690 * (LEARNING ? 2 : 1), 780, LEARNING);
        game.start();
    }

}
