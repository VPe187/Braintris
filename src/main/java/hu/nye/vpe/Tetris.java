package hu.nye.vpe;

import java.awt.Graphics2D;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;
import hu.nye.vpe.nn.InputNormalizer;
import hu.nye.vpe.nn.NeuralNetworkQLearning;

/**
 * Tetris class.
 */
public class Tetris {
    private static final int ROWS = 24;
    private static final int COLS = 12;
    //private static final int FEED_DATA_SIZE = ROWS * COLS + 32 + 37;
    private static final int FEED_DATA_SIZE = 30;
    private static final int OUTPUT_NODES = 4;
    private static final int SHAPE_SIZE = 4 * 4;
    private static final long DROP_SPEED = 10L;
    private final long speed = 1000L;
    private final long learningSpeed = 10L;
    private Shape nextShape = null;
    private static final ShapeFactory sf = ShapeFactory.getInstance();
    private final Stack stack;
    private GameTimeTicker tickDown;
    private final GameTimeTicker tickBackground;
    private final GameTimeTicker tickControl;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private static final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private boolean musicOn = true;
    private final boolean learning;
    private NeuralNetworkQLearning brain;
    private double[] lastState;
    private int lastAction;

    private double lastFullRows = 0;
    private double lastNearlyFullRows = 0;
    private double lastDroppedElements = 0;
    private double lastAvgDensity;

    private double lastNumberofHoles = 0;
    private double lastAvgColumnHeights = 0;
    private double lastMaxHeight = 0;
    private double lastBumpiness = 0;
    private double lastBlockedRows = 0;
    private double lastSurroundingHoles = 0;

    public Tetris(int width, int height, GameInput gameInput, boolean learning) {
        stack = new Stack(learning);
        tickBackground = new GameTimeTicker(80);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        this.gameInput = gameInput;
        this.learning = learning;
        if (this.learning) {
            try {
                brain = NeuralNetworkQLearning.loadFromFile();
            } catch (Exception e) {
                System.out.println("Creating new Q-Learning Neural Network");
                brain = new NeuralNetworkQLearning(FEED_DATA_SIZE, OUTPUT_NODES);
            }
        }
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack.start();
        stack.nextIteration();
        if (learning) {
            stack.setCurrentSpeed(learningSpeed);
        } else {
            stack.setCurrentSpeed(speed);
        }
        GameColorPalette.getInstance().setRandomPalette();
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        tickDown = new GameTimeTicker(stack.getCurrentSpeed());
        nextShape = createNextShape();
        if (!learning) {
            gameAudio.musicBackgroundPlay();
        }
        musicOn = true;
        nextShape();
        lastAction = -1;
    }

    private void nextShape() {
        Shape currentShape = nextShape;
        nextShape = createNextShape();
        stack.setShapes(currentShape, nextShape);
    }

    private Shape createNextShape() {
        Shape nb = sf.getRandomShape(-1);
        if (nb != null) {
            nb.setColPosition((COLS / 2) - 2);
            nb.setRowPosition(0);
            nb.rotateRight();
        }
        return nb;
    }

    /**
     * Update.
     */
    public void update() {
        if (tickBackground.tick()) {
            starField.update();
        }
        if (tickControl.tick()) {
            handleInput();
        }
        if (tickDown.tick()) {
            if (stack.getState() != State.DELETINGROWS) {
                if (stack.getLevel() == 0) {
                    stack.nextLevel();
                }
                if (stack.getState() != State.GAMEOVER && stack.getState() != State.PAUSED && stack.getState() != State.CHANGINGLEVEL) {
                    if (stack.getCurrentShape() == null) {
                        nextShape();
                    } else {
                        if (stack.moveShapeDown()) {
                            if (learning) {
                                double[] currentState = getFeedData();
                                double reward = calculateReward();
                                //System.out.printf("%.2f ", reward);
                                brain.learn(lastState, lastAction, reward, currentState, false);
                                lastState = currentState;
                            }
                        }
                    }
                }
            }
        }
        if (stack.getState() == State.GAMEOVER) {
            if (learning) {
                /*
                System.out.printf("%nHoles: %.0f, S.holes: %.0f, Height: %d, Avg.height: %.2f, Bumpiness: %.2f, " +
                                "Blocked rows: %.0f, Nearly full rows: %.0f, Dropped: %.0f%n",
                        stack.getNumberofHoles(), stack.getSurroundingHoles(), stack.getMaxHeight(),
                        stack.getAvgColumnHeights(), stack.getBumpiness(), stack.getBlockedRows(),
                        stack.getNearlyFullRows(), stack.getDroppedElements());
                 */
                double[] currentState = getFeedData();
                double reward = calculateFinishReward();
                brain.learn(lastState, lastAction, reward, currentState, true);
                lastState = currentState;
                try {
                    brain.saveToFile();
                } catch (Exception e) {
                    System.out.println("Error saving Q-Learning Neural Network: " + e.getMessage());
                }
                start();
            }
        }
    }

