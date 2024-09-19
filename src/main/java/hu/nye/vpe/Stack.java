package hu.nye.vpe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameElement;

/**
 * Stack class.
 */
public class Stack implements GameElement {
    private final Color colorStackBackground = new Color(15, 15, 15);
    private final Color colorStackBorder = new Color(100, 100, 100, 100);
    private final Color helperColor = new Color(25,25,25,90);
    private final Color helperLineColor = new Color(40,40,40,40);
    private final int penaltyNoFullrows = 12;
    private final int rowOffset = 2;
    private final int stackX;
    private final int stackY;
    private final int stackW;
    private final int stackH;
    private final int blockSize = 30;
    private final int cols = 12;
    private final int rows = 24;
    private Cell[][] stackArea;
    private Shape currentShape = null;
    private Shape nextShape = null;
    private boolean upSideDown;
    private int clearBlockSize = 0;
    private boolean tickAnim;
    int yoffset = 0;
    private boolean stateDeletingRows = false;
    private boolean stateGameOver = false;
    private boolean stateLevelChanging = false;
    private boolean statePaused = false;
    private final int bonusColorAlpha = 200;
    private GameAudio audio;
    private int noFullRows;
    private int gameScore;
    private int gameLevel;
    private final int levelBonus = 20;
    private int allFullRows;
    private final int rowScore = 100;
    private final int levelChangeRows = 4;

    public Stack() {
        stackX = 2 * this.blockSize;
        stackY = 2 * this.blockSize;
        stackW = (this.cols * this.blockSize);
        stackH = ((this.rows - rowOffset) * this.blockSize);
    }

    protected void reset() {
        stackArea = new Cell[rows][cols];
        emptyStack();
        audio = new GameAudio();
        gameScore = 0;
        gameLevel = 0;
        noFullRows = 0;
        allFullRows = 0;
        stateGameOver = false;
    }

    private void emptyStack() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                stackArea[i][j] = new Cell(ShapeFactory.getInstance().getEmptyShape().getId(),
                        ShapeFactory.getInstance().getEmptyShape().getColor());
            }
        }
    }

    protected void addShapes(Shape currentShape, Shape nextShape) {
        this.currentShape = currentShape;
        this.nextShape = nextShape;
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
            if (!stateGameOver) {
                genPenaltyRows(1);
            }
        }
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
            stateDeletingRows = true;
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
                stateGameOver = true;
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
            g2D.fill3DRect(stackX - blockSize, (stackY - blockSize) + ((k - rowOffset) * blockSize), blockSize, blockSize, true);
            g2D.fill3DRect(
                    (stackX - blockSize) + ((cols + 1) * blockSize),
                    (stackY - blockSize) + ((k - rowOffset) * blockSize),
                    blockSize, blockSize,
                    true
            );
        }
    }

    private void renderStack(Graphics2D g2D) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i >= rowOffset) {
                    int i1 = stackY + ((rows + 1 - i - rowOffset) * blockSize);
                    if (stackArea[i][j].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {

                        // Deleted blocks
                        if (stackArea[i][j].getShapeId() == Shape.ShapeType.ERASED.getShapeTypeId()) {
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
                                if (clearBlockSize > 0) {
                                    if (tickAnim) {
                                        clearBlockSize--;
                                        tickAnim = false;
                                    }
                                } else {
                                    clearBlockSize = blockSize;
                                    clearRows();
                                    stateDeletingRows = false;
                                }
                            }

                            // Inserted penalty blocks
                        } else if (stackArea[i][j].getShapeId() == Shape.ShapeType.LOADED.getShapeTypeId()) {
                            g2D.setColor(Color.DARK_GRAY);
                            if (upSideDown) {
                                g2D.fill3DRect(stackX + j * blockSize, i1, blockSize, blockSize, true);
                            } else {
                                g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize, true);
                            }

                            // Ordinary blocks
                        } else {
                            g2D.setColor(stackArea[i][j].getColor());
                            if (upSideDown) {
                                g2D.fill3DRect(stackX + j * blockSize, i1, blockSize, blockSize, true);
                            } else {
                                g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize, true);
                            }
                            if (stackArea[i][j].getBonus() != Cell.BonusType.NONE) {
                                renderBonus(g2D, i, j);
                            }

                        }
                    } else {
                        if (currentShape != null && j >= currentShape.getStackCol() &&
                                j <= currentShape.getStackCol() + currentShape.getHeightPoints() - 1 && i >= currentShape.getStackRow()) {
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

    protected void clearRows() {
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
            //nextLevel();
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

    public Shape getCurrentShape() {
        return currentShape;
    }

}
