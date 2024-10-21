package hu.nye.vpe.tetris;

/**
 * Metric class.
 */
public class Metric {
    double bumpiness;
    double[] columnHeights;
    int maxHeight;
    double avgColumnHeight;
    double surroundingHoles;
    double numberOfHoles;
    double blockedRows;
    double nearlyFullRows;
    double avgDensity;

    public Metric() {
    }

    public double getBumpiness() {
        return bumpiness;
    }

    public void setBumpiness(double bumpiness) {
        this.bumpiness = bumpiness;
    }

    public double[] getColumnHeights() {
        return columnHeights;
    }

    public void setColumnHeights(double[] columnHeights) {
        this.columnHeights = columnHeights;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getAvgColumnHeight() {
        return avgColumnHeight;
    }

    public void setAvgColumnHeight(double avgColumnHeight) {
        this.avgColumnHeight = avgColumnHeight;
    }

    public double getSurroundingHoles() {
        return surroundingHoles;
    }

    public void setSurroundingHoles(double surroundingHoles) {
        this.surroundingHoles = surroundingHoles;
    }

    public double getNumberOfHoles() {
        return numberOfHoles;
    }

    public void setNumberOfHoles(double numberOfHoles) {
        this.numberOfHoles = numberOfHoles;
    }

    public double getBlockedRows() {
        return blockedRows;
    }

    public void setBlockedRows(double blockedRows) {
        this.blockedRows = blockedRows;
    }

    public double getNearlyFullRows() {
        return nearlyFullRows;
    }

    public void setNearlyFullRows(double nearlyFullRows) {
        this.nearlyFullRows = nearlyFullRows;
    }

    public double getAvgDensity() {
        return avgDensity;
    }

    public void setAvgDensity(double avgDensity) {
        this.avgDensity = avgDensity;
    }
}
