package hu.nye.vpe.tetris;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameConstans;
import hu.nye.vpe.gaming.GameState;

/**
 * Stack manager class.
 */
public class StackManager implements StackComponent {
    private Cell[][] stackArea = new Cell[GameConstans.ROWS][GameConstans.COLS];
    private Cell[][] simStackArea = new Cell[GameConstans.ROWS][GameConstans.COLS];
    private boolean learning;
    private Tetromino currentTetromino;
    private Tetromino nextTetromino;
    private GameState gameState;
    private static int iteration;
    private double tetrominoRotation;
    private StackMetrics metrics;
    private int noFullRows;
    private int allFullRows;
    private int gameScore;
    private int gameLevel = 0;
    private long currentSpeed;
    private final GameAudio audio = new GameAudio();
    private long startTime;
    private int droppedElements;

    public StackManager(boolean learning) {
        this.learning = learning;
        iteration = 0;
    }

    protected void start() {
        initializeGameState();
    }

    private void initializeGameState() {
        initializeStack();
        gameScore = 0;
        gameLevel = 0;
        noFullRows = 0;
        allFullRows = 0;
        droppedElements = 0;
        gameState = GameState.RUNNING;
        startTime = System.currentTimeMillis();
        currentSpeed = learning ? GameConstans.LEARNING_START_SPEED : GameConstans.START_SPEED;
    }