    private void handleInput() {
        if (gameInput.letterP()) {
            if (stack.getState() == State.RUNNING) {
                stack.setState(State.PAUSED);
            } else if (stack.getState() == State.PAUSED) {
                stack.setState(State.RUNNING);
            }
            return;
        }
        if (gameInput.letterR()) {
            start();
            return;
        }
        if (stack.getState() == State.RUNNING && stack.getCurrentShape() != null) {
            if (learning) {
                double[] currentState = getFeedData();
                int action = brain.selectAction(currentState);
                //System.out.println(action);
                lastState = currentState;
                lastAction = action;
                switch (action) {
                    case 0:
                        stack.moveShapeLeft();
                        break;
                    case 1:
                        stack.moveShapeRight();
                        break;
                    case 2:
                        stack.rotateShapeRight();
                        break;
                    case 3:
                        tickDown.setPeriodMilliSecond(DROP_SPEED);
                        break;
                    default:
                        tickDown.setPeriodMilliSecond(stack.getCurrentSpeed());
                        break;
                }
            } else {
                if (gameInput.left()) {
                    stack.moveShapeLeft();
                }
                if (gameInput.right()) {
                    stack.moveShapeRight();
                }
                if (gameInput.space()) {
                    stack.rotateShapeRight();
                }
                if (gameInput.downRepeat()) {
                    tickDown.setPeriodMilliSecond(DROP_SPEED);
                } else {
                    tickDown.setPeriodMilliSecond(stack.getCurrentSpeed());
                }
                if (gameInput.letterM()) {
                    if (musicOn) {
                        gameAudio.musicBackgroundStop();
                        musicOn = false;
                    } else {
                        gameAudio.musicBackgroundPlay();
                        musicOn = true;
                    }
                }
            }
        } else {
            gameInput.clearBuffer();
            if (learning) {
                tickDown.setPeriodMilliSecond(learningSpeed);
            } else {
                tickDown.setPeriodMilliSecond(speed);
            }
        }
    }

    /**
     * Render.
     *
     * @param g2d Graphics2D
     */
    public void render(Graphics2D g2d) {
        starField.render(g2d);
        stack.setTickAnim(tickAnim.tick());
        stack.render(g2d);
    }

    private double[] getFeedData() {
        double[] feedData = new double[FEED_DATA_SIZE];
        int k = 0;
        /*
        // Aktuális és következő alakzat
        if (stack.getCurrentShape() != null) {
            double[] currentShapeData = ShapeFactory.getInstance().shapeToArray(stack.getCurrentShape());
            double[] nextShapeData = ShapeFactory.getInstance().shapeToArray(nextShape);
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = currentShapeData[i];
            }
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = nextShapeData[i];
            }
        } else {
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = 0.0;
            }
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = 0.0;
            }
        }
         */
        // Oszlopmagasságok
        double[] columnHeights = stack.getColumnHeights();
        for (double columnHeight : columnHeights) {
            feedData[k++] = columnHeight;
        }

        // Aktuális elem
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getQid();
        } else {
            feedData[k++] = -1;
        }

        // Aktuális elem x és y poziciója
        if (stack.getCurrentShape() != null) {
            feedData[k++] = (double) stack.getCurrentShape().getStackCol() / COLS;
            feedData[k++] = (double) stack.getCurrentShape().getStackRow() / ROWS;
            feedData[k++] = stack.getShapeRotation();
        } else {
            feedData[k++] = -1;
            feedData[k++] = -1;
            feedData[k++] = -1;
        }

        // Következő elem
        feedData[k++] = nextShape.getQid();

        // Lyukak száma
        feedData[k++] = stack.getNumberofHoles() / ROWS;

        // Majdnem teli sorok
        feedData[k++] = stack.getNearlyFullRows();

        // Blokkolt sorok
        feedData[k++] = stack.getBlockedRows();

        // Magasság különbségek
        feedData[k++] = stack.getAvgColumnHeights();

        // Körbevett lukak
        feedData[k++] = stack.calculateHolesSurroundings();

        // Maximum magasság
        feedData[k++] = stack.calculateMaxHeight();

        // Egyenetlenség
        feedData[k++] = stack.calculateBumpiness();

