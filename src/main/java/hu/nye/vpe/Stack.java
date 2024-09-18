package hu.nye.vpe;

import hu.nye.vpe.gaming.GameElement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Stack class.
 */
public class Stack implements GameElement {
    private final Color colorStackBackground = new Color(15, 15, 15);
    private final Color colorStackBorder = new Color(100, 100, 100, 100);
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
    int yOffset = 0;
    private boolean stateDeletingRows = false;
    private boolean stateGameOver = false;
    private boolean stateLevelChanging = false;
    private boolean statePaused = false;
    private final int bonusColorAlpha = 200;

    public Stack() {
        stackX = 2 * this.blockSize;
        stackY = 2 * this.blockSize;
        stackW = (this.cols * this.blockSize);
        stackH = ((this.rows - rowOffset) * this.blockSize);
    }

    protected void reset() {
        stackArea = new Cell[rows][cols];
        emptyStack();
    }

    private void emptyStack() {
        for (int i=0; i<rows; i++) {
            for (int j=0; j<cols; j++) {
                stackArea[i][j]=new Cell(ShapeFactory.getInstance().getEmptyShape().getId(), ShapeFactory.getInstance().getEmptyShape().getColor());
            }
        }
    }

    protected void addShapes(Shape currentShape, Shape nextShape ) {
        this.currentShape = currentShape;
        this.nextShape = nextShape;
    }