    private void initializeStack() {
        if (stackArea == null) {
            stackArea = new Cell[GameConstans.ROWS][GameConstans.COLS];
        }
        for (int i = 0; i < GameConstans.ROWS; i++) {
            for (int j = 0; j < GameConstans.COLS; j++) {
                stackArea[i][j] = new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                        TetrominoFactory.getInstance().getEmptyTetromino().getColor());
                simStackArea[i][j] = new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                        TetrominoFactory.getInstance().getEmptyTetromino().getColor());
            }
        }
    }

    /**
     * Simulated stack drop.
     */
    public void simulateStackDrop() {
        if (currentTetromino == null) {
            return;
        }

        for (int i = 0; i < GameConstans.ROWS; i++) {
            System.arraycopy(stackArea[i], 0, simStackArea[i], 0, GameConstans.COLS);
        }

        Tetromino simTetromino = copyTetromino(currentTetromino);


        while (!isTetrominoDown(simStackArea, simTetromino)) {
            removeTetromino(simStackArea, simTetromino);
            simTetromino.setRowPosition(simTetromino.getStackRow() + 1);
            putTetromino(simStackArea, simTetromino);
        }
    }

    private Tetromino copyTetromino(Tetromino original) {
        Tetromino copy = new Tetromino(
                original.getId(),
                original.getScore(),
                original.getColor(),
                original.getWeight(),
                original.getPixels()
        );
        copy.setColPosition(original.getStackCol());
        copy.setRowPosition(original.getStackRow());
        return copy;
    }

    /**
     * putTetromino method.
     */
    public void putTetromino(Cell[][] stackArea, Tetromino tetromino) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    stackArea[tetromino.getStackRow() + i][tetromino.getStackCol() + j] =
                            new Cell(tetromino.getId(), tetromino.getColor());
                    if (tetromino.getPixels()[i][j] == 2) {
                        stackArea[tetromino.getStackRow() + i][tetromino.getStackCol() + j].setBonus(BonusType.BOMB);
                    } else {
                        stackArea[tetromino.getStackRow() + i][tetromino.getStackCol() + j].setBonus(BonusType.NONE);
                    }
                }
            }
        }
    }

    /**
     * removeTetromino method.
     */
    protected void removeTetromino(Cell[][] stackArea, Tetromino tetromino) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    stackArea[tetromino.getStackRow() + i][tetromino.getStackCol() + j] =
                            new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                                    TetrominoFactory.getInstance().getEmptyTetromino().getColor());
                }
            }
        }
    }

    private boolean isTetrominoDown(Cell[][] stackArea, Tetromino tetromino) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    if (isBottomCollision(stackArea, tetromino, i) || isDownCollision(stackArea, tetromino, i, j)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBottomCollision(Cell[][] stackArea, Tetromino tetromino, int i) {
        return tetromino.getStackRow() + i + 1 > stackArea.length - 1;
    }

    private boolean isDownCollision(Cell[][] stackArea, Tetromino tetromino, int i, int j) {
        int nextRow = tetromino.getStackRow() + i + 1;
        int col = tetromino.getStackCol() + j;
        return col <= stackArea[0].length - 1 &&
                nextRow <= stackArea.length - 1 &&
                stackArea[nextRow][col].getTetrominoId() != 0 &&
                !isPartOfCurrentTetromino(tetromino, nextRow, col);
    }

    private boolean isPartOfCurrentTetromino(Tetromino tetromino, int row, int col) {
        int localRow = row - tetromino.getStackRow();
        int localCol = col - tetromino.getStackCol();
        return localRow >= 0 && localRow < tetromino.getPixels().length &&
                localCol >= 0 && localCol < tetromino.getPixels()[0].length &&
                tetromino.getPixels()[localRow][localCol] != 0;
    }

    /**
     * moveTetrominoDown method.
     */
    protected boolean moveTetrominoDown(Cell[][] stackArea, Tetromino tetromino) {
        if (!isTetrominoDown(stackArea, tetromino)) {
            removeTetromino(stackArea, tetromino);
            tetromino.setRowPosition(tetromino.getStackRow() + 1);
            putTetromino(stackArea, tetromino);
            simulateStackDrop();
            return false;
        } else {
            itemFalled(stackArea, tetromino);
            return true;
        }
    }

    private boolean checkTetrominoIsLeft(Cell[][] stackArea, Tetromino tetromino) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    int newCol = tetromino.getStackCol() - 1 + j;
                    int row = tetromino.getStackRow() + i;
                    if (newCol < 0) {
                        return true;
                    }
                    if (row >= 0 && row < stackArea.length && newCol < stackArea[0].length) {
                        if (stackArea[row][newCol].getTetrominoId() != 0 &&
                                !isPartOfCurrentTetromino(tetromino, row, newCol)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move tetromino left.
     */
    protected boolean moveTetrominoLeft(Cell[][] stackArea, Tetromino tetromino) {
        if (!checkTetrominoIsLeft(stackArea, tetromino)) {
            removeTetromino(stackArea, tetromino);
            tetromino.setColPosition(tetromino.getStackCol() - 1);
            putTetromino(stackArea, tetromino);
            simulateStackDrop();
            return true;
        } else {
            return false;
        }
    }

    private boolean checkTetrominoIsRight(Cell[][] stackArea, Tetromino tetromino) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    int newCol = tetromino.getStackCol() + 1 + j;
                    int row = tetromino.getStackRow() + i;
                    if (newCol >= stackArea[0].length) {
                        return true;
                    }
                    if (row >= 0 && row < stackArea.length) {
                        if (stackArea[row][newCol].getTetrominoId() != 0 &&
                                !isPartOfCurrentTetromino(tetromino, row, newCol)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move tetromino right.
     */
    public boolean moveTetrominoRight(Cell[][] stackArea, Tetromino tetromino) {
        if (!checkTetrominoIsRight(stackArea, tetromino)) {
            removeTetromino(stackArea, tetromino);
            tetromino.setColPosition(tetromino.getStackCol() + 1);
            putTetromino(stackArea, tetromino);
            simulateStackDrop();
            return true;
        } else {
            return false;
        }
    }

    private boolean checkTetrominoCanRotateRight(Cell[][] stackArea, Tetromino tetromino) {
        int[][] rotatedPixels = getRotatedPixelsRight(tetromino.getPixels());
        return checkRotatedTetrominoPosition(stackArea, tetromino, rotatedPixels);
    }

    private boolean checkTetrominoCanRotateLeft(Cell[][] stackArea, Tetromino tetromino) {
        int[][] rotatedPixels = getRotatedPixelsLeft(tetromino.getPixels());
        return checkRotatedTetrominoPosition(stackArea, tetromino, rotatedPixels);
    }

    private boolean checkRotatedTetrominoPosition(Cell[][] stackArea, Tetromino tetromino, int[][] rotatedPixels) {
        int x = rotatedPixels.length;
        int y = rotatedPixels[0].length;

        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                if (rotatedPixels[i][j] != 0) {
                    int newRow = tetromino.getStackRow() + i;
                    int newCol = tetromino.getStackCol() + j;
                    if (newRow < 0 || newRow >= stackArea.length ||
                            newCol < 0 || newCol >= stackArea[0].length) {
                        return false;
                    }
                    if (stackArea[newRow][newCol].getTetrominoId() != 0 &&
                            !isPartOfCurrentTetromino(tetromino, newRow, newCol)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int[][] getRotatedPixelsRight(int[][] pixels) {
        int n = pixels.length;
        int m = pixels[0].length;
        int[][] rotated = new int[m][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                rotated[j][n - 1 - i] = pixels[i][j];
            }
        }
        return rotated;
    }

    private int[][] getRotatedPixelsLeft(int[][] pixels) {
        int n = pixels.length;
        int m = pixels[0].length;
        int[][] rotated = new int[m][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                rotated[m - 1 - j][i] = pixels[i][j];
            }
        }
        return rotated;
    }


    /**
     * Rotate tetromino (right).
     */
    protected boolean rotateTetrominoRight(Cell[][] stackArea, Tetromino tetromino) {
        if (checkTetrominoCanRotateRight(stackArea, tetromino)) {
            removeTetromino(stackArea, tetromino);
            tetromino.rotateRight();
            tetrominoRotation = (tetrominoRotation + 90) % 360;
            putTetromino(stackArea, tetromino);
            simulateStackDrop();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Rotate tetromino (left).
     */
    public void rotateTetrominoLeft(Cell[][] stackArea, Tetromino tetromino) {
        if (checkTetrominoCanRotateLeft(stackArea, tetromino)) {
            removeTetromino(stackArea, tetromino);
            tetromino.rotateLeft();
            tetrominoRotation = (tetrominoRotation + 270) % 360; // 270 fokkal való forgatás megegyezik a -90 fokkal
            putTetromino(stackArea, tetromino);
            simulateStackDrop();
        }
    }

    /**
     * Return number of full rows.
     *
     * @return int fullRowNum
     */
    protected int getFullRowsNum() {
        int fullRowNum = 0;
        boolean isFull;
        for (Cell[] cells : stackArea) {
            isFull = true;
            for (Cell cell : cells) {
                if (cell.getTetrominoId() == 0) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                fullRowNum++;
            }
        }
        return fullRowNum;
    }

    protected void insertPixel(int x, int y, boolean on) {
        if (x >= 1 && x < stackArea.length + 1 && y >= 1 && y <= stackArea[0].length + 1) {
            stackArea[(x - 1) + GameConstans.ROW_OFFSET][y - 1] =
                    (on ? new Cell(TetrominoFactory.getInstance().getLoadedTetromino().getId(),
                    TetrominoFactory.getInstance().getLoadedTetromino().getColor()) :
                    new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                            TetrominoFactory.getInstance().getEmptyTetromino().getColor()));
        }
    }

    private void generatePenaltyRows(int rowNum) {
        for (int i = GameConstans.ROW_OFFSET; i < stackArea.length; i++) {
            System.arraycopy(stackArea[i], 0, stackArea[i - 1], 0, stackArea[i].length);
        }
        Random rnd = new Random();
        int rn;
        for (int w = GameConstans.ROWS - GameConstans.ROW_OFFSET; w >= GameConstans.ROWS - (rowNum + 1); w--) {
            for (int h = 1; h <= GameConstans.COLS; h++) {
                rn = rnd.nextInt(2);
                insertPixel(w, h, rn == 1);
            }
        }
        noFullRows = 0;
    }

    private void generateEasyRows(int rowNum) {
        for (int i = GameConstans.ROW_OFFSET; i < stackArea.length; i++) {
            System.arraycopy(stackArea[i], 0, stackArea[i - 1], 0, stackArea[i].length);
        }
        Random rnd = new Random();
        int rn;
        for (int w = GameConstans.ROWS - GameConstans.ROW_OFFSET; w >= GameConstans.ROWS - (rowNum + 1); w--) {
            for (int h = 1; h <= GameConstans.COLS; h++) {
                if (h >= 3) {
                    insertPixel(w, h, true);
                }
            }
        }

        for (int w = GameConstans.ROWS - GameConstans.ROW_OFFSET; w >= GameConstans.ROWS - (rowNum + 2); w--) {
            for (int h = 1; h <= GameConstans.COLS; h++) {
                if (h >= 5) {
                    insertPixel(w, h, true);
                }
            }
        }

    }

    protected void checkPenalty() {
        if (noFullRows >= GameConstans.PENALTY_NO_FULL_ROW) {
            if (!learning) {
                audio.soundPenalty();
            }
            if (gameState != GameState.GAMEOVER) {
                if (!learning) {
                    generatePenaltyRows(1); // Tanításhoz kikapcsolva
                }
                noFullRows = 0;
            }
        }
    }

    protected void nextLevel() {
        allFullRows = 0;
        if (currentSpeed >= GameConstans.SPEED_ACCELERATION) {
            currentSpeed -= GameConstans.SPEED_ACCELERATION;
        }
        gameLevel++;
        noFullRows = 0;
        if (!learning) {
            audio.soundNextLevel();
            gameState = GameState.CHANGINGLEVEL;
            //upSideDown = gameLevel % 2 != 1;
        }
    }

    protected void flagFullRows() {
        boolean thereIsFullRow = false;
        boolean rowFull;
        Color c;
        for (Cell[] cells : stackArea) {
            rowFull = true;
            for (Cell cell : cells) {
                if (cell.getTetrominoId() == 0) {
                    rowFull = false;
                    break;
                }
            }
            if (rowFull) {
                thereIsFullRow = true;
                int currentRowScore = 0;
                for (int k = 0; k < cells.length; k++) {
                    currentRowScore += cells[k].getScore() + (gameLevel * (GameConstans.LEVEL_BONUS / 10));
                    c = cells[k].getColor();
                    cells[k] = new Cell(TetrominoFactory.getInstance().getErasedTetromino().getId(),
                            TetrominoFactory.getInstance().getErasedTetromino().getColor());
                    cells[k].setColor(c);
                }
                gameScore += currentRowScore;
                gameScore += gameLevel * GameConstans.LEVEL_BONUS;
            }
        }
        if (thereIsFullRow) {
            gameState = GameState.DELETINGROWS;
            if (!learning) {
                audio.soundClear();
            }
        }
    }

    private void itemFalled(Cell[][] stackArea, Tetromino tetromino) {
        putTetromino(stackArea, tetromino);
        boolean wasFullRow = getFullRowsNum() > 0;
        if (!wasFullRow) {
            if (!learning) {
                audio.soundDown();
            }
            gameScore += tetromino.getScore() + (gameLevel * 10);
            noFullRows++;
            checkPenalty();
            if (tetromino.getStackRow() <= GameConstans.ROW_OFFSET) {
                gameState = GameState.GAMEOVER;
                if (!learning) {
                    audio.soundLose();
                    audio.musicBackgroundStop();
                }
            }
        } else {
            flagFullRows();
        }
        currentTetromino = null;
        droppedElements++;
    }

    private boolean tetrominoIsDownAfter(Cell[][] stackArea, Tetromino tetromino, int p) {
        for (int i = 0; i < tetromino.getPixels().length; i++) {
            for (int j = 0; j < tetromino.getPixels()[i].length; j++) {
                if (tetromino.getPixels()[i][j] != 0) {
                    if ((tetromino.getStackRow() + i + p > stackArea.length - 1) ||
                            (tetromino.getStackCol() + j > stackArea[i].length - 1) ||
                            (stackArea[tetromino.getStackRow() + i + p][tetromino.getStackCol() + j]).getTetrominoId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected int howFarFromDown(Cell[][] stackArea, Tetromino tetromino) {
        boolean down = false;
        int p = 0;
        removeTetromino(stackArea, tetromino);
        while (!down) {
            p++;
            down = tetrominoIsDownAfter(stackArea, tetromino, p);

        }
        putTetromino(stackArea, tetromino);
        return p;
    }

    protected void clearRows() {
        int fullRows = 0;
        for (int i = 0; i < stackArea.length; i++) {
            boolean rowFull = true;
            for (Cell cell : stackArea[i]) {
                if (cell.getTetrominoId() != TetrominoType.ERASED.getTetrominoTypeId()) {
                    rowFull = false;
                    break;
                }
            }
            if (rowFull) {
                fullRows++;
                for (int j = 0; j < stackArea[i].length; j++) {
                    stackArea[i][j] = new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                            TetrominoFactory.getInstance().getEmptyTetromino().getColor());
                }
                for (int k = i; k > 0; k--) {
                    System.arraycopy(stackArea[k - 1], 0, stackArea[k], 0, stackArea[k].length);
                }
            }
        }
        gameScore += fullRows * GameConstans.ROW_SCORE;
        allFullRows += fullRows;
        noFullRows = Math.max(0, noFullRows - (fullRows * 2));
        if (allFullRows >= gameLevel * GameConstans.LEVEL_CHANGE_ROWS) {
            nextLevel();
        }
    }

    /**
     * Megpróbálja az aktuális elemet a megadott pozícióba mozgatni és elforgatni.
     *
     * @param targetX        A célpozíció x koordinátája
     * @param targetRotation A célforgatás állapota (0-3, ahol 0 az eredeti állapot)
     */
    public void moveAndRotateTetrominoTo(Cell[][] stackArea, Tetromino tetromino, int targetX, int targetRotation) {
        if (tetromino == null) {
            return;
        }
        rotateToTarget(stackArea, tetromino, targetRotation);
        moveHorizontally(stackArea, tetromino, targetX);
        dropTetrominoToBottom(stackArea, tetromino);
    }

    private void rotateToTarget(Cell[][] stackArea, Tetromino tetromino, int targetRotation) {
        while (tetrominoRotation != targetRotation) {
            if (!rotateTetrominoRight(stackArea, tetromino)) {
                break;
            }
        }
        tetrominoRotation = targetRotation;
    }

    private void moveHorizontally(Cell[][] stackArea, Tetromino tetromino, int targetX) {
        while (tetromino.getStackCol() != targetX) {
            if (tetromino.getStackCol() < targetX) {
                if (!moveTetrominoRight(stackArea, tetromino)) {
                    break;
                }
            } else {
                if (!moveTetrominoLeft(stackArea, tetromino)) {
                    break;
                }
            }
        }
    }

    private void dropTetrominoToBottom(Cell[][] stackArea, Tetromino tetromino) {
        while (!moveTetrominoDown(stackArea, tetromino)) {
        }
    }

    public void nextIteration() {
        iteration++;
    }

    public Tetromino getCurrentTetromino() {
        return currentTetromino;
    }

    public Cell[][] getStackArea() {
        return stackArea;
    }

    public Cell[][] getSimStackArea() {
        return simStackArea;
    }

    public int getAllFullRows() {
        return allFullRows;
    }

    public long getStartTime() {
        return startTime;
    }

    public double getTetrominoRotation() {
        return tetrominoRotation;
    }

    public Tetromino getNextTetromino() {
        return nextTetromino;
    }

    public int getGameLevel() {
        return gameLevel;
    }

    public int getNoFullRows() {
        return noFullRows;
    }

    public int getGameScore() {
        return gameScore;
    }

    public long getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * getEllapsedTime.
     *
     * @return et
     */
    protected Long getElapsedTimeLong() {
        long et = System.currentTimeMillis() - this.getStartTime();
        return et;
    }

    protected String getElapsedTime() {
        long et = System.currentTimeMillis() - this.getStartTime();
        final long hr = TimeUnit.MILLISECONDS.toHours(et);
        final long min = TimeUnit.MILLISECONDS.toMinutes(et - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(et - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    public GameState getGameState() {
        return gameState;
    }

    public static int getIteration() {
        return iteration;
    }

    public int getDroppedElements() {
        return droppedElements;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setState(GameState gameState) {
        this.gameState = gameState;
    }

    protected void setTetrominos(Tetromino currentTetromino, Tetromino nextTetromino) {
        this.currentTetromino = currentTetromino;
        this.nextTetromino = nextTetromino;
        this.setTetrominoRotation(0);
    }

    public void setTetrominoRotation(double tetrominoRotation) {
        this.tetrominoRotation = tetrominoRotation;
    }

    public void setCurrentSpeed(long currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    @Override
    public void initializeStackComponents(StackUI stackUI, StackManager manager, StackMetrics metrics) {
    }
}
