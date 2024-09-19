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
    private final String fontName = "Truly Madly Dpad";
    private final Color colorStackBackground = new Color(15, 15, 15);
    private final Color colorStackBorder = new Color(100, 100, 100, 100);
    private final Color helperColor = new Color(25, 25, 25, 90);
    private final Color helperLineColor = new Color(40, 40, 40, 40);
    private State state;
    private final int penaltyNoFullrows = 12;
    private final int rowOffset = 2;
    private final int stackX;
    private final int stackY;
    private final int stackW;
    private final int stackH;
    private final int blockSize = 30;
    private int clearBlockSize = blockSize;
    private final int cols = 12;
    private final int rows = 24;
    private Cell[][] stackArea;
    private Shape currentShape = null;
    private Shape nextShape = null;
    private boolean upSideDown;
    private boolean tickAnim;
    int yoffset = 0;
    private final int bonusColorAlpha = 200;
    private GameAudio audio;
    private int noFullRows;
    private int gameScore;
    private int gameLevel;
    private final int levelBonus = 20;
    private int allFullRows;
    private final int rowScore = 100;
    private final int levelChangeRows = 4;
    private final long startSpeed = 1000L;
    private final long speedAcceleration = 50L;
    private final long dropSpeed = 10L;
    private long currentSpeed;
    private int levelTextAlpha = 200;
    private long startTime;

    public Stack() {
        stackX = 2 * this.blockSize;
        stackY = 2 * this.blockSize;
        stackW = (this.cols * this.blockSize);
        stackH = ((this.rows - rowOffset) * this.blockSize);
    }

    protected void start() {
        stackArea = new Cell[rows][cols];
        emptyStack();
        audio = new GameAudio();
        gameScore = 0;
        gameLevel = 0;
        noFullRows = 0;
        allFullRows = 0;
        state = State.RUNNING;
        startTime = System.currentTimeMillis();
    }

    private void emptyStack() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
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
            stackArea[(x - 1) + rowOffset][y - 1] = (on ? new Cell(ShapeFactory.getInstance().getLoadedShape().getId(),
                    ShapeFactory.getInstance().getLoadedShape().getColor()) :
                    new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                            ShapeFactory.getInstance().getEmptyShape().getColor()));
        }
    }

    protected void genPenaltyRows(int rowNum) {
        for (int i = rowOffset; i < stackArea.length; i++) {
            System.arraycopy(stackArea[i], 0, stackArea[i - 1], 0, stackArea[i].length);
        }
        Random rnd = new Random();
        int rn;
        for (int w = rows - rowOffset; w >= rows - (rowNum + 1); w--) {
            for (int h = 1; h <= cols; h++) {
                rn = rnd.nextInt(2);
                insertPixel(w, h, rn == 1);
            }
        }
        noFullRows = 0;
    }

    protected void checkPenalty() {
        if (noFullRows >= penaltyNoFullrows) {
            audio.soundPenalty();
            if (state != State.GAMEOVER) {
                genPenaltyRows(1);
            }
        }
    }

    protected void nextLevel() {
        allFullRows = 0;
        if (currentSpeed >= speedAcceleration) {
            currentSpeed -= speedAcceleration;
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
                    currentRowScore += cells[k].getScore() + (gameLevel * (levelBonus / 10));
                    c = cells[k].getColor();
                    cells[k] = new Cell(ShapeFactory.getInstance().getErasedShape().getId(),
                            ShapeFactory.getInstance().getErasedShape().getColor());
                    cells[k].setColor(c);
                }
                gameScore += currentRowScore;
                gameScore += gameLevel * levelBonus;
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
            if (currentShape.getStackRow() <= rowOffset) {
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

    @Override
    public void update() {

    }

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

    private void renderPlayArea(Graphics2D g2D) {
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setColor(colorStackBackground);
        g2D.setColor(new Color(colorStackBackground.getRed(), colorStackBackground.getGreen(), colorStackBackground.getBlue(), 190));
        g2D.fillRect(stackX, stackY, stackW, stackH);
        g2D.setColor(colorStackBorder);
        for (int k = 1; k <= cols; k++) {
            g2D.fill3DRect((stackX - blockSize) + (k * blockSize), stackY - blockSize, blockSize, blockSize, true);
            g2D.fill3DRect((stackX - blockSize) + (k * blockSize), stackY + ((rows - rowOffset) * blockSize), blockSize, blockSize, true);
        }
        for (int k = rowOffset; k <= rows + 1; k++) {
            int y = (stackY - blockSize) + ((k - rowOffset) * blockSize);
            g2D.fill3DRect(stackX - blockSize, y, blockSize, blockSize, true);
            g2D.fill3DRect(
                    (stackX - blockSize) + ((cols + 1) * blockSize),
                    y,
                    blockSize, blockSize,
                    true
            );
        }
    }

    private void renderStack(Graphics2D g2D) {
        if (state == State.RUNNING || state == State.DELETINGROWS || state == State.GAMEOVER) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (i >= rowOffset) {
                        int i1 = stackY + ((rows + 1 - i - rowOffset) * blockSize);
                        if (stackArea[i][j].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {
                            // Deleted blocks
                            if (stackArea[i][j].getShapeId() == Shape.ShapeType.ERASED.getShapeTypeId()) {
                                if (state == State.DELETINGROWS) {
                                    for (int k = 0; k < cols; k++) {
                                        g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(),
                                                stackArea[i][j].getColor().getBlue(), clearBlockSize / 2));
                                        if (upSideDown) {
                                            g2D.fill3DRect(stackX + j * blockSize, i1,
                                                    blockSize, blockSize, true);
                                        } else {
                                            g2D.fill3DRect(stackX + j * blockSize, stackY + ((i - rowOffset) * blockSize),
                                                    blockSize, blockSize, true);
                                        }
                                        if (upSideDown) {
                                            g2D.fill3DRect(stackX + j * blockSize, i1,
                                                    blockSize, blockSize, false);
                                        } else {
                                            g2D.fill3DRect(stackX + j * blockSize, stackY + ((i - rowOffset) * blockSize),
                                                    blockSize, blockSize, false);
                                        }
                                        g2D.setColor(stackArea[i][j].getColor());
                                        if (upSideDown) {
                                            g2D.fill3DRect((stackX + j * blockSize) + (blockSize - clearBlockSize),
                                                    i1 + (blockSize - clearBlockSize),
                                                    blockSize - ((blockSize - clearBlockSize) * 2),
                                                    blockSize - ((blockSize - clearBlockSize) * 2), true);
                                        } else {
                                            g2D.fill3DRect((stackX + j * blockSize) + (blockSize - clearBlockSize),
                                                    (stackY + ((i - rowOffset) * blockSize)) + (blockSize - clearBlockSize),
                                                    blockSize - ((blockSize - clearBlockSize) * 2),
                                                    blockSize - ((blockSize - clearBlockSize) * 2), true);
                                        }
                                        if (clearBlockSize >= 0) {
                                            if (tickAnim) {
                                                clearBlockSize--;
                                                tickAnim = false;
                                            }
                                        } else {
                                            clearBlockSize = blockSize;
                                            state = State.RUNNING;
                                            clearRows();
                                        }
                                    }
                                }
                            // Inserted penalty blocks
                            } else if (stackArea[i][j].getShapeId() == Shape.ShapeType.LOADED.getShapeTypeId()) {
                                g2D.setColor(Color.DARK_GRAY);
                                if (upSideDown) {
                                    g2D.fill3DRect(stackX + j * blockSize, i1, blockSize, blockSize, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize,
                                            blockSize, blockSize, true);
                                }
                            // Ordinary blocks
                            } else {
                                g2D.setColor(stackArea[i][j].getColor());
                                if (upSideDown) {
                                    g2D.fill3DRect(stackX + j * blockSize, i1, blockSize, blockSize, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize,
                                            blockSize, blockSize, true);
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
                                    g2D.fillRect(stackX + j * blockSize, i1, blockSize, blockSize);
                                } else {
                                    g2D.fillRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize);
                                }
                            }
                        }
                    }
                }
                renderHelper(g2D);
            }
        }
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
        gameScore += fullRows * rowScore;
        allFullRows += fullRows;
        noFullRows = Math.max(0, noFullRows -= fullRows * 2);
        if (allFullRows >= gameLevel * levelChangeRows) {
            nextLevel();
        }
    }

    private void renderHelper(Graphics2D g2D) {
        if (currentShape != null) {
            yoffset = howFarFromDown() - rowOffset + 1;
            for (int i = 0; i < currentShape.getPixels().length; i++) {
                for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                    if (currentShape.getPixels()[i][j] != 0) {
                        g2D.setColor(helperColor);
                        if (upSideDown) {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * blockSize,
                                    stackY + ((rows + 1) - (yoffset + (i + currentShape.getStackRow() + rowOffset))) * blockSize,
                                    blockSize, blockSize, true);
                        } else {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * blockSize,
                                    stackY + (yoffset + (i + currentShape.getStackRow() - rowOffset)) * blockSize,
                                    blockSize, blockSize, true);
                        }
                    }
                }
            }
        }
    }

    private void renderBonus(Graphics2D g2D, int i, int j) {
        g2D.setColor(Color.BLACK);
        int x = stackX + (j * blockSize) + (blockSize / 4);
        if (upSideDown) {
            g2D.fillOval(x,
                    stackY + (rows + 1 - i - rowOffset) *
                            blockSize + (blockSize / 4),
                    blockSize - (blockSize / 4) * 2, blockSize - (blockSize / 4) * 2);
        } else {
            g2D.fillOval(x,
                    stackY + (i - rowOffset) * blockSize + (blockSize / 4), blockSize - (blockSize / 4) * 2,
                    blockSize - (blockSize / 4) * 2);
        }
        g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(),
                stackArea[i][j].getColor().getBlue(), bonusColorAlpha));
        int x1 = stackX + (j * blockSize) + (blockSize / 4) + 4;
        if (upSideDown) {
            g2D.fillOval(x1,
                    stackY + (rows + 1 - i - rowOffset) * blockSize + (blockSize / 4) + 4,
                    blockSize - (blockSize / 4 + 4) * 2, blockSize - (blockSize / 4 + 4) * 2);
        } else {
            g2D.fillOval(x1,
                    stackY + (i - rowOffset) * blockSize + (blockSize / 4) + 4,
                    blockSize - (blockSize / 4 + 4) * 2, blockSize - (blockSize / 4 + 4) * 2);
        }
    }

    private void renderText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(fontName, Font.BOLD, blockSize * 2));
        textWidth = g2D.getFontMetrics().stringWidth(text);
        g2D.setColor(new Color(250, 250, 250,  230));
        g2D.drawString(text, stackX + (stackW / 2 - textWidth / 2), stackY + (stackH / 2 - textWidth / 2));
    }

    private void renderLevelText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(fontName, Font.BOLD, blockSize * 2));
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
        int panelWidth = 6 * this.blockSize;
        int panelHeight = 4 * blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int nextPanelHeight = 4 * blockSize;
        GamePanel nextPanel = new GamePanel(panelX, panelY, panelWidth, panelHeight, panelBorderWidth, panelColor,
                blockSize, "Next shape", fontName);
        nextPanel.render(g2D);
        int nbW = nextShape.getPixels()[0].length * blockSize;
        int nbH = nextShape.getPixels().length * blockSize;
        int nbX = panelX + (panelWidth / 2 - nbW / 2);
        int nbY = panelY + blockSize + (nextPanelHeight / 2 - nbH / 2);
        if (state != State.PAUSED) {
            for (int i = 0; i < nextShape.getPixels().length; i++) {
                for (int j = 0; j < nextShape.getPixels()[i].length; j++) {
                    if (nextShape.getPixels()[i][j] != 0) {
                        g2D.setColor(nextShape.getColor());
                        g2D.fill3DRect(nbX + j * blockSize, nbY + i * blockSize, blockSize, blockSize, true);
                    }
                }
            }
        }
    }

    private void renderPenaltyPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int penaltyPanelOffsetY = blockSize * 6;
        int penaltyPanelHeight = blockSize;
        int panelWidth = 6 * this.blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel penaltyPanel = new GamePanel(panelX, panelY + penaltyPanelOffsetY, panelWidth,
                penaltyPanelHeight, panelBorderWidth, panelColor, blockSize, "Penalty row", fontName);
        penaltyPanel.render(g2D);
        float penaltyWidth = panelWidth - panelBorderWidth * 2;
        penaltyWidth = Math.round((penaltyWidth / (penaltyNoFullrows - 1)) * noFullRows);
        int penaltyHeight = penaltyPanelHeight - panelBorderWidth * 2;
        int ppX = panelX + panelBorderWidth;
        int ppY = panelY + blockSize + penaltyPanelOffsetY + panelBorderWidth;
        g2D.setColor(new Color(55 + (200 / penaltyNoFullrows) * noFullRows, 0, 0, 100));
        if (noFullRows > 0) {
            g2D.fillRect(ppX, ppY, (int) penaltyWidth, penaltyHeight);
        }
    }

    private void renderScorePanel(Graphics2D g2D) {
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int scorePanelOffsetY = blockSize * 9;
        int panelWidth = 6 * this.blockSize;
        int panelHeight = 2 * blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel scorePanel = new GamePanel(panelX, panelY + scorePanelOffsetY, panelWidth, panelHeight,
                panelBorderWidth, panelColor, blockSize, "Score", fontName);
        scorePanel.render(g2D);
        String gamePointsStr = String.valueOf(gameScore);
        int stringHeight = blockSize - 2;
        g2D.setFont(new Font(fontName, Font.PLAIN, stringHeight));
        int stringWidth = g2D.getFontMetrics().stringWidth(gamePointsStr);
        g2D.setColor(Color.LIGHT_GRAY);
        int scoreX = panelX + (panelWidth / 2 - stringWidth / 2);
        int scoreY = panelY + blockSize + scorePanelOffsetY + (panelHeight / 2 - stringHeight / 2) - 2;
        g2D.drawString(gamePointsStr, scoreX, scoreY + stringHeight - 3);
    }

    private void renderLevelPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int levelPanelOffsetY = blockSize * 13;
        int levelPanelHeight = blockSize;
        int panelWidth = 6 * this.blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel levelPanel = new GamePanel(panelX, panelY + levelPanelOffsetY, panelWidth, levelPanelHeight,
                panelBorderWidth, panelColor, blockSize, "Level: " + gameLevel, fontName);
        levelPanel.render(g2D);
        int levelHeight = levelPanelHeight - panelBorderWidth * 2;
        float nextLevelWidth = panelWidth - panelBorderWidth * 2;
        nextLevelWidth = Math.round((nextLevelWidth / (gameLevel * levelChangeRows)) * (allFullRows + 1));
        int lpX = panelX + panelBorderWidth;
        int lpY = panelY + blockSize + levelPanelOffsetY + panelBorderWidth;
        int levelStringHeight = blockSize - 12;
        g2D.setFont(new Font(fontName, Font.PLAIN, levelStringHeight));
        g2D.setColor(new Color(0, 55 + (200 / (Math.max(gameLevel, 1) * levelChangeRows)) * allFullRows, 0, 100));
        if (allFullRows > 0) {
            g2D.fillRect(lpX, lpY, (int) nextLevelWidth, levelHeight);
        }
    }



    private void renderInfoPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int infoPanelOffsetY = blockSize * 16;
        int infoPanelHeight = 3 * blockSize;
        int panelWidth = 6 * this.blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel infoPanel = new GamePanel(panelX, panelY + infoPanelOffsetY, panelWidth, infoPanelHeight,
                panelBorderWidth, panelColor, blockSize, "Info", fontName);
        infoPanel.render(g2D);
        String infoStrM = "M: Music On/Off";
        String infoStrP = "P: Pause/Resume";
        String infoStrC = "Arrows: Move";
        String infoStrR = "Space: Rotate";
        int stringHeight = blockSize - 14;
        g2D.setFont(new Font(fontName, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int infoX = panelX + blockSize;
        int infoY = panelY + blockSize + infoPanelOffsetY + blockSize / 2;
        g2D.drawString(infoStrM, infoX, infoY + stringHeight - 5);
        g2D.drawString(infoStrP, infoX, infoY + blockSize / 2 + stringHeight - 5);
        g2D.drawString(infoStrC, infoX, infoY + blockSize + stringHeight - 5);
        g2D.drawString(infoStrR, infoX, infoY + blockSize * 3 / 2 + stringHeight - 5);
    }

    private void renderStatisticPanel(Graphics2D g2D) {
        int panelX = stackW + 4 * blockSize;
        int panelY = blockSize;
        int statPanelOffsetY = blockSize * 21;
        int statPanelHeight = 2 * blockSize;
        int panelWidth = 6 * this.blockSize;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        GamePanel statPanel = new GamePanel(panelX, panelY + statPanelOffsetY, panelWidth, statPanelHeight,
                panelBorderWidth, panelColor, blockSize, "Stat", fontName);
        statPanel.render(g2D);
        String allRowsStr = "Rows: " + allFullRows;
        String timeStr = "Time: " + getEllapsedTime();
        int stringHeight = blockSize - 14;
        g2D.setFont(new Font(fontName, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int scoreX = panelX + blockSize;
        int scoreY = panelY + blockSize + statPanelOffsetY + blockSize / 2;
        g2D.drawString(allRowsStr, scoreX, scoreY + stringHeight - 5);
        g2D.drawString(timeStr, scoreX, scoreY + blockSize / 2 + stringHeight - 5);
    }

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

    public void setTickAnim(boolean tick) {
        this.tickAnim = tick;
    }

    public State getState() {
        return state;
    }

    public int getLevel() {
        return gameLevel;
    }

    public void setState(State state) {
        this.state = state;
    }

    protected void setShapes(Shape currentShape, Shape nextShape) {
        this.currentShape = currentShape;
        this.nextShape = nextShape;
    }
}
