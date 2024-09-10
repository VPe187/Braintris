package hu.nye.vpe;

import java.awt.Color;

/***
 * Cell class.
 */
public class Cell {

    private Color color;
    private int shapeId;
    private int score;

    /***
     * Bonus types.
     */
    protected enum BonusType { NONE, DBSC, RMVCLR, BOMB, UPSDWN
    }

    private BonusType bonus;

    public Cell(int shapeId, Color color) {
        this.shapeId = shapeId;
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

    public void setScore(int score) {
        this.score = score;
    }

    public int getShapeId() {
        return this.shapeId;
    }

    public void setShapeId(int shapeId) {
        this.shapeId = shapeId;
    }

    public BonusType getBonus() {
        return this.bonus;
    }

    public void setBonus(BonusType bonusType) {
        this.bonus = bonusType;
    }
}
