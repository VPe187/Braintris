package hu.nye.vpe.tetris;

import java.util.Stack;

/**
 * Tetromino pool class.
 */
public class TetrominoPool {
    private static final TetrominoPool instance = new TetrominoPool();
    private final Stack<Tetromino> availableTetrominos;

    private TetrominoPool() {
        availableTetrominos = new Stack<>();
    }

    public static TetrominoPool getInstance() {
        return instance;
    }

    /**
     * GetTetromino method.
     *
     * @return tetromino
     */
    public Tetromino getTetromino() {
        if (!availableTetrominos.isEmpty()) {
            return availableTetrominos.pop();
        }
        return null;
    }

    public void releaseTetromino(Tetromino tetromino) {
        availableTetrominos.push(tetromino);
    }
}
