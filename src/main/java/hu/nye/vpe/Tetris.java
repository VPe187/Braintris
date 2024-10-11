package hu.nye.vpe;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;
import hu.nye.vpe.nn.Activation;
import hu.nye.vpe.nn.InputNormalizerZScore;
import hu.nye.vpe.nn.NeuralNetwork;
import hu.nye.vpe.nn.WeightInitStrategy;

/**
 * Tetris class.
 */
public class Tetris {
    private static final int ROWS = 24;
    private static final int COLS = 12;
    private static final int FEED_DATA_SIZE = 16;
    private static final int OUTPUT_NODES = 4;
    private static final long DROP_SPEED = 10L;
    private static int moveCount = 0;
    private static int episodeMoveCount = 0;

    private NeuralNetwork brain;

    private static final String[] NAMES = {"INP", "H1", "H2", "H3", "OUT"};
    private static final int[] LAYER_SIZES = {FEED_DATA_SIZE, 32, 16, 8, OUTPUT_NODES};
    private static final Activation[] ACTIVATIONS = {
            Activation.LEAKY_RELU,
            Activation.LEAKY_RELU,
            Activation.ELU,
            Activation.LINEAR
    };
    private static final WeightInitStrategy[] INIT_STRATEGIES = {
            WeightInitStrategy.HE,
            WeightInitStrategy.HE,
            WeightInitStrategy.HE,
            WeightInitStrategy.HE
    };
    private static final boolean[] USE_BATCH_NORM = {false, true, true, false};
    private static final double[] L2 = {0.0, 0.0001, 0.0001, 0.0};

