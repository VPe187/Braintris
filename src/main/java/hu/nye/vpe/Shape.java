package hu.nye.vpe;

import java.awt.Color;

/**
 * Shape class.
 */
public class Shape {

    private int[][] pixels = null;
    private Color color;
    private final int id;
    private final int score;
    private final int weight;
    private int rowPosition;
    private int colPosition;

    /**
     * Shape type enum.
     */
    protected enum ShapeType {
        COMMON(100), EMPTY(0), ERASED(99), LOADED(90), HIDDEN(80);
        private final int typeId;

        ShapeType(int typeId) {
            this.typeId = typeId;
        }

        public int getShapeTypeId() {
            return typeId;
        }
    }

    public Shape(int id, int score, Color color, int weight, int[][] pixels) {
        this.id = id;
        this.score = score;
        this.color = color;
        this.weight = weight;
        this.pixels = pixels;
    }

    public int getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public int getWeight() {
        return weight;
    }

    public int[][] getPixels() {
        return pixels;
    }

    public int getStackRow() {
        return rowPosition;
    }

    public int getStackCol() {
        return colPosition;
    }

    /**
     * Get pixels numbers.
     *
     * @return int
     */
    public int getPixelsNumber() {
        int pixelNumber = 0;
        for (int i = 0; i < this.pixels.length; i++) {
            for (int j = 0; j < this.pixels[i].length; j++) {
                if (this.pixels[i][j] != 0) {
                    pixelNumber++;
                }
            }
        }
        return pixelNumber;
    }

    /**
     * Rotate right.
     */
    public void rotateRight() {
        int x = getPixels().length;
        int y = getPixels()[0].length;
        int[][] newPixels = new int[y][x];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                newPixels[((y - 1) - j)][i] = this.pixels[i][j];
            }
        }
        this.pixels = newPixels;
    }

    /**
     * Get width.
     *
     * @return int
     *
     */
    public int getWidth() {
        int width = 0;
        boolean foundPixel;
        for (int i = 0; i < getPixels().length; i++) {
            foundPixel = false;
            for (int j = 0; j < getPixels()[i].length; j++) {
                if (this.pixels[i][j] != 0) {
                    foundPixel = true;
                }
            }
            if (foundPixel) {
                width += 1;
            }
        }
        return width;
    }

    /**
     * Get height.
     *
     * @return int
     *
     */
    public int getHeight() {
        int height = 0;
        boolean foundPixel;
        for (int j = 0; j < getPixels()[0].length; j++) {
            foundPixel = false;
            for (int i = 0; i < getPixels().length; i++) {
                if (this.pixels[i][j] != 0) {
                    foundPixel = true;
                }
            }
            if (foundPixel) {
                height += 1;
            }
        }
        return height;
    }

    public Color getColor() {
        return this.color;
    }

    public void setRowPosition(int stackRow) {
        this.rowPosition = stackRow;
    }

    public void setColPosition(int stackCol) {
        this.colPosition = stackCol;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Get width points.
     *
     * @return int
     *
     */
    public int getWidthPoints() {
        int width = 0;
        boolean foundPixel;
        for (int i = 0; i < getPixels().length; i++) {
            foundPixel = false;
            for (int j = 0; j < getPixels()[i].length; j++) {
                if (this.pixels[i][j] != 0) {
                    foundPixel = true;
                }
            }
            if (foundPixel) {
                width += 1;
            }
        }
        return width;
    }

    /**
     * Get height points.
     *
     * @return int
     *
     */
    public int getHeightPoints() {
        int height = 0;
        boolean foundPixel;
        for (int j = 0; j < getPixels()[0].length; j++) {
            foundPixel = false;
            for (int i = 0; i < getPixels().length; i++) {
                if (this.pixels[i][j] != 0) {
                    foundPixel = true;
                }
            }
            if (foundPixel) {
                height += 1;
            }
        }
        return height;
    }

    public void setPixelBonus(int i, int j) {
        this.pixels[i][j] = 2;
    }

    @Override
    public String toString() {
        String rs = "";
        for (int i = 0; i < this.pixels.length; i++) {
            for (int j = 0; j < this.pixels[i].length; j++) {
                rs += pixels[i][j] + " ";
            }
            rs += "\n";
        }
        return rs;
    }

}
