package hu.nye.vpe.tetris;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameConstans;
import hu.nye.vpe.gaming.GameState;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class StackManager implements StackComponent {
    private Cell[][] stackArea = new Cell[GameConstans.ROWS][GameConstans.COLS];
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
        initializeMetrics();
    }

    private void initializeGameState() {
        initStack();
        gameScore = 0;
        gameLevel = 0;
        noFullRows = 0;
        allFullRows = 0;
        gameState = GameState.RUNNING;
        startTime = System.currentTimeMillis();
        currentSpeed = learning ? GameConstans.LEARNING_START_SPEED : GameConstans.START_SPEED;
    }

    private void initializeMetrics() {
        metrics.setMetricDroppedElements(0);
        droppedElements = 0;
        metrics.calculateGameMetrics();
    }

    public void initStack() {
        if (stackArea == null) {
            stackArea = new Cell[GameConstans.ROWS][GameConstans.COLS];
        }
        for (int i = 0; i < GameConstans.ROWS; i++) {
            for (int j = 0; j < GameConstans.COLS; j++) {
                stackArea[i][j] = new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                        TetrominoFactory.getInstance().getEmptyTetromino().getColor());
            }
        }
    }

    private boolean isTetrominoDown() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if (isBottomCollision(i, j) || isDownCollision(i, j)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBottomCollision(int i, int j) {
        return currentTetromino.getStackRow() + i + 1 > stackArea.length - 1;
    }

    private boolean isDownCollision(int i, int j) {
        int nextRow = currentTetromino.getStackRow() + i + 1;
        int col = currentTetromino.getStackCol() + j;
        return col <= stackArea[0].length - 1 &&
                nextRow <= stackArea.length - 1 &&
                stackArea[nextRow][col].getTetrominoId() != 0 &&
                !isPartOfCurrentTetromino(nextRow, col);
    }

    private boolean isPartOfCurrentTetromino(int row, int col) {
        int localRow = row - currentTetromino.getStackRow();
        int localCol = col - currentTetromino.getStackCol();
        return localRow >= 0 && localRow < currentTetromino.getPixels().length &&
                localCol >= 0 && localCol < currentTetromino.getPixels()[0].length &&
                currentTetromino.getPixels()[localRow][localCol] != 0;
    }

    /*
    private boolean checkTetrominoIsDown() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if ((currentTetromino.getStackRow() + i + 1 > stackArea.length - 1) ||
                            (currentTetromino.getStackCol() + j > stackArea[i].length - 1) ||
                            (stackArea[currentTetromino.getStackRow() + i + 1][currentTetromino.getStackCol() + j]).getTetrominoId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
     */

    /**
     * putTetromino method.
     */
    public void putTetromino() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() + j] =
                            new Cell(currentTetromino.getId(), currentTetromino.getColor());
                    if (currentTetromino.getPixels()[i][j] == 2) {
                        stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() + j].setBonus(BonusType.BOMB);
                    } else {
                        stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() + j].setBonus(BonusType.NONE);
                    }
                }
            }
        }
    }

    /**
     * removeTetromino method.
     */
    protected void removeTetromino() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() + j] =
                            new Cell(TetrominoFactory.getInstance().getEmptyTetromino().getId(),
                                    TetrominoFactory.getInstance().getEmptyTetromino().getColor());
                }
            }
        }
    }

    /**
     * moveTetrominoDown method.
     */
    protected boolean moveTetrominoDown() {
        removeTetromino();
        //if (!checkTetrominoIsDown()) {
        if (!isTetrominoDown()) {
            currentTetromino.setRowPosition(currentTetromino.getStackRow() + 1);
            putTetromino();
            return false;
        } else {
            itemFalled();
            metrics.calculateGameMetrics();
            return true;
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
            stackArea[(x - 1) + GameConstans.ROW_OFFSET][y - 1] = (on ? new Cell(TetrominoFactory.getInstance().getLoadedTetromino().getId(),
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

    private void itemFalled() {
        metrics.calculateGameMetrics();
        putTetromino();
        boolean wasFullRow = getFullRowsNum() > 0;
        if (!wasFullRow) {
            if (!learning) {
                audio.soundDown();
            }
            gameScore += currentTetromino.getScore() + (gameLevel * 10);
            noFullRows++;
            checkPenalty();
            if (currentTetromino.getStackRow() <= GameConstans.ROW_OFFSET) {
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
        metrics.setMetricDroppedElements(droppedElements);
    }

    private boolean checkTetrominoIsLeft() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if (currentTetromino.getStackCol() - 1 + j < 0) {
                        return true;
                    }
                    if ((stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() - 1 + j]).getTetrominoId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move tetromino left.
     */
    public boolean moveTetrominoLeft() {
        removeTetromino();
        if (!checkTetrominoIsLeft()) {
            currentTetromino.setColPosition(currentTetromino.getStackCol() - 1);
            putTetromino();
            return true;
        } else {
            putTetromino();
            return false;
        }
    }

    private boolean checkTetrominoIsRight() {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if (currentTetromino.getStackCol() + 1 + j > stackArea[i].length - 1) {
                        return true;
                    }
                    if ((stackArea[currentTetromino.getStackRow() + i][currentTetromino.getStackCol() + 1 + j]).getTetrominoId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move tetromino right.
     */
    public boolean moveTetrominoRight() {
        removeTetromino();
        if (!checkTetrominoIsRight()) {
            currentTetromino.setColPosition(currentTetromino.getStackCol() + 1);
            putTetromino();
            return true;
        } else {
            putTetromino();
            return false;
        }
    }

    private boolean checkTetrominoIsRotate() {
        int x = currentTetromino.getPixels().length;
        int y = currentTetromino.getPixels()[0].length;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if (currentTetromino.getStackRow() + (y - 1) - j < 0 || currentTetromino.getStackCol() + i < 0 ||
                            currentTetromino.getStackRow() + (y - 1) - j > stackArea.length - 1 ||
                            currentTetromino.getStackCol() + i > stackArea[i].length - 1) {
                        return false;
                    }
                    if (stackArea[currentTetromino.getStackRow() + (y - 1) - j][currentTetromino.getStackCol() + i].getTetrominoId() != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Rotate tetromino (right).
     */
    public boolean rotateTetrominoRight() {
        removeTetromino();
        if (checkTetrominoIsRotate()) {
            currentTetromino.rotateRight();
            putTetromino();
            if (tetrominoRotation < 75) {
                tetrominoRotation += 25;
            } else {
                tetrominoRotation = 0;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Rotate tetromino (left).
     */
    public void rotateTetrominoLeft() {
        removeTetromino();
        if (checkTetrominoIsRotate()) {
            currentTetromino.rotateLeft();
            if (tetrominoRotation < 75) {
                tetrominoRotation += 25;
            } else {
                tetrominoRotation = 0;
            }
        }
        putTetromino();
    }

    private boolean tetrominoIsDownAfter(int p) {
        for (int i = 0; i < currentTetromino.getPixels().length; i++) {
            for (int j = 0; j < currentTetromino.getPixels()[i].length; j++) {
                if (currentTetromino.getPixels()[i][j] != 0) {
                    if ((currentTetromino.getStackRow() + i + p > stackArea.length - 1) ||
                            (currentTetromino.getStackCol() + j > stackArea[i].length - 1) ||
                            (stackArea[currentTetromino.getStackRow() + i + p][currentTetromino.getStackCol() + j]).getTetrominoId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int howFarFromDown() {
        boolean down = false;
        int p = 0;
        removeTetromino();
        while (!down) {
            p++;
            down = tetrominoIsDownAfter(p);

        }
        putTetromino();
        return p;
    }

    public void clearRows() {
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
     * @param targetY        A célpozíció y koordinátája
     * @param targetRotation A célforgatás állapota (0-3, ahol 0 az eredeti állapot)
     */
    public void moveAndRotateTetrominoTo(int targetX, int targetY, int targetRotation) {
        if (currentTetromino == null) {
            return;
        }
        // Forgatás
        int currentRotation = (int) (tetrominoRotation / 25);
        while (currentRotation != targetRotation) {
            if (!rotateTetrominoRight()) {
                return;
            }
            currentRotation = (currentRotation + 1) % 4;
        }
        // Vízszintes mozgatás
        while (currentTetromino.getStackCol() != targetX) {
            if (currentTetromino.getStackCol() < targetX) {
                moveTetrominoRight();
            } else {
                moveTetrominoLeft();
            }
        }
        // Függőleges mozgatás
        while (currentTetromino.getStackRow() < targetY) {
            moveTetrominoDown();
        }

    }

    /**
     * Az elemet a lehető legmélyebbre ejti.
     */
    public void dropTetrominoToBottom() {
        if (currentTetromino != null) {
            while (!moveTetrominoDown()) {
            }
        }
    }

    public Tetromino getCurrentTetromino() {
        return currentTetromino;
    }

    public Cell[][] getStackArea() {
        return stackArea;
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
    public Long getElapsedTimeLong() {
        long et = System.currentTimeMillis() - this.getStartTime();
        return et;
    }

    public String getElapsedTime() {
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

    public void nextIteration() {
        iteration++;
    }

    public void setTetrominoRotation(double tetrominoRotation) {
        this.tetrominoRotation = tetrominoRotation;
    }

    public void setCurrentSpeed(long currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    @Override
    public void initializeStackComponents(StackUI stackUI, StackManager manager, StackMetrics metrics) {
        this.metrics = metrics;
    }
}