    private final long speed = 1000L;
    private final long learningSpeed = 5L;
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
                brain = NeuralNetwork.loadFromFile();
            } catch (Exception e) {
                System.out.println("Creating new Neural Network");
                brain = new NeuralNetwork(
                        NAMES,
                        LAYER_SIZES,
                        ACTIVATIONS,
                        INIT_STRATEGIES,
                        USE_BATCH_NORM,
                        L2
                );
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
        moveCount = 0;
        episodeMoveCount = 0;
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
                                //System.out.println("Down: " + moveCount);
                                learning(false);
                                moveCount = 0;
                            }
                        }
                    }
                }
            }
        }
        if (stack.getState() == State.GAMEOVER) {
            if (learning) {
                learning(true);
                //System.out.println("All movements: " + episodeMoveCount);
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
                lastAction = action;
                lastState = currentState;
                switch (action) {
                    case 0:
                        stack.rotateShapeRight();
                        learning(false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 1:
                        stack.rotateShapeLeft();
                        learning(false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 2:
                        stack.moveShapeRight();
                        learning(false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 3:
                        stack.moveShapeLeft();
                        learning(false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 4:
                        tickDown.setPeriodMilliSecond(DROP_SPEED);
                        stack.moveShapeDown();
                        learning(false);
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
                    /*
                    if (stack.getCurrentShape() != null) {
                        stack.moveAndRotateShapeTo(0, 24, 2);
                    }
                     */
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

        /*
        // Oszlopmagasságok
        double[] columnHeights = stack.getMetricColumnHeights();
        for (double columnHeight : columnHeights) {
            feedData[k++] = columnHeight;
        }
         */

        //Aktuális elem
        /*
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
         */

        // Összes sor
        feedData[k++] = stack.getAllFullRows();

        // Majdnem teli sorok
        feedData[k++] = stack.getMetricNearlyFullRows();

        // Lyukak száma
        feedData[k++] = stack.getMetricNumberOfHoles();

        // Ledobott elemek
        feedData[k++] = stack.getMetricDroppedElements();

        // Átlagsűrűség
        feedData[k++] = stack.getMetricAvgDensity();

        // Lyukak száma
        feedData[k++] = stack.getMetricNumberOfHoles();

        // Körbevett lukak
        feedData[k++] = stack.getMetricSurroundingHoles();

        // Blokkolt sorok
        feedData[k++] = stack.getMetricBlockedRows();

        // Egyenetlenség
        feedData[k++] = stack.getMetricBumpiness();

        // Maximum magasság
        feedData[k++] = stack.getMetricMaxHeight();

        // Magasság különbségek
        feedData[k++] = stack.getMetricAvgColumnHeight();

        // Játék állapot
        /*
        feedData[k++] = stack.getGameScore();
        feedData[k++] = stack.getGameLevel();
         */

        // Terület tömörsége
        feedData[k++] = stack.getMetricAvgDensity();

        // Mozgatások száma
        feedData[k] = moveCount;

        // Normalizálás
        //InputNormalizerMinmax normalizer = new InputNormalizerMinmax(FEED_DATA_SIZE);
        InputNormalizerZScore normalizer = new InputNormalizerZScore(FEED_DATA_SIZE);
        double[] normalizedData = normalizer.normalizeAutomatically(feedData);
        return normalizedData;
    }

    private double calculateReward() {
        double reward = 0;

        // Jutalom teljes kirakott sorokra
        double fullRows = stack.getAllFullRows();
        reward += (fullRows - lastFullRows) * 100;

        // Jutalom a közel teli sorokra
        double nearlyFullRows = stack.getMetricNearlyFullRows();
        reward += (nearlyFullRows - lastNearlyFullRows) * 5;

        // Jutalom a ledobott elemekért
        double droppedElements = stack.getMetricDroppedElements();
        reward += (droppedElements - lastDroppedElements) * 2;

        // Jutalom az átlagsűrűségért
        double avgDensity = stack.getMetricAvgDensity();
        reward += (avgDensity - lastAvgDensity) * 10;

        // Jutalom a lyuk nélküli elhelyezésért
        if (stack.getMetricNumberOfHoles() == 0) {
            reward += 20;
        }

        // Jutalom az alacsonyra lerakásért
        if (stack.getMetricMaxHeight() <= 8) {
            reward += 1;
        } else {
            reward -= 2;
        }

        // Büntetés a lyukakért
        double numberofHoles = stack.getMetricNumberOfHoles();
        reward -= (numberofHoles - lastNumberofHoles) * 0.3;

        // Büntetés a végleges lyukakért
        double surroundingHoles = stack.getMetricSurroundingHoles();
        reward -= (surroundingHoles - lastSurroundingHoles) * 0.8;

        // Büntetés a blokkolt sorokért
        double blockedRows = stack.getMetricBlockedRows();
        reward -= (blockedRows - lastBlockedRows) * 0.2;

        // Büntetés az egyenetlenségért
        double bumpiness = stack.getMetricBumpiness();
        reward -= (bumpiness - lastBumpiness) * 0.1;

        // Büntetés a magasra helyezésért
        double maxHeight = stack.getMetricMaxHeight();
        reward -= (maxHeight - lastMaxHeight) * 0.1;

        // Büntetés az átlagos magasságért
        double avgColumnHeights = stack.getMetricAvgColumnHeight();
        reward -= (avgColumnHeights - lastAvgColumnHeights) * 0.1;

        // Összes lerakott elemre jutó mozgatás
        //reward += (5 - (episodeCount / droppedElements));

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
        return reward;
    }

    private double calculateFinishReward() {
        double reward = -10;

        // Jutalom a teljes kirakott sorokra
        double fullRows = stack.getAllFullRows();
        reward += fullRows * 100;

        // Jutalom a közel teli sorokra
        double nearlyFullRows = stack.getMetricNearlyFullRows();
        reward += nearlyFullRows * 5;

        // Jutalom a ledobott elemekért
        double droppedElements = stack.getMetricDroppedElements();
        reward += droppedElements * 2;

        // Jutalom az átlagsűrűségért
        double avgDensity = stack.getMetricAvgDensity();
        reward += avgDensity * 10;

        // Jutalom a játékidőért
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        reward += Math.log(elapsedTime + 1) * 0.1;

        // Büntetés a lyukakért
        double numberofHoles = stack.getMetricNumberOfHoles();
        reward -= numberofHoles * 0.3;

        // Büntetés a végleges lyukakért
        double surroundingHoles = stack.getMetricSurroundingHoles();
        reward -= surroundingHoles * 0.8;

        // Büntetés a blokkolt sorokért
        double blockedRows = stack.getMetricBlockedRows();
        reward -= blockedRows * 0.2;

        // Büntetés az egyenetlenségért
        double bumpiness = stack.getMetricBumpiness() * 0.1;
        reward -= bumpiness;

        // Büntetés az átlagos magasságért
        double avgColumnHeights = stack.getMetricAvgColumnHeight();
        reward -= avgColumnHeights;

        if (droppedElements > 0) {
            reward += 5 - episodeMoveCount / droppedElements;
        }

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
        return 50 + reward;
    }

    private void learning(boolean gameOver) {
        double[] currentState = getFeedData();
        double reward;
        if (gameOver) {
            reward = calculateFinishReward();
        } else {
            reward = calculateReward();
        }
        brain.learn(lastState, lastAction, reward, currentState, gameOver);
        lastState = currentState;
    }

    /**
     * Get neural network.
     *
     * @return neural network
     */
    public NeuralNetwork getBrain() {
        if (!learning) {
            throw new IllegalStateException("Neural network is not available when learning is disabled.");
        }
        return brain;
    }
}
