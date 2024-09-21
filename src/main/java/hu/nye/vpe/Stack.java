package hu.nye.vpe;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameElement;
import hu.nye.vpe.gaming.GamePanel;

/**
 * Stack class.
 */
public class Stack implements GameElement {
    private static final String FONT_NAME = "Truly Madly Dpad";
    private static final int PENALTY_NO_FULL_ROW = 12;
    private static final int ROW_OFFSET = 2;
    private static final int BLOCK_SIZE = 30;
    private static final int COLS = 12;
    private static final int ROWS = 24;
    private static final int BONUS_COLOR_ALPHA = 200;
    private static final Color colorStackBackground = new Color(15, 15, 15);
    private static final Color colorStackBorder = new Color(100, 100, 100, 100);
    private static final Color helperColor = new Color(55, 55, 55, 20);
    private static final Color helperLineColor = new Color(40, 40, 40, 40);
    private static final int LEVEL_BONUS = 20;
    private static final int ROW_SCORE = 100;
    private static final int LEVEL_CHANGE_ROWS = 4;
    private static final long START_SPEED = 10L;
    //private static final long SPEED_ACCELERATION = 50L;
    private static final long SPEED_ACCELERATION = 0L;
    private final int stackX;
    private final int stackY;
    private final int stackW;
    private final int stackH;
    private State state;
    private int clearBlockSize = BLOCK_SIZE;
    private Cell[][] stackArea;
    private Shape currentShape = null;
    private Shape nextShape = null;
    private boolean upSideDown;
    private boolean tickAnim;
    private int yoffset = 0;
    private GameAudio audio;
    private int noFullRows;
    private int gameScore;
    private int gameLevel;
    private int allFullRows;
    private long currentSpeed;
    private int levelTextAlpha = 200;
    private long startTime;

    public Stack() {
        stackX = 2 * this.BLOCK_SIZE;
        stackY = 2 * this.BLOCK_SIZE;
        stackW = (this.COLS * this.BLOCK_SIZE);
        stackH = ((this.ROWS - ROW_OFFSET) * this.BLOCK_SIZE);
    }

    protected void start() {
        stackArea = new Cell[ROWS][COLS];
        emptyStack();
        audio = new GameAudio();
        gameScore = 0;
        gameLevel = 0;
        noFullRows = 0;
        allFullRows = 0;
        state = State.RUNNING;
        startTime = System.currentTimeMillis();
        currentSpeed = START_SPEED;
    }

