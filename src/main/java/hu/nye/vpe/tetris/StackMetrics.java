package hu.nye.vpe.tetris;

import hu.nye.vpe.gaming.GameConstans;

public class StackMetrics implements StackComponent {
    private StackManager manager;
    private double metricNumberOfHoles;
    private double[] metricColumnHeights;
    private double metricAvgColumnHeight;
    private int metricMaxHeight;
    private double metricBumpiness;
    private double metricNearlyFullRows;
    private double metricBlockedRows;
    private double metricSurroundingHoles;
    private double metricDroppedElements;
    private double metricAvgDensity;

    public StackMetrics() {
    }

    public void calculateGameMetrics() {
        metricBumpiness = calculateBumpiness();
        metricMaxHeight = calculateMaxHeight();
        metricColumnHeights = calculateColumnHeights();
        metricAvgColumnHeight = calculateAverageHeightDifference();
        metricSurroundingHoles = calculateHolesSurroundings();
        metricNumberOfHoles = countHoles();
        metricBlockedRows = countBlockedRows();
        metricNearlyFullRows = countNearlyFullRows();
        metricAvgDensity = calculateAverageDensity();
    }

    private int countHoles() {
        if (manager.getCurrentTetromino() != null) {
            manager.removeTetromino();
        }
        int holes = 0;
        for (int col = 0; col < GameConstans.COLS; col++) {
            boolean blockFound = false;
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    blockFound = true;
                } else if (blockFound) {
                    // If we have already found a block in this column and now find an empty cell, it is a hole.
                    holes++;
                }
            }
        }
        if (manager.getCurrentTetromino() != null) {
            manager.putTetromino();
        }
        return holes;
    }

    private double calculateAverageDensity() {
        int lowestEmptyRow = findLowestEmptyRow();
        int activeCells = lowestEmptyRow * GameConstans.COLS; // Az összes cella a legalacsonyabb üres sor alatt
        int filledCells = 0;
        for (int row = GameConstans.ROWS - 1; row >= GameConstans.ROWS - lowestEmptyRow; row--) {
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    filledCells++;
                }
            }
        }
        return activeCells > 0 ? (double) filledCells / activeCells : 0.0;
    }

    private int findLowestEmptyRow() {
        for (int row = 0; row < GameConstans.ROWS; row++) {
            boolean isEmpty = true;
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (manager.getStackArea()[GameConstans.ROWS - 1 - row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                return row;
            }
        }
        return GameConstans.ROWS;
    }

    private int countAccessibleEmptyCells() {
        int accessibleEmptyCells = 0;
        boolean[] columnBlocked = new boolean[GameConstans.COLS];
        for (int row = 0; row < GameConstans.ROWS; row++) {
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (manager.getStackArea()[row][col].getTetrominoId() == TetrominoType.EMPTY.getTetrominoTypeId() && !columnBlocked[col]) {
                    accessibleEmptyCells++;
                } else if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    columnBlocked[col] = true;
                }
            }
        }
        return accessibleEmptyCells;
    }

    private double calculateBumpiness() {
        if (manager.getCurrentTetromino() != null) {
            manager.removeTetromino();
        }
        int[] columnHeights = new int[GameConstans.COLS];
        // Calculate columns height.
        for (int col = 0; col < GameConstans.COLS; col++) {
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    columnHeights[col] = GameConstans.ROWS - row;
                    break;
                }
            }
        }
        // Calculate bumpiness
        double bumpiness = 0;
        for (int i = 0; i < GameConstans.COLS - 1; i++) {
            bumpiness += Math.abs(columnHeights[i] - columnHeights[i + 1]);
        }
        if (manager.getCurrentTetromino() != null) {
            manager.putTetromino();
        }
        return bumpiness;
    }

    private int calculateMaxHeight() {
        if (manager.getCurrentTetromino() != null) {
            manager.removeTetromino();
        }
        int maxHeight = 0;
        for (int col = 0; col < GameConstans.COLS; col++) {
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    int height = GameConstans.ROWS - row;
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                    break;
                }
            }
        }
        if (manager.getCurrentTetromino() != null) {
            manager.putTetromino();
        }
        return maxHeight;
    }

    private int[] getHighestOccupiedCells() {
        if (manager.getCurrentTetromino() != null) {
            manager.removeTetromino();
        }
        int[] highestOccupied = new int[GameConstans.COLS];
        for (int col = 0; col < GameConstans.COLS; col++) {
            highestOccupied[col] = -1; // Initialize with -1 to indicate an empty column
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (manager.getStackArea()[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    highestOccupied[col] = row;
                    break; // Stop at the first occupied cell found
                }
            }
        }
        if (manager.getCurrentTetromino() != null) {
            manager.putTetromino();
        }
        return highestOccupied;
    }

    private double countNearlyFullRows() {
        int nearlyFullRows = 0;
        boolean foundNonEmptyRow = false;

        for (int i = GameConstans.ROWS - 1; i >= 0; i--) {  // Alulról felfelé haladunk
            int filledCells = 0;
            for (int j = 0; j < GameConstans.COLS; j++) {
                if (manager.getStackArea()[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    filledCells++;
                }
            }

            if (filledCells > 0) {
                foundNonEmptyRow = true;
            }

            if (foundNonEmptyRow) {
                if (filledCells >= GameConstans.COLS - 3 && filledCells < GameConstans.COLS) {  // 9 - 11 kitöltött cella 12-ből
                    nearlyFullRows++;
                }

                if (filledCells == 0) {  // Ha üres sort találunk az első nem üres sor után, kilépünk
                    break;
                }
            }
        }
        return nearlyFullRows;
    }

    private int countBlockedRows() {
        int blockedRows = 0;
        boolean[] hasHole = new boolean[GameConstans.COLS];

        for (int i = GameConstans.ROWS - 1; i >= 0; i--) {
            boolean rowBlocked = false;
            boolean rowHasBlock = false;

            for (int j = 0; j < GameConstans.COLS; j++) {
                if (manager.getStackArea()[i][j].getTetrominoId() == TetrominoType.EMPTY.getTetrominoTypeId()) {
                    hasHole[j] = true;
                } else {
                    rowHasBlock = true;
                    if (hasHole[j]) {
                        rowBlocked = true;
                    }
                }
            }

            if (rowBlocked && rowHasBlock) {
                blockedRows++;
            }
        }

        return blockedRows;
    }

    private double[] calculateColumnHeights() {
        double[] columnHeights = new double[GameConstans.COLS];
        for (int j = 0; j < GameConstans.COLS; j++) {
            for (int i = 0; i < GameConstans.ROWS; i++) {
                if (manager.getStackArea()[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    columnHeights[j] = GameConstans.ROWS - i;
                    break;
                }
            }
        }
        return columnHeights;
    }

    private double[] calculateHeightDifferences() {
        double[] columnHeights = calculateColumnHeights();
        double[] heightDifferences = new double[GameConstans.COLS - 1];
        for (int j = 0; j < GameConstans.COLS - 1; j++) {
            heightDifferences[j] = Math.abs(columnHeights[j] - columnHeights[j + 1]);
        }
        return heightDifferences;
    }

    private double calculateAverageHeightDifference() {
        double[] columnHeights = calculateColumnHeights();
        double totalDifference = 0;
        int comparisons = 0;
        for (int i = 0; i < columnHeights.length - 1; i++) {
            totalDifference += Math.abs(columnHeights[i] - columnHeights[i + 1]);
            comparisons++;
        }
        double averageDifference = (comparisons > 0) ? totalDifference / comparisons : 0;
        return averageDifference / GameConstans.ROWS;
    }

    private int calculateHolesSurroundings() {
        int surroundings = 0;
        for (int j = 0; j < GameConstans.COLS; j++) {
            boolean blockAbove = false;
            for (int i = 0; i < GameConstans.ROWS; i++) {
                if (manager.getStackArea()[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    blockAbove = true;
                } else if (blockAbove) {
                    if (j > 0 && manager.getStackArea()[i][j - 1].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                        surroundings++;
                    }
                    if (j < GameConstans.COLS - 1 && manager.getStackArea()[i][j + 1].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                        surroundings++;
                    }
                }
            }
        }
        return surroundings;
    }



    public double getMetricNumberOfHoles() {
        return metricNumberOfHoles;
    }

    public double[] getMetricColumnHeights() {
        return metricColumnHeights;
    }

    public double getMetricAvgColumnHeight() {
        return metricAvgColumnHeight;
    }

    public int getMetricMaxHeight() {
        return metricMaxHeight;
    }

    public double getMetricBumpiness() {
        return metricBumpiness;
    }

    public double getMetricNearlyFullRows() {
        return metricNearlyFullRows;
    }

    public double getMetricBlockedRows() {
        return metricBlockedRows;
    }

    public double getMetricSurroundingHoles() {
        return metricSurroundingHoles;
    }

    public double getMetricDroppedElements() {
        return metricDroppedElements;
    }

    public double getMetricAvgDensity() {
        return metricAvgDensity;
    }

    public void setMetricDroppedElements(double metricDroppedElements) {
        this.metricDroppedElements = metricDroppedElements;
    }

    @Override
    public void initializeStackComponents(StackUI stackUI, StackManager manager, StackMetrics metrics) {
        this.manager = manager;
    }
}
