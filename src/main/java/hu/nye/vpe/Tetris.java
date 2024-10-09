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
    private static final int FEED_DATA_SIZE = 34;
    private static final int OUTPUT_NODES = 4;
    private static final long DROP_SPEED = 10L;
    private final long speed = 1000L;
    private final long learningSpeed = 30L;
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
    private double lastReward;

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
        lastAction = 0;
        lastState = getFeedData();
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
                                brain.learn(lastState, lastAction, reward, currentState, false);
                                lastState = currentState;
                                lastReward = reward;
                            }
                        }
                    }
                }
            }
        }
        if (stack.getState() == State.GAMEOVER) {
            if (learning) {
                double[] currentState = getFeedData();
                double reward = calculateReward();
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
                lastState = currentState;
                int action = brain.selectAction(currentState);
                lastAction = action;
                switch (action) {
                    case 0:
                        stack.rotateShapeRight();
                        break;
                    case 1:
                        stack.moveShapeRight();
                        break;
                    case 2:
                        stack.moveShapeLeft();
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
                if (gameInput.ctrlDown()) {
                    if (stack.getCurrentShape() != null) {
                        stack.moveAndRotateShapeTo(0, 24, 2);
                    }
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

        // Oszlopmagasságok
        double[] columnHeights = stack.getMetricColumnHeights();
        for (double columnHeight : columnHeights) {
            feedData[k++] = columnHeight;
        }

        //Aktuális elem
        if (stack.getCurrentShape() != null) {
            double[] currentShape = sf.shapeToArray(stack.getCurrentShape());
            for (int i = 0; i < currentShape.length; i++) {
                feedData[k++] = currentShape[i];
            }
        } else {
            for (int i = 0; i < 4; i++) {
                feedData[i] = -1;
                k++;
            }
        }

        // Aktuális elem x és y poziciója
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getStackCol();
            feedData[k++] = stack.getCurrentShape().getStackRow();
            feedData[k++] = stack.getShapeRotation();
        } else {
            feedData[k++] = 0;
            feedData[k++] = 0;
            feedData[k++] = 0;
        }

        // Következő elem
        feedData[k++] = nextShape.getId();

        // Lyukak száma
        feedData[k++] = stack.getMetricNumberOfHoles();

        // Majdnem teli sorok
        /*
        feedData[k++] = stack.getNearlyFullRows();
         */

        // Blokkolt sorok
        /*
        feedData[k++] = stack.getBlockedRows();
         */

        // Magasság különbségek
        /*
        feedData[k++] = stack.getAvgColumnHeights();
         */

        // Körbevett lukak
        /*
        feedData[k++] = stack.calculateHolesSurroundings();
         */

        // Maximum magasság
        //feedData[k++] = stack.calculateMaxHeight();

        // Egyenetlenség
        /*
        feedData[k++] = stack.calculateBumpiness();
         */

        // Játék állapot
        /*
        feedData[k++] = stack.getGameScore();
        feedData[k++] = stack.getGameLevel();
        feedData[k++] = stack.getAllFullRows();
        feedData[k++] = stack.getDroppedElements();
         */

        // Terület tömörsége
        feedData[k++] = stack.getMetricAvgDensity();

        InputNormalizer normalizer = new InputNormalizer(FEED_DATA_SIZE);
        double[] normalizedData = normalizer.normalizeAutomatically(feedData);
        for (int i = 0; i < normalizedData.length; i++) {
            if (Double.isNaN(normalizedData[i])) {
                System.out.println("NaN detektálva normalizálás után! Index: " + k);
            }
        }
        return normalizedData;
    }

    private double calculateReward() {
        double reward = 0;
        // Jutalom
        //double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        //reward += Math.log(elapsedTime + 1) * 0.1;
        double fullRows = stack.getAllFullRows();
        reward += (fullRows - lastFullRows) * 100;
        //reward += (nearlyFullRows - lastNearlyFullRows) * 3;
        double droppedElements = stack.getMetricDroppedElements();
        reward += (droppedElements - lastDroppedElements) * 0.1;
        // Büntetés
        double avgDensity = stack.getMetricAvgDensity();
        reward -= 50 - (avgDensity - lastAvgDensity) * 100;
        double numberofHoles = stack.getMetricNumberOfHoles();
        reward -= (numberofHoles - lastNumberofHoles);

        /*
        double surroundingHoles = stack.getSurroundingHoles();
        reward -= (surroundingHoles - lastSurroundingHoles);
        double maxHeight = stack.getMaxHeight();
        reward -= (maxHeight - lastMaxHeight) * 0.3;
        double avgColumnHeights = stack.getAvgColumnHeights();
        reward -= (avgColumnHeights - lastAvgColumnHeights) * 0.5;
        double blockedRows = stack.getBlockedRows();
        reward -= (blockedRows - lastBlockedRows) * 0.5;
        double bumpiness = stack.getBumpiness();
        reward -= (bumpiness - lastBumpiness);

        if (stack.getNumberofHoles() == 0) {
            reward += 10;
        }
        if (stack.getMaxHeight() <= 6) {
            reward += 10;
        }
        lastFullRows = fullRows;
        double nearlyFullRows = stack.getNearlyFullRows();
        lastNearlyFullRows = nearlyFullRows;
        lastDroppedElements = droppedElements;
        lastNumberofHoles = numberofHoles;
        lastSurroundingHoles = surroundingHoles;
        lastMaxHeight = maxHeight;
        lastAvgColumnHeights = avgColumnHeights;
        lastBlockedRows = blockedRows;
        lastBumpiness = bumpiness;
        lastAvgDensity = avgDensity;
         */
        return reward;
    }

    private double calculateFinishReward() {
        double reward = 0;
        // Jutalom
        double avgDensity = stack.getMetricAvgDensity();
        reward += avgDensity * 10;
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        reward += Math.log(elapsedTime + 1) * 0.2;
        double fullRows = stack.getAllFullRows();
        reward += fullRows * 100;
        double nearlyFullRows = stack.getMetricNearyFullRows();
        reward += nearlyFullRows * 5;
        double droppedElements = stack.getMetricDroppedElements();
        reward += droppedElements * 2;
        // Büntetés
        double numberofHoles = stack.getMetricNumberOfHoles();
        reward -= numberofHoles * 0.5;
        double surroundingHoles = stack.getMetricSurroundingHoles();
        reward -= surroundingHoles * 0.5;
        double avgColumnHeights = stack.getMetricAvgColumnHeight();
        reward -= avgColumnHeights;
        double blockedRows = stack.getMetricBlockedRows();
        reward -= blockedRows * 0.5;
        double bumpiness = stack.getMetricBumpiness();
        reward -= bumpiness;
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
        return reward;
    }

    /**
     * Get neural network.
     *
     * @return neural network
     */
    public NeuralNetworkQLearning getBrain() {
        if (!learning) {
            throw new IllegalStateException("Neural network is not available when learning is disabled.");
        }
        return brain;
    }
}
