package hu.nye.vpe.tetris;

import hu.nye.vpe.gaming.GameConstans;

/**
 * Stack metric class.
 */
public class StackMetrics implements StackComponent {
    private StackManager manager;
    private double metricNumberOfHoles;
    private double[] metricColumnHeights;
    private double[] metricColumnHoles;
    private double metricColumnHeightSum;
    private double metricColumnHoleSum;
    private double metricAvgColumnHeight;
    private int metricMaxHeight;
    private double metricBumpiness;
    private double metricNearlyFullRows;
    private double metricBlockedRows;
    private double metricSurroundingHoles;
    private double metricDroppedElements;
    private double metricAvgDensity;
    private double metricAccessibleEmptyCells;
    private int[] metricHighestOccupiedCell;

    public StackMetrics() {
    }

    /**
     * Calculate metrics.
     *
     * @param stack current or simulated stack
     */
    public void calculateGameMetrics(Cell[][] stack) {
        metricBumpiness = calculateBumpiness(stack);
        metricMaxHeight = calculateMaxHeight(stack);
        metricColumnHeights = calculateColumnHeights(stack);
        metricColumnHeightSum = calculateColumnHeightSum(stack);
        metricColumnHoles = calculateColumnHoles(stack);
        metricColumnHoleSum = calculateColumnHoleSum(stack);
        metricAvgColumnHeight = calculateAverageHeightDifference(stack);
        metricSurroundingHoles = calculateHolesSurrounded(stack);
        metricNumberOfHoles = countHoles(stack);
        metricBlockedRows = countBlockedRows(stack);
        metricNearlyFullRows = countNearlyFullRows(stack);
        metricAvgDensity = calculateAverageDensity(stack);
        metricAccessibleEmptyCells = countAccessibleEmptyCells(stack);
        metricHighestOccupiedCell = calculateHighestOccupiedCells(stack);
    }