        // Játék állapot
        feedData[k++] = stack.getGameScore();
        feedData[k++] = stack.getGameLevel();
        feedData[k++] = stack.getAllFullRows();
        feedData[k++] = stack.getDroppedElements();

        // Terület tömörsége
        feedData[k++] = stack.calculateAverageDensity();

        InputNormalizer normalizer = new InputNormalizer(FEED_DATA_SIZE);
        double[] normalizedData = normalizer.normalizeAutomatically (feedData);
        for (int i = 0; i < normalizedData.length; i++) {
            if (Double.isNaN(normalizedData[i])) {
                System.out.println("NaN detektálva normalizálás után! Index: " + k);
            }
        }
        return normalizedData;
    }

    private double calculateReward() {
        double reward = 4;
        double fullRows = stack.getAllFullRows();
        double nearlyFullRows = stack.getNearlyFullRows();
        double droppedElements = stack.getDroppedElements();
        double numberofHoles = stack.getNumberofHoles();
        double surroundingHoles = stack.getSurroundingHoles();
        double maxHeight = stack.getMaxHeight();
        double avgColumnHeights = stack.getAvgColumnHeights();
        double blockedRows = stack.getBlockedRows();
        double bumpiness = stack.getBumpiness();
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        double avgDensity = stack.calculateAverageDensity();

        // Jutalom
        reward += Math.log(elapsedTime + 1) * 0.1;
        reward += (fullRows - lastFullRows) * 50;
        reward += (nearlyFullRows - lastNearlyFullRows) * 3;
        reward += (droppedElements - lastDroppedElements) * 3;
        reward += (avgDensity - lastAvgDensity) * 10;
        // Büntetés
        reward -= (numberofHoles - lastNumberofHoles);
        reward -= (surroundingHoles - lastSurroundingHoles);
        reward -= (maxHeight - lastMaxHeight) * 0.3;
        reward -= (avgColumnHeights - lastAvgColumnHeights) * 0.5;
        reward -= (blockedRows - lastBlockedRows) * 0.5;
        reward -= (bumpiness - lastBumpiness);
        if (stack.getNumberofHoles() == 0) {
            reward += 10;
        }
        if (stack.getMaxHeight() <= 6) {
            reward += 10;
        }
        lastFullRows = fullRows;
        lastNearlyFullRows = nearlyFullRows;
        lastDroppedElements = droppedElements;
        lastNumberofHoles = numberofHoles;
        lastSurroundingHoles = surroundingHoles;
        lastMaxHeight = maxHeight;
        lastAvgColumnHeights = avgColumnHeights;
        lastBlockedRows = blockedRows;
        lastBumpiness = bumpiness;
        lastAvgDensity = avgDensity;
        return reward * 8;
    }


    private double calculateFinishReward() {
        double reward = 0;
        double fullRows = stack.getAllFullRows();
        double nearlyFullRows = stack.getNearlyFullRows();
        double droppedElements = stack.getDroppedElements();
        double numberofHoles = stack.getNumberofHoles();
        double surroundingHoles = stack.getSurroundingHoles();
        double avgColumnHeights = stack.getAvgColumnHeights();
        double blockedRows = stack.getBlockedRows();
        double bumpiness = stack.getBumpiness();
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        double avgDensity = stack.calculateAverageDensity();
        // Jutalom
        reward += (avgDensity - lastAvgDensity) * 10;
        reward += Math.log(elapsedTime + 1) * 0.1;
        reward += (fullRows - lastFullRows) * 100;
        reward += (fullRows) * 50;
        reward += (nearlyFullRows - lastNearlyFullRows) * 5;
        reward += (droppedElements - lastDroppedElements) * 5;
        // Büntetés
        reward -= (numberofHoles - lastNumberofHoles);
        reward -= (surroundingHoles - lastSurroundingHoles);
        reward -= (avgColumnHeights - lastAvgColumnHeights) * 0.5;
        reward -= (blockedRows - lastBlockedRows) * 0.5;
        reward -= (bumpiness - lastBumpiness);
        lastFullRows = 0;
        lastNearlyFullRows = 0;
        lastDroppedElements = 0;
        lastNumberofHoles = 0;
        lastSurroundingHoles = 0;
        lastMaxHeight = 0;
        lastAvgColumnHeights = 0;
        lastBlockedRows = 0;
        lastBumpiness = 0;
        lastAvgDensity = 0;
        return reward * 400;
    }

    public NeuralNetworkQLearning getBrain() {
        if (!learning) {
            throw new IllegalStateException("Neural network is not available when learning is disabled.");
        }
        return brain;
    }


}
