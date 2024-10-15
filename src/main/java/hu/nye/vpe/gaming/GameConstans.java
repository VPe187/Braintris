package hu.nye.vpe.gaming;

import java.awt.Color;

/**
 * Game constant class.
 */
public class GameConstans {
    public static final String FONT_NAME = "Truly Madly Dpad";
    public static final int COLS = 12;
    public static final int ROWS = 24;
    public static final int BLOCK_SIZE = 30;
    public static final int ROW_OFFSET = 2;
    public static final int STACK_X = 2 * BLOCK_SIZE;
    public static final int STACK_Y = 2 * BLOCK_SIZE;
    public static final int STACK_W = COLS * BLOCK_SIZE;
    public static final int STACK_H = (ROWS - ROW_OFFSET) * BLOCK_SIZE;
    public static final int PENALTY_NO_FULL_ROW = 12;
    public static final int BONUS_COLOR_ALPHA = 200;
    public static final int LEVEL_CHANGE_ROWS = 4;
    public static final long START_SPEED = 1000L;
    public static final long LEARNING_START_SPEED = 20L;
    public static final int LEVEL_BONUS = 20;
    public static final int ROW_SCORE = 100;
    public static final long SPEED_ACCELERATION = 50L;
    public static final Color PANEL_COLOR = new Color(30, 30, 30, 100);
    public static final Color COLOR_STACK_BACKGROUND = new Color(15, 15, 15);
    public static final Color COLOR_STACK_BORDER = new Color(100, 100, 100, 100);
    public static final Color COLOR_HELPER = new Color(30, 30, 30, 40);
    public static final Color COLOR_HELPER_LINE = new Color(40, 40, 40, 40);
}