    private int countHoles(Cell[][] stack) {
        int holes = 0;
        for (int col = 0; col < GameConstans.COLS; col++) {
            boolean blockFound = false;
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (stack[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    blockFound = true;
                } else if (blockFound) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private double calculateAverageDensity(Cell[][] stack) {
        int lowestEmptyRow = findLowestEmptyRow(stack);
        int activeCells = lowestEmptyRow * GameConstans.COLS;
        int filledCells = 0;
        for (int row = GameConstans.ROWS - 1; row >= GameConstans.ROWS - lowestEmptyRow; row--) {
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (stack[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    filledCells++;
                }
            }
        }
        return activeCells > 0 ? (double) filledCells / activeCells : 0.0;
    }

    private int findLowestEmptyRow(Cell[][] stack) {
        for (int row = 0; row < GameConstans.ROWS; row++) {
            boolean isEmpty = true;
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (stack[GameConstans.ROWS - 1 - row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                return GameConstans.ROWS - 1 - row;
            }
        }
        return 0;
    }

    private int countAccessibleEmptyCells(Cell[][] stack) {
        int accessibleEmptyCells = 0;
        boolean[] columnBlocked = new boolean[GameConstans.COLS];
        int blockedColumns = 0;

        for (int row = GameConstans.ROWS - 1; row >= 0; row--) {
            for (int col = 0; col < GameConstans.COLS; col++) {
                if (!columnBlocked[col]) {
                    if (stack[row][col].getTetrominoId() == TetrominoType.EMPTY.getTetrominoTypeId()) {
                        accessibleEmptyCells++;
                    } else {
                        columnBlocked[col] = true;
                        blockedColumns++;
                    }
                }
            }
            if (blockedColumns == GameConstans.COLS) {
                break;
            }
        }
        return accessibleEmptyCells;
    }

    private double calculateBumpiness(Cell[][] stack) {
        int[] columnHeights = new int[stack[0].length];

        for (int col = 0; col < stack[0].length; col++) {
            for (int row = 0; row < stack.length; row++) {
                if (stack[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    columnHeights[col] = stack.length - row;
                    break;
                }
            }
        }

        double bumpiness = 0;
        for (int i = 0; i < columnHeights.length - 1; i++) {
            int diff = Math.abs(columnHeights[i] - columnHeights[i + 1]);
            bumpiness += diff;
        }

        return bumpiness;
    }

    private int calculateMaxHeight(Cell[][] stack) {
        int maxHeight = 0;
        for (int col = 0; col < GameConstans.COLS; col++) {
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (stack[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    int height = GameConstans.ROWS - row;
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                    break;
                }
            }
        }
        return maxHeight;
    }

    private int[] calculateHighestOccupiedCells(Cell[][] stack) {
        int[] highestOccupied = new int[GameConstans.COLS];
        for (int col = 0; col < GameConstans.COLS; col++) {
            highestOccupied[col] = -1;
            for (int row = 0; row < GameConstans.ROWS; row++) {
                if (stack[row][col].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    highestOccupied[col] = row;
                    break;
                }
            }
        }
        return highestOccupied;
    }

    private double countNearlyFullRows(Cell[][] stack) {
        int nearlyFullRows = 0;
        boolean foundNonEmptyRow = false;

        for (int i = GameConstans.ROWS - 1; i >= 0; i--) {
            int filledCells = 0;
            for (int j = 0; j < GameConstans.COLS; j++) {
                if (stack[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    filledCells++;
                }
            }

            if (filledCells > 0) {
                foundNonEmptyRow = true;
            }

            if (foundNonEmptyRow) {
                if (filledCells >= GameConstans.COLS - 1 && filledCells < GameConstans.COLS) {
                    nearlyFullRows++;
                }

                if (filledCells == 0) {
                    break;
                }
            }
        }
        return nearlyFullRows;
    }

    private int countBlockedRows(Cell[][] stack) {
        int blockedRows = 0;
        boolean[] hasHole = new boolean[GameConstans.COLS];

        for (int i = GameConstans.ROWS - 1; i >= 0; i--) {
            boolean rowBlocked = false;
            boolean rowHasBlock = false;

            for (int j = 0; j < GameConstans.COLS; j++) {
                if (stack[i][j].getTetrominoId() == TetrominoType.EMPTY.getTetrominoTypeId()) {
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

    private double[] calculateColumnHeights(Cell[][] stack) {
        double[] columnHeights = new double[GameConstans.COLS];
        for (int j = 0; j < GameConstans.COLS; j++) {
            for (int i = 0; i < GameConstans.ROWS; i++) {
                if (stack[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    columnHeights[j] = GameConstans.ROWS - i;
                    break;
                }
            }
        }
        return columnHeights;
    }

    private double calculateColumnHeightSum(Cell[][] stack) {
        double sum = 0;
        double[] columnHeights = calculateColumnHeights(stack);
        for (int i = 0; i < columnHeights.length; i++) {
            sum += columnHeights[i];
        }
        return sum;
    }

    private double[] calculateColumnHoles(Cell[][] stack) {
        double[] columnHoles = new double[GameConstans.COLS];

        for (int j = 0; j < GameConstans.COLS; j++) {
            boolean foundFirstPiece = false;
            int holes = 0;

            for (int i = 0; i < GameConstans.ROWS; i++) {
                if (stack[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    foundFirstPiece = true;
                } else if (foundFirstPiece &&
                        stack[i][j].getTetrominoId() == TetrominoType.EMPTY.getTetrominoTypeId()) {
                    holes++;
                }
            }
            columnHoles[j] = holes;
        }

        return columnHoles;
    }

    private double calculateColumnHoleSum(Cell[][] stack) {
        double sum = 0;
        double[] columnHoles = calculateColumnHoles(stack);
        for (int i = 0; i < columnHoles.length; i++) {
            sum += columnHoles[i];
        }
        return sum;
    }

    private double[] calculateHeightDifferences(Cell[][] stack) {
        double[] columnHeights = calculateColumnHeights(stack);
        double[] heightDifferences = new double[GameConstans.COLS - 1];
        for (int j = 0; j < GameConstans.COLS - 1; j++) {
            heightDifferences[j] = Math.abs(columnHeights[j] - columnHeights[j + 1]);
        }
        return heightDifferences;
    }

    private double calculateAverageHeightDifference(Cell[][] stack) {
        double[] columnHeights = calculateColumnHeights(stack);
        double totalDifference = 0;
        int comparisons = 0;
        for (int i = 0; i < columnHeights.length - 1; i++) {
            totalDifference += Math.abs(columnHeights[i] - columnHeights[i + 1]);
            comparisons++;
        }
        double averageDifference = (comparisons > 0) ? totalDifference / comparisons : 0;
        return averageDifference / GameConstans.ROWS;
    }

    private int calculateHolesSurrounded(Cell[][] stack) {
        int surroundings = 0;
        for (int j = 0; j < GameConstans.COLS; j++) {
            boolean blockAbove = false;
            for (int i = 0; i < GameConstans.ROWS; i++) {
                if (stack[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                    blockAbove = true;
                } else if (blockAbove) {
                    if (j > 0 && stack[i][j - 1].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                        surroundings++;
                    }
                    if (j < GameConstans.COLS - 1 && stack[i][j + 1].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
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

    public double getMetricSurroundedHoles() {
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

    public double getMetricAccessibleEmptyCells() {
        return metricAccessibleEmptyCells;
    }

    public int[] getMetricHighestOccupiedCell() {
        return metricHighestOccupiedCell;
    }

    public double getMetricSurroundingHoles() {
        return metricSurroundingHoles;
    }

    public double[] getMetricColumnHoles() {
        return metricColumnHoles;
    }

    public double getMetricColumnHeightSum() {
        return metricColumnHeightSum;
    }

    public double getMetricColumnHoleSum() {
        return metricColumnHoleSum;
    }

    @Override
    public void initializeStackComponents(StackUI stackUI, StackManager manager, StackMetrics metrics) {
        this.manager = manager;
    }
}