    private void emptyStack() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                stackArea[i][j] = new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                        ShapeFactory.getInstance().getEmptyShape().getColor());
            }
        }
    }

    private boolean checkShapeIsDown() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if ((currentShape.getStackRow() + i + 1 > stackArea.length - 1) ||
                            (currentShape.getStackCol() + j > stackArea[i].length - 1) ||
                            (stackArea[currentShape.getStackRow() + i + 1][currentShape.getStackCol() + j]).getShapeId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * putShape method.
     */
    public void putShape() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + j] =
                            new Cell(currentShape.getId(), currentShape.getColor());
                    if (currentShape.getPixels()[i][j] == 2) {
                        stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + j].setBonus(Cell.BonusType.BOMB);
                    } else {
                        stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + j].setBonus(Cell.BonusType.NONE);
                    }
                }
            }
        }
    }

    /**
     * removeShape method.
     */
    protected void removeShape() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + j] =
                            new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                                    ShapeFactory.getInstance().getEmptyShape().getColor());
                }
            }
        }
    }

    /**
     * moveShapwDown method.
     */
    protected void moveShapeDown() {
        removeShape();
        if (!checkShapeIsDown()) {
            currentShape.setRowPosition(currentShape.getStackRow() + 1);
            putShape();
        } else {
            itemFalled();
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
                if (cell.getShapeId() == 0) {
                    isFull = false;
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
            stackArea[(x - 1) + ROW_OFFSET][y - 1] = (on ? new Cell(ShapeFactory.getInstance().getLoadedShape().getId(),
                    ShapeFactory.getInstance().getLoadedShape().getColor()) :
                    new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                            ShapeFactory.getInstance().getEmptyShape().getColor()));
        }
    }

    private void generatePenaltyRows(int rowNum) {
        for (int i = ROW_OFFSET; i < stackArea.length; i++) {
            System.arraycopy(stackArea[i], 0, stackArea[i - 1], 0, stackArea[i].length);
        }
        Random rnd = new Random();
        int rn;
        for (int w = ROWS - ROW_OFFSET; w >= ROWS - (rowNum + 1); w--) {
            for (int h = 1; h <= COLS; h++) {
                rn = rnd.nextInt(2);
                insertPixel(w, h, rn == 1);
            }
        }
        noFullRows = 0;
    }

    protected void checkPenalty() {
        if (noFullRows >= PENALTY_NO_FULL_ROW) {
            audio.soundPenalty();
            if (state != State.GAMEOVER) {
                generatePenaltyRows(1);
            }
        }
    }

    protected void nextLevel() {
        allFullRows = 0;
        if (currentSpeed >= SPEED_ACCELERATION) {
            currentSpeed -= SPEED_ACCELERATION;
        }
        gameLevel++;
        noFullRows = 0;
        audio.soundNextLevel();
        state = State.CHANGINGLEVEL;
        //upSideDown = gameLevel % 2 != 1;
    }

    protected void flagFullRows() {
        boolean thereIsFullRow = false;
        boolean rowFull;
        Color c;
        for (Cell[] cells : stackArea) {
            rowFull = true;
            for (Cell cell : cells) {
                if (cell.getShapeId() == 0) {
                    rowFull = false;
                }
            }
            if (rowFull) {
                thereIsFullRow = true;
                int currentRowScore = 0;
                for (int k = 0; k < cells.length; k++) {
                    currentRowScore += cells[k].getScore() + (gameLevel * (LEVEL_BONUS / 10));
                    c = cells[k].getColor();
                    cells[k] = new Cell(ShapeFactory.getInstance().getErasedShape().getId(),
                            ShapeFactory.getInstance().getErasedShape().getColor());
                    cells[k].setColor(c);
                }
                gameScore += currentRowScore;
                gameScore += gameLevel * LEVEL_BONUS;
            }
        }
        if (thereIsFullRow) {
            state = State.DELETINGROWS;
            audio.soundClear();
        }
    }

    private void itemFalled() {
        putShape();
        boolean wasFullRow = getFullRowsNum() > 0;
        if (!wasFullRow) {
            audio.soundDown();
            gameScore += currentShape.getScore() + (gameLevel * 10);
            noFullRows++;
            checkPenalty();
            if (currentShape.getStackRow() <= ROW_OFFSET) {
                state = State.GAMEOVER;
                audio.soundLose();
                audio.musicBackgroundStop();
            }
        } else {
            flagFullRows();
        }
        currentShape = null;
    }

    private boolean checkShapeIsLeft() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if (currentShape.getStackCol() - 1 + j < 0) {
                        return true;
                    }
                    if ((stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() - 1 + j]).getShapeId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move shape left.
     */
    public void moveShapeLeft() {
        removeShape();
        if (!checkShapeIsLeft()) {
            currentShape.setColPosition(currentShape.getStackCol() - 1);
        }
        putShape();
    }

    private boolean checkShapeIsRight() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if (currentShape.getStackCol() + 1 + j > stackArea[i].length - 1) {
                        return true;
                    }
                    if ((stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + 1 + j]).getShapeId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Move shape right.
     */
    public void moveShapeRight() {
        removeShape();
        if (!checkShapeIsRight()) {
            currentShape.setColPosition(currentShape.getStackCol() + 1);
        }
        putShape();
    }

    private boolean checkShapeIsRotate() {
        int x = currentShape.getPixels().length;
        int y = currentShape.getPixels()[0].length;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if (currentShape.getStackRow() + (y - 1) - j < 0 || currentShape.getStackCol() + i < 0 ||
                            currentShape.getStackRow() + (y - 1) - j > stackArea.length - 1 ||
                            currentShape.getStackCol() + i > stackArea[i].length - 1) {
                        return false;
                    }
                    if (stackArea[currentShape.getStackRow() + (y - 1) - j][currentShape.getStackCol() + i].getShapeId() != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Rotate shape (right).
     */
    public void rotateShapeRight() {
        removeShape();
        if (checkShapeIsRotate()) {
            currentShape.rotateRight();
        }
        putShape();
    }

    private boolean shapeIsDownAfter(int p) {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if ((currentShape.getStackRow() + i + p > stackArea.length - 1) ||
                            (currentShape.getStackCol() + j > stackArea[i].length - 1) ||
                            (stackArea[currentShape.getStackRow() + i + p][currentShape.getStackCol() + j]).getShapeId() != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int howFarFromDown() {
        boolean down = false;
        int p = 0;
        while (!down) {
            removeShape();
            p++;
            down = shapeIsDownAfter(p);
            putShape();
        }
        return p;
    }

    private void clearRows() {
        int fullRows = 0;
        for (int i = 0; i < stackArea.length; i++) {
            boolean rowFull = true;
            for (Cell cell : stackArea[i]) {
                if (cell.getShapeId() != Shape.ShapeType.ERASED.getShapeTypeId()) {
                    rowFull = false;
                }
            }
            if (rowFull) {
                fullRows++;
                for (int j = 0; j < stackArea[i].length; j++) {
                    stackArea[i][j] = new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                            ShapeFactory.getInstance().getEmptyShape().getColor());
                }
                for (int k = i; k > 0; k--) {
                    System.arraycopy(stackArea[k - 1], 0, stackArea[k], 0, stackArea[k].length);
                }
            }
        }
        gameScore += fullRows * ROW_SCORE;
        allFullRows += fullRows;
        noFullRows = Math.max(0, noFullRows -= fullRows * 2);
        if (allFullRows >= gameLevel * LEVEL_CHANGE_ROWS) {
            nextLevel();
        }
    }

    /**
     * Megszámolja a lukakat a játéktérben.
     * Egy luk olyan üres cella, ami felett nem üres cella található.
     *
     * @return A lukak száma a játéktérben
     */
    public int countHoles() {
        int holes = 0;
        for (int col = 0; col < COLS; col++) {
            boolean blockFound = false;
            for (int row = 0; row < ROWS; row++) {
                if (stackArea[row][col].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {
                    blockFound = true;
                } else if (blockFound) {
                    // Ha már találtunk blokkot ebben az oszlopban, és most üres cellát találunk, az egy luk
                    holes++;
                }
            }
        }
        return holes;
    }

    /**
     * Kiszámolja a játéktér egyenetlenségét (bumpiness).
     * A bumpiness az egymás melletti oszlopok magasságkülönbségeinek összege.
     *
     * @return A játéktér egyenetlensége (bumpiness)
     */
    public double calculateBumpiness() {
        int[] columnHeights = new int[COLS];

        // Oszlopmagasságok kiszámítása
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                if (stackArea[row][col].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {
                    columnHeights[col] = ROWS - row;
                    break;
                }
            }
        }

        // Bumpiness kiszámítása
        double bumpiness = 0;
        for (int i = 0; i < COLS - 1; i++) {
            bumpiness += Math.abs(columnHeights[i] - columnHeights[i + 1]);
        }

        return bumpiness;
    }

    /**
     * Kiszámolja a játéktér maximális magasságát.
     *
     * @return A játéktér maximális magassága
     */
    public int calculateMaxHeight() {
        int maxHeight = 0;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                if (stackArea[row][col].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {
                    int height = ROWS - row;
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                    break;
                }
            }
        }
        return maxHeight;
    }

    @Override
    public void update() {

    }

    /**
     * Render game elements.
     *
     * @param g2D Graphics2D
     */
    @Override
    public void render(Graphics2D g2D) {
        renderPlayArea(g2D);
        renderStack(g2D);
        if (nextShape != null) {
            renderNextShapePanel(g2D);
        }
        renderScorePanel(g2D);
        renderPenaltyPanel(g2D);
        renderLevelPanel(g2D);
        renderInfoPanel(g2D);
        renderStatisticPanel(g2D);
        if (state == State.CHANGINGLEVEL) {
            renderLevelText(g2D, "L E V E L  " + gameLevel + ".");
        }
        if (state == State.GAMEOVER) {
            renderText(g2D, "G A M E  O V E R");
        }
        if (state == State.PAUSED) {
            renderText(g2D, "P A U S E D");
        }
    }

    /**
     * Render play area.
     *
     * @param g2D Graphics2D
     */
    private void renderPlayArea(Graphics2D g2D) {
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setColor(colorStackBackground);
        g2D.setColor(new Color(colorStackBackground.getRed(), colorStackBackground.getGreen(), colorStackBackground.getBlue(), 190));
        g2D.fillRect(stackX, stackY, stackW, stackH);
        g2D.setColor(colorStackBorder);
        for (int k = 1; k <= COLS; k++) {
            g2D.fill3DRect((stackX - BLOCK_SIZE) + (k * BLOCK_SIZE), stackY - BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, true);
            g2D.fill3DRect((stackX - BLOCK_SIZE) + (k * BLOCK_SIZE), stackY + ((ROWS - ROW_OFFSET) * BLOCK_SIZE),
                    BLOCK_SIZE, BLOCK_SIZE, true);
        }
        for (int k = ROW_OFFSET; k <= ROWS + 1; k++) {
            int y = (stackY - BLOCK_SIZE) + ((k - ROW_OFFSET) * BLOCK_SIZE);
            g2D.fill3DRect(stackX - BLOCK_SIZE, y, BLOCK_SIZE, BLOCK_SIZE, true);
            g2D.fill3DRect(
                    (stackX - BLOCK_SIZE) + ((COLS + 1) * BLOCK_SIZE),
                    y,
                    BLOCK_SIZE, BLOCK_SIZE,
                    true
            );
        }
    }

    /**
     * Render stack.
     *
     * @param g2D Graphics2D
     */
    private void renderStack(Graphics2D g2D) {
        if (state == State.RUNNING || state == State.DELETINGROWS || state == State.GAMEOVER) {
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    if (i >= ROW_OFFSET) {
                        int i1 = stackY + ((ROWS + 1 - i - ROW_OFFSET) * BLOCK_SIZE);
                        if (stackArea[i][j].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {
                            // Deleted blocks
                            if (stackArea[i][j].getShapeId() == Shape.ShapeType.ERASED.getShapeTypeId()) {
                                if (state == State.DELETINGROWS) {
                                    for (int k = 0; k < COLS; k++) {
                                        g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(),
                                                stackArea[i][j].getColor().getBlue(), clearBlockSize / 2));
                                        if (upSideDown) {
                                            g2D.fill3DRect(stackX + j * BLOCK_SIZE, i1,
                                                    BLOCK_SIZE, BLOCK_SIZE, true);
                                        } else {
                                            g2D.fill3DRect(stackX + j * BLOCK_SIZE, stackY + ((i - ROW_OFFSET) * BLOCK_SIZE),
                                                    BLOCK_SIZE, BLOCK_SIZE, true);
                                        }
                                        if (upSideDown) {
                                            g2D.fill3DRect(stackX + j * BLOCK_SIZE, i1,
                                                    BLOCK_SIZE, BLOCK_SIZE, false);
                                        } else {
                                            g2D.fill3DRect(stackX + j * BLOCK_SIZE, stackY + ((i - ROW_OFFSET) * BLOCK_SIZE),
                                                    BLOCK_SIZE, BLOCK_SIZE, false);
                                        }
                                        g2D.setColor(stackArea[i][j].getColor());
                                        if (upSideDown) {
                                            g2D.fill3DRect((stackX + j * BLOCK_SIZE) + (BLOCK_SIZE - clearBlockSize),
                                                    i1 + (BLOCK_SIZE - clearBlockSize),
                                                    BLOCK_SIZE - ((BLOCK_SIZE - clearBlockSize) * 2),
                                                    BLOCK_SIZE - ((BLOCK_SIZE - clearBlockSize) * 2), true);
                                        } else {
                                            g2D.fill3DRect((stackX + j * BLOCK_SIZE) + (BLOCK_SIZE - clearBlockSize),
                                                    (stackY + ((i - ROW_OFFSET) * BLOCK_SIZE)) + (BLOCK_SIZE - clearBlockSize),
                                                    BLOCK_SIZE - ((BLOCK_SIZE - clearBlockSize) * 2),
                                                    BLOCK_SIZE - ((BLOCK_SIZE - clearBlockSize) * 2), true);
                                        }
                                        if (clearBlockSize >= 0) {
                                            if (tickAnim) {
                                                clearBlockSize--;
                                                tickAnim = false;
                                            }
                                        } else {
                                            clearBlockSize = BLOCK_SIZE;
                                            state = State.RUNNING;
                                            clearRows();
                                        }
                                    }
                                }
                                // Inserted penalty blocks
                            } else if (stackArea[i][j].getShapeId() == Shape.ShapeType.LOADED.getShapeTypeId()) {
                                g2D.setColor(Color.DARK_GRAY);
                                if (upSideDown) {
                                    g2D.fill3DRect(stackX + j * BLOCK_SIZE, i1, BLOCK_SIZE, BLOCK_SIZE, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * BLOCK_SIZE, stackY + (i - ROW_OFFSET) * BLOCK_SIZE,
                                            BLOCK_SIZE, BLOCK_SIZE, true);
                                }
                                // Ordinary blocks
                            } else {
                                g2D.setColor(stackArea[i][j].getColor());
                                if (upSideDown) {
                                    g2D.fill3DRect(stackX + j * BLOCK_SIZE, i1, BLOCK_SIZE, BLOCK_SIZE, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * BLOCK_SIZE, stackY + (i - ROW_OFFSET) * BLOCK_SIZE,
                                            BLOCK_SIZE, BLOCK_SIZE, true);
                                }
                                if (stackArea[i][j].getBonus() != Cell.BonusType.NONE) {
                                    renderBonus(g2D, i, j);
                                }
                            }
                        } else {
                            if (currentShape != null && j >= currentShape.getStackCol() &&
                                    j <= currentShape.getStackCol() + currentShape.getHeightPoints() - 1 &&
                                    i >= currentShape.getStackRow()) {
                                g2D.setColor(new Color(currentShape.getColor().getRed(),
                                        currentShape.getColor().getGreen(), currentShape.getColor().getBlue(), 10));
                                g2D.setColor(helperLineColor);
                                if (upSideDown) {
                                    g2D.fillRect(stackX + j * BLOCK_SIZE, i1, BLOCK_SIZE, BLOCK_SIZE);
                                } else {
                                    g2D.fillRect(stackX + j * BLOCK_SIZE, stackY + (i - ROW_OFFSET) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                                }
                            }
                        }
                    }
                }
                renderHelper(g2D);
            }
        }
    }

    /**
     * Render helper.
     *
     * @param g2D Graphics2D
     */
    private void renderHelper(Graphics2D g2D) {
        if (currentShape != null) {
            yoffset = howFarFromDown() - ROW_OFFSET + 1;
            for (int i = 0; i < currentShape.getPixels().length; i++) {
                for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                    if (currentShape.getPixels()[i][j] != 0) {
                        g2D.setColor(helperColor);
                        if (upSideDown) {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * BLOCK_SIZE,
                                    stackY + ((ROWS + 1) - (yoffset + (i + currentShape.getStackRow() + ROW_OFFSET))) * BLOCK_SIZE,
                                    BLOCK_SIZE, BLOCK_SIZE, true);
                        } else {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * BLOCK_SIZE,
                                    stackY + (yoffset + (i + currentShape.getStackRow() - ROW_OFFSET)) * BLOCK_SIZE,
                                    BLOCK_SIZE, BLOCK_SIZE, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Render bonus element.
     *
     * @param g2D Graphics2D
     * @param i cell coordinate
     * @param j cell coordinate
     */
    private void renderBonus(Graphics2D g2D, int i, int j) {
        g2D.setColor(Color.BLACK);
        int x = stackX + (j * BLOCK_SIZE) + (BLOCK_SIZE / 4);
        if (upSideDown) {
            g2D.fillOval(x,
                    stackY + (ROWS + 1 - i - ROW_OFFSET) *
                            BLOCK_SIZE + (BLOCK_SIZE / 4),
                    BLOCK_SIZE - (BLOCK_SIZE / 4) * 2, BLOCK_SIZE - (BLOCK_SIZE / 4) * 2);
        } else {
            g2D.fillOval(x,
                    stackY + (i - ROW_OFFSET) * BLOCK_SIZE + (BLOCK_SIZE / 4), BLOCK_SIZE - (BLOCK_SIZE / 4) * 2,
                    BLOCK_SIZE - (BLOCK_SIZE / 4) * 2);
        }
        g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(),
                stackArea[i][j].getColor().getBlue(), BONUS_COLOR_ALPHA));
        int x1 = stackX + (j * BLOCK_SIZE) + (BLOCK_SIZE / 4) + 4;
        if (upSideDown) {
            g2D.fillOval(x1,
                    stackY + (ROWS + 1 - i - ROW_OFFSET) * BLOCK_SIZE + (BLOCK_SIZE / 4) + 4,
                    BLOCK_SIZE - (BLOCK_SIZE / 4 + 4) * 2, BLOCK_SIZE - (BLOCK_SIZE / 4 + 4) * 2);
        } else {
            g2D.fillOval(x1,
                    stackY + (i - ROW_OFFSET) * BLOCK_SIZE + (BLOCK_SIZE / 4) + 4,
                    BLOCK_SIZE - (BLOCK_SIZE / 4 + 4) * 2, BLOCK_SIZE - (BLOCK_SIZE / 4 + 4) * 2);
        }
    }

    private void renderText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(FONT_NAME, Font.BOLD, BLOCK_SIZE * 2));
        textWidth = g2D.getFontMetrics().stringWidth(text);
        g2D.setColor(new Color(250, 250, 250, 230));
        g2D.drawString(text, stackX + (stackW / 2 - textWidth / 2), stackY + (stackH / 2 - textWidth / 2));
    }

    private void renderLevelText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(FONT_NAME, Font.BOLD, BLOCK_SIZE * 2));
        textWidth = g2D.getFontMetrics().stringWidth(text);
        g2D.setColor(new Color(250, 250, 250, levelTextAlpha));
        g2D.drawString(text, stackX + (stackW / 2 - textWidth / 2), stackY + (stackH / 2 - textWidth / 2));
        if (levelTextAlpha > 0) {
            if (tickAnim) {
                levelTextAlpha -= 2;
                tickAnim = false;
            }
        } else {
            levelTextAlpha = 200;
            state = State.RUNNING;
        }
    }

    private void renderNextShapePanel(Graphics2D g2D) {
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelHeight = 4 * BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int nextPanelHeight = 4 * BLOCK_SIZE;
        GamePanel nextPanel = new GamePanel(panelX, panelY, panelWidth, panelHeight, panelBorderWidth, panelColor,
                BLOCK_SIZE, "Next shape", FONT_NAME);
        nextPanel.render(g2D);
        int nbW = nextShape.getPixels()[0].length * BLOCK_SIZE;
        int nbH = nextShape.getPixels().length * BLOCK_SIZE;
        int nbX = panelX + (panelWidth / 2 - nbW / 2);
        int nbY = panelY + BLOCK_SIZE + (nextPanelHeight / 2 - nbH / 2);
        if (state != State.PAUSED) {
            for (int i = 0; i < nextShape.getPixels().length; i++) {
                for (int j = 0; j < nextShape.getPixels()[i].length; j++) {
                    if (nextShape.getPixels()[i][j] != 0) {
                        g2D.setColor(nextShape.getColor());
                        g2D.fill3DRect(nbX + j * BLOCK_SIZE, nbY + i * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, true);
                    }
                }
            }
        }
    }

    private void renderPenaltyPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int penaltyPanelOffsetY = BLOCK_SIZE * 6;
        int penaltyPanelHeight = BLOCK_SIZE;
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel penaltyPanel = new GamePanel(panelX, panelY + penaltyPanelOffsetY, panelWidth,
                penaltyPanelHeight, panelBorderWidth, panelColor, BLOCK_SIZE, "Penalty row", FONT_NAME);
        penaltyPanel.render(g2D);
        float penaltyWidth = panelWidth - panelBorderWidth * 2;
        penaltyWidth = Math.round((penaltyWidth / (PENALTY_NO_FULL_ROW - 1)) * noFullRows);
        int penaltyHeight = penaltyPanelHeight - panelBorderWidth * 2;
        int ppX = panelX + panelBorderWidth;
        int ppY = panelY + BLOCK_SIZE + penaltyPanelOffsetY + panelBorderWidth;
        g2D.setColor(new Color(55 + (200 / PENALTY_NO_FULL_ROW) * noFullRows, 0, 0, 100));
        if (noFullRows > 0) {
            g2D.fillRect(ppX, ppY, (int) penaltyWidth, penaltyHeight);
        }
    }

    private void renderScorePanel(Graphics2D g2D) {
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int scorePanelOffsetY = BLOCK_SIZE * 9;
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelHeight = 2 * BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel scorePanel = new GamePanel(panelX, panelY + scorePanelOffsetY, panelWidth, panelHeight,
                panelBorderWidth, panelColor, BLOCK_SIZE, "Score", FONT_NAME);
        scorePanel.render(g2D);
        String gamePointsStr = String.valueOf(gameScore);
        int stringHeight = BLOCK_SIZE - 2;
        g2D.setFont(new Font(FONT_NAME, Font.PLAIN, stringHeight));
        int stringWidth = g2D.getFontMetrics().stringWidth(gamePointsStr);
        g2D.setColor(Color.LIGHT_GRAY);
        int scoreX = panelX + (panelWidth / 2 - stringWidth / 2);
        int scoreY = panelY + BLOCK_SIZE + scorePanelOffsetY + (panelHeight / 2 - stringHeight / 2) - 2;
        g2D.drawString(gamePointsStr, scoreX, scoreY + stringHeight - 3);
    }

    private void renderLevelPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int levelPanelOffsetY = BLOCK_SIZE * 13;
        int levelPanelHeight = BLOCK_SIZE;
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel levelPanel = new GamePanel(panelX, panelY + levelPanelOffsetY, panelWidth, levelPanelHeight,
                panelBorderWidth, panelColor, BLOCK_SIZE, "Level: " + gameLevel, FONT_NAME);
        levelPanel.render(g2D);
        int levelHeight = levelPanelHeight - panelBorderWidth * 2;
        float nextLevelWidth = panelWidth - panelBorderWidth * 2;
        nextLevelWidth = Math.round((nextLevelWidth / (gameLevel * LEVEL_CHANGE_ROWS)) * (allFullRows + 1));
        int lpX = panelX + panelBorderWidth;
        int lpY = panelY + BLOCK_SIZE + levelPanelOffsetY + panelBorderWidth;
        int levelStringHeight = BLOCK_SIZE - 12;
        g2D.setFont(new Font(FONT_NAME, Font.PLAIN, levelStringHeight));
        g2D.setColor(new Color(0, 55 + (200 / (Math.max(gameLevel, 1) * LEVEL_CHANGE_ROWS)) * allFullRows, 0, 100));
        if (allFullRows > 0) {
            g2D.fillRect(lpX, lpY, (int) nextLevelWidth, levelHeight);
        }
    }

    private void renderInfoPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int infoPanelOffsetY = BLOCK_SIZE * 16;
        int infoPanelHeight = 3 * BLOCK_SIZE;
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel infoPanel = new GamePanel(panelX, panelY + infoPanelOffsetY, panelWidth, infoPanelHeight,
                panelBorderWidth, panelColor, BLOCK_SIZE, "Info", FONT_NAME);
        infoPanel.render(g2D);
        String infoStrM = "M: Music On/Off";
        String infoStrP = "P: Pause/Resume";
        String infoStrC = "Arrows: Move";
        String infoStrR = "Space: Rotate";
        int stringHeight = BLOCK_SIZE - 14;
        g2D.setFont(new Font(FONT_NAME, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int infoX = panelX + BLOCK_SIZE;
        int infoY = panelY + BLOCK_SIZE + infoPanelOffsetY + BLOCK_SIZE / 2;
        g2D.drawString(infoStrM, infoX, infoY + stringHeight - 5);
        g2D.drawString(infoStrP, infoX, infoY + BLOCK_SIZE / 2 + stringHeight - 5);
        g2D.drawString(infoStrC, infoX, infoY + BLOCK_SIZE + stringHeight - 5);
        g2D.drawString(infoStrR, infoX, infoY + BLOCK_SIZE * 3 / 2 + stringHeight - 5);
    }

    private void renderStatisticPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * BLOCK_SIZE;
        int panelY = BLOCK_SIZE;
        int statPanelOffsetY = BLOCK_SIZE * 21;
        int statPanelHeight = 2 * BLOCK_SIZE;
        int panelWidth = 6 * this.BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel statPanel = new GamePanel(panelX, panelY + statPanelOffsetY, panelWidth, statPanelHeight,
                panelBorderWidth, panelColor, BLOCK_SIZE, "Stat", FONT_NAME);
        statPanel.render(g2D);
        String allRowsStr = "Rows: " + allFullRows;
        String timeStr = "Time: " + getEllapsedTime();
        int stringHeight = BLOCK_SIZE - 14;
        g2D.setFont(new Font(FONT_NAME, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int scoreX = panelX + BLOCK_SIZE;
        int scoreY = panelY + BLOCK_SIZE + statPanelOffsetY + BLOCK_SIZE / 2;
        g2D.drawString(allRowsStr, scoreX, scoreY + stringHeight - 5);
        g2D.drawString(timeStr, scoreX, scoreY + BLOCK_SIZE / 2 + stringHeight - 5);
    }

    /**
     * Getters.
     */
    private String getEllapsedTime() {
        long et = System.currentTimeMillis() - startTime;
        final long hr = TimeUnit.MILLISECONDS.toHours(et);
        final long min = TimeUnit.MILLISECONDS.toMinutes(et - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(et - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    public Shape getCurrentShape() {
        return currentShape;
    }

    public State getState() {
        return state;
    }

    public int getLevel() {
        return gameLevel;
    }

    protected long getCurrentSpeed() {
        return currentSpeed;
    }

    protected int getGameScore() {
        return gameScore;
    }

    protected Cell[][] getStackArea() {
        return stackArea;
    }

    protected int getNoFullRows() {
        return noFullRows;
    }

    public int getGameLevel() {
        return gameLevel;
    }

    public int getAllFullRows() {
        return allFullRows;
    }

    /**
     * Setters.
     */
    public void setTickAnim(boolean tick) {
        this.tickAnim = tick;
    }

    public void setState(State state) {
        this.state = state;
    }

    protected void setShapes(Shape currentShape, Shape nextShape) {
        this.currentShape = currentShape;
        this.nextShape = nextShape;
    }
}