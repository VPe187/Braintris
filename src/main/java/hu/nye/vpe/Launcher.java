package hu.nye.vpe;

import hu.nye.vpe.gaming.GameEngine;
import hu.nye.vpe.tetris.RunMode;

/**
 * Main launcher.
 */
public class Launcher {

    public static final RunMode RUN_MODE = GlobalConfig.getInstance().getRunMode();

    public static void main(String[] args) {
        GameEngine game = new GameEngine("TetBrain - NYE - Varga Péter", 690 * (RUN_MODE == RunMode.TRAIN_AI ? 2 : 1), 780, RUN_MODE);
        game.start();
    }

}