    private boolean checkShapeIsDown() {
        for (int i = 0; i < currentShape.getPixels().length; i++) {
            for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    if ((currentShape.getStackRow()+i+1 > stackArea.length-1) || (currentShape.getStackCol()+j > stackArea[i].length-1) || (stackArea[currentShape.getStackRow()+i+1][currentShape.getStackCol()+j]).getShapeId() != 0) {
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
        for (int i = 0; i< currentShape.getPixels().length; i++) {
            for (int j = 0; j< currentShape.getPixels()[i].length; j++) {
                if (currentShape.getPixels()[i][j] != 0) {
                    stackArea[currentShape.getStackRow() + i][currentShape.getStackCol() + j] = new Cell(ShapeFactory.getInstance().getEmptyShape().getId(), ShapeFactory.getInstance().getEmptyShape().getColor());
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
            currentShape.setRowPosition(currentShape.getStackRow()+1);
            putShape();
        } else {
            //itemFalled();
        }
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
            for (int i=0; i<rows; i++) {
                for (int j=0; j<cols; j++) {
                    if (i>=rowOffset) {
                        if (stackArea[i][j].getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId()) {

                            // Deleted blocks
                            if (stackArea[i][j].getShapeId() == Shape.ShapeType.ERASED.getShapeTypeId()) {
                                for (int k=0; k<cols; k++) {
                                    g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(), stackArea[i][j].getColor().getBlue(), clearBlockSize/2));
                                    if (upSideDown) {
                                        g2D.fill3DRect(stackX + j * blockSize, stackY + ((rows + 1 - i - rowOffset) * blockSize), blockSize, blockSize, true);
                                    } else {
                                        g2D.fill3DRect(stackX + j * blockSize, stackY + ((i - rowOffset) * blockSize), blockSize, blockSize, true);
                                    }
                                    if (upSideDown) {
                                        g2D.fill3DRect(stackX+j*blockSize, stackY + ((rows + 1 - i - rowOffset) * blockSize), blockSize, blockSize, false);
                                    } else {
                                        g2D.fill3DRect(stackX+j*blockSize, stackY + ((i - rowOffset) * blockSize), blockSize, blockSize, false);
                                    }
                                    g2D.setColor(stackArea[i][j].getColor());
                                    if (upSideDown) {
                                        g2D.fill3DRect((stackX+j*blockSize)+(blockSize-clearBlockSize), (stackY + ((rows + 1 - i - rowOffset) * blockSize)) + (blockSize-clearBlockSize), blockSize-((blockSize-clearBlockSize)*2), blockSize-((blockSize-clearBlockSize)*2), true);
                                    } else {
                                        g2D.fill3DRect((stackX + j * blockSize) + (blockSize - clearBlockSize), (stackY + ((i - rowOffset) * blockSize)) + (blockSize - clearBlockSize), blockSize - ((blockSize - clearBlockSize) * 2), blockSize - ((blockSize - clearBlockSize) * 2), true);
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
                                    g2D.fill3DRect(stackX+j*blockSize, stackY + (rows + 1 - i - rowOffset) * blockSize, blockSize, blockSize, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize, true);
                                }

                                // Ordinary blocks
                            } else {
                                g2D.setColor(stackArea[i][j].getColor());
                                if (upSideDown) {
                                    g2D.fill3DRect(stackX + j * blockSize, stackY + (rows + 1 - i - rowOffset) * blockSize, blockSize, blockSize, true);
                                } else {
                                    g2D.fill3DRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize, true);
                                }
                                if (stackArea[i][j].getBonus() != Cell.BonusType.NONE) {
                                    renderBonus(g2D, i, j);
                                }

                            }
                        } else {
                            if (currentShape!= null && j >= currentShape.getStackCol() && j <= currentShape.getStackCol()+currentShape.getHeightPoints()-1 && i >= currentShape.getStackRow()) {
                                g2D.setColor(new Color(currentShape.getColor().getRed(), currentShape.getColor().getGreen(), currentShape.getColor().getBlue(), 20));
                                if (upSideDown) {
                                    g2D.fillRect(stackX + j * blockSize, stackY + (rows + 1 - i - rowOffset) * blockSize, blockSize, blockSize);
                                } else {
                                    g2D.fillRect(stackX + j * blockSize, stackY + (i - rowOffset) * blockSize, blockSize, blockSize);
                                }
                            }
                        }
                    }
                }
            //renderHelper(g2D);
        }
    }

    protected void clearRows() {
        int fullRows;
        fullRows = 0;
        for (int i=0; i<stackArea.length; i++) {
            boolean rowFull = true;
            for (Cell cell : stackArea[i]) {
                if (cell.getShapeId() != Shape.ShapeType.ERASED.getShapeTypeId()) {
                    rowFull=false;
                }
            }
            if (rowFull) {
                fullRows++;
                for (int j=0; j<stackArea[i].length; j++)
                {
                    stackArea[i][j] = new Cell(ShapeFactory.getInstance().getEmptyShape().getId(), ShapeFactory.getInstance().getEmptyShape().getColor());
                }
                for (int k=i; k>0; k--) {
                    System.arraycopy(stackArea[k-1], 0, stackArea[k], 0, stackArea[k].length);
                }
            }
        }
        /*
        gameScore+=fullRows*ROW_SCORE;
        allFullRows+=fullRows;
        noFullRows = Math.max(0, noFullRows -= fullRows*2);
        if (allFullRows >= gameLevel* LEVEL_CHANGE_ROWS) {
            nextLevel();
        }
         */
    }

    private void renderHelper(Graphics2D g2D) {
        if (currentShape!=null) {
            yOffset = howFarFromDown()-rowOffset+1;
            for (int i = 0; i < currentShape.getPixels().length; i++) {
                for (int j = 0; j < currentShape.getPixels()[i].length; j++) {
                    if (currentShape.getPixels()[i][j] != 0) {
                        g2D.setColor(new Color(currentShape.getColor().getRed(), currentShape.getColor().getGreen(), currentShape.getColor().getBlue(),20 ));
                        if (upSideDown) {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * blockSize, stackY + ((rows+1)-(yOffset + (i + currentShape.getStackRow() + rowOffset))) * blockSize, blockSize, blockSize, true);
                        } else {
                            g2D.fill3DRect(stackX + (j + currentShape.getStackCol()) * blockSize, stackY + (yOffset + (i + currentShape.getStackRow() - rowOffset)) * blockSize, blockSize, blockSize, true);
                        }
                    }
                }
            }
        }
    }

    private int howFarFromDown() {
        boolean down = false;
        int p = 0;
        while (!down) {
            //removeShape();
            p++;
            //down = shapeIsDownAfter(p);
            //putShape();
        }
        return p;
    }

    private void renderBonus(Graphics2D g2D, int i, int j) {
        g2D.setColor(Color.BLACK);
        if (upSideDown) {
            g2D.fillOval(stackX + (j * blockSize) + (blockSize / 4), stackY + (rows + 1 - i - rowOffset) * blockSize + (blockSize / 4), blockSize -(blockSize / 4) * 2, blockSize- (blockSize / 4) * 2);
        } else {
            g2D.fillOval(stackX + (j * blockSize) + (blockSize / 4), stackY + (i - rowOffset) * blockSize + (blockSize / 4), blockSize -(blockSize / 4) * 2, blockSize- (blockSize / 4) * 2);
        }
        g2D.setColor(new Color(stackArea[i][j].getColor().getRed(), stackArea[i][j].getColor().getGreen(), stackArea[i][j].getColor().getBlue(), bonusColorAlpha));
        if (upSideDown) {
            g2D.fillOval(stackX + (j * blockSize) + (blockSize / 4) + 4, stackY + (rows + 1 - i - rowOffset) * blockSize + (blockSize / 4) + 4, blockSize - (blockSize / 4 + 4) * 2, blockSize - (blockSize / 4 + 4) * 2);
        } else {
            g2D.fillOval(stackX + (j * blockSize) + (blockSize / 4) + 4, stackY + (i - rowOffset) * blockSize + (blockSize / 4) + 4, blockSize - (blockSize / 4 + 4) * 2, blockSize - (blockSize / 4 + 4) * 2);
        }
    }



}
