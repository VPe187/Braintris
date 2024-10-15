package hu.nye.vpe.tetris;

import java.awt.Color;

/***
 * Cell class.
 */
public class Cell {

    private Color color;
    private final int tetrominoId;
    private int score;

    private BonusType bonus;

    public Cell(int tetrominoId, Color color) {
        this.tetrominoId = tetrominoId;
        this.color = color;
    }

    public Color getColor() {
        return  this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getScore() {
        return this.score;
    }

    public int getTetrominoId() {
        return this.tetrominoId;
    }

    public int getTetrmonioQId() {
        return this.getTetrmonioQId();
    }

    public BonusType getBonus() {
        return this.bonus;
    }

    public void setBonus(BonusType bonusType) {
        this.bonus = bonusType;
    }
}
