package hu.nye.vpe.tetris;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import hu.nye.vpe.gaming.GameColorPalette;

/**
 * Tetromino factory class.
 */
public class TetrominoFactory {
    private static final int MAX_TETROMINO_WIDTH = 4;
    private static final int MAX_TEROMINO_HEIGHT = 4;
    private static final TetrominoFactory TETROMINO_FACTORY = new TetrominoFactory();
    private static final int tetrominoNum = 7;
    private final int[][] pixelsEmpty = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Tetromino tetrominoEmpty = new Tetromino(0, 0, Color.WHITE, 1, pixelsEmpty);
    private final int[][] pixelsErased = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Tetromino tetrominoErased = new Tetromino(99, 40, Color.BLACK, 1, pixelsErased);
    private final int[][] pixelsLoaded = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1} };
    private final Tetromino tetrominoLoaded = new Tetromino(90, 120, new Color(60, 60, 60), 1, pixelsLoaded);
    private final ArrayList<Tetromino> tetrominoArray = new ArrayList<Tetromino>();
    private static final GameColorPalette palette = GameColorPalette.getInstance();
    private static final Random rnd = new Random();
    private final TetrominoPool tetrominoPool = TetrominoPool.getInstance();

    private TetrominoFactory() {
        init();
    }

    public static TetrominoFactory getInstance() {
        return TETROMINO_FACTORY;
    }

    private void init() {
        tetrominoArray.add(new Tetromino(1, 10, Color.WHITE, 1, new int[][]{{1}, {1}, {1}, {1}})); // Tetromino I
        tetrominoArray.add(new Tetromino(2, 20, Color.WHITE, 2, new int[][]{{0, 1}, {0, 1}, {1, 1}})); // Tetromino J
        tetrominoArray.add(new Tetromino(3, 20, Color.WHITE, 2, new int[][]{{1, 0}, {1, 0}, {1, 1}})); // Tetromino L
        tetrominoArray.add(new Tetromino(4, 10, Color.WHITE, 2, new int[][]{{1, 1}, {1, 1}})); // Tetromino O
        tetrominoArray.add(new Tetromino(5, 30, Color.WHITE, 3, new int[][]{{1, 0}, {1, 1}, {0, 1}})); // Tetromino S
        tetrominoArray.add(new Tetromino(6, 20, Color.WHITE, 2, new int[][]{{1, 0}, {1, 1}, {1, 0}})); // Tetromino T
        tetrominoArray.add(new Tetromino(7, 30, Color.WHITE, 2, new int[][]{{0, 1}, {1, 1}, {1, 0}})); // Tetromino Z
    }

    /**
     * Get random tetromino.
     *
     * @return tetromino
     */
    public Tetromino getRandomTetromino(int tetrominoId) {
        Tetromino poolTetromino = tetrominoPool.getTetromino();
        if (poolTetromino != null) {
            return poolTetromino;
        }
        Tetromino randomTetromino;
        if (tetrominoId == -1) {
            int rsn = rnd.nextInt(tetrominoArray.size());
            randomTetromino = tetrominoArray.get(rsn);
        } else {
            randomTetromino = tetrominoArray.get(tetrominoId);
        }
        int[][] pixels = new int[randomTetromino.getPixels().length][randomTetromino.getPixels()[0].length];
        for (int a = 0; a < randomTetromino.getPixels().length; a++) {
            for (int b = 0; b < randomTetromino.getPixels()[a].length; b++) {
                pixels[a][b] = randomTetromino.getPixels()[a][b];
            }
        }
        Tetromino newTetromino = new Tetromino(
                randomTetromino.getId(),
                randomTetromino.getScore(),
                palette.getCurrentPalette()[randomTetromino.getId()],
                randomTetromino.getWeight(),
                pixels
        );
        int rn = rnd.nextInt(10);
        if (rn == 1) {
            putBonusToTile(newTetromino);
        }
        return newTetromino;
    }

    public void releaseTetromino(Tetromino tetromino) {
        tetrominoPool.releaseTetromino(tetromino);
    }

    /**
     * Get empty tetrimino.
     *
     * @return EmptyTetromino
     *
     */
    public Tetromino getEmptyTetromino() {
        return new Tetromino(
                tetrominoEmpty.getId(),
                tetrominoEmpty.getScore(),
                tetrominoEmpty.getColor(),
                tetrominoEmpty.getWeight(),
                tetrominoEmpty.getPixels()
        );
    }

    /**
     * Get erased tetromino.
     *
     * @return erased tetromino
     *
     */
    public Tetromino getErasedTetromino() {
        return new Tetromino(
                tetrominoErased.getId(),
                tetrominoErased.getScore(),
                tetrominoErased.getColor(),
                tetrominoErased.getWeight(),
                tetrominoErased.getPixels()
        );
    }

    /**
     * Get loaded tetromino.
     *
     * @return loaded tetromino
     *
     */
    public Tetromino getLoadedTetromino() {
        return new Tetromino(
                tetrominoLoaded.getId(),
                tetrominoLoaded.getScore(),
                tetrominoLoaded.getColor(),
                tetrominoLoaded.getWeight(),
                tetrominoLoaded.getPixels()
        );
    }

    private void putBonusToTile(Tetromino tetromino) {
        Random randomPixel = new Random();
        int bonusPixel = randomPixel.nextInt(tetromino.getPixelsNumber());
        int pixelCounter = 0;
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] == 1) {
                    pixelCounter++;
                    if (pixelCounter == bonusPixel) {
                        tetromino.setPixelBonus(i, j);
                    }
                }
            }
        }
    }

    /**
     * Convert tetromino to double array, where 1.0 pixel on, 0,0 pixel off.
     *
     * @param tetromino tetromino which convert to.
     *
     * @return double selected tetromino array
     */
    public double[] tetrominoToArray(Tetromino tetromino) {
        double[] output = new double[MAX_TETROMINO_WIDTH * MAX_TEROMINO_HEIGHT];
        int[][] pixels = tetromino.getPixels();

        for (int i = 0; i < MAX_TEROMINO_HEIGHT; i++) {
            for (int j = 0; j < MAX_TETROMINO_WIDTH; j++) {
                if (i < pixels.length && j < pixels[i].length) {
                    output[i * MAX_TETROMINO_WIDTH + j] = pixels[i][j] == 1 ? 1.0 : 0.0;
                } else {
                    output[i * MAX_TETROMINO_WIDTH + j] = 0.0;
                }
            }
        }

        return output;
    }
}
