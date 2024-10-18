package hu.nye.vpe.tetris;

import java.awt.Graphics2D;

import hu.nye.vpe.GlobalConfig;
import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameState;
import hu.nye.vpe.gaming.GameTimeTicker;
import hu.nye.vpe.nn.Activation;
import hu.nye.vpe.nn.BatchNormParameters;
import hu.nye.vpe.nn.InputNormalizerMinmax;
import hu.nye.vpe.nn.NeuralNetwork;
import hu.nye.vpe.nn.WeightInitStrategy;

/**
 * Tetris class.
 */
public class Tetris {
    private static final int ROWS = 24;
    private static final int COLS = 12;

    private static final double REWARD_FULLROW = GlobalConfig.getInstance().getRewardFullRow();
    private static final double REWARD_NEARLY_FULLROW = GlobalConfig.getInstance().getRewardNearlyFullRow();
    private static final double REWARD_PLACE_WITHOUT_HOLE = GlobalConfig.getInstance().getRewardPlaceWithoutHole();
    private static final double REWARD_DROP_LOWER = GlobalConfig.getInstance().getRewardDropLower();
    private static final double REWARD_DROPPED_ELEMENTS = GlobalConfig.getInstance().getRewardDropedElements();
    private static final double REWARD_AVG_DENSITY = GlobalConfig.getInstance().getRewardAvgDensity();
    private static final double REWARD_NUMBER_OF_HOLES = GlobalConfig.getInstance().getRewardNumberOfHoles();
    private static final double REWARD_SURROUNDED_HOLES = GlobalConfig.getInstance().getRewardSurroundedHoles();
    private static final double REWARD_DROP_HIGHER = GlobalConfig.getInstance().getRewardDropHigher();
    private static final double REWARD_BLOCKED_ROW = GlobalConfig.getInstance().getRewardBlockedRow();
    private static final double REWARD_BUMPINESS = GlobalConfig.getInstance().getRewardBumpiness();
    private static final double REWARD_AVG_COLUMN_HEIGHT = GlobalConfig.getInstance().getRewardAvgColumnHeight();
    private static final double REWARD_MAXIMUM_HEIGHT = GlobalConfig.getInstance().getRewardMaximumHeight();

    private static final int FEED_DATA_SIZE = GlobalConfig.getInstance().getFeedDataSize();
    private static final boolean NORMALIZE_FEED_DATA = GlobalConfig.getInstance().getNormalizeFeedData();

    private static final String[] LAYER_NAMES = GlobalConfig.getInstance().getLayerNames();
    private static final int[] LAYER_SIZES = GlobalConfig.getInstance().getLayerSizes();
    private static final Activation[] LAYER_ACTIVATIONS = GlobalConfig.getInstance().getLayerActivations();
    private static final WeightInitStrategy[] WEIGHT_INIT_STRATEGIES = GlobalConfig.getInstance().getWeightInitStrategies();
    private static final BatchNormParameters[] BATCH_NORMS = GlobalConfig.getInstance().getBatchNorms();
    private static final double[] L2_REGULARIZATION = GlobalConfig.getInstance().getL2Regularization();

    private static final long DROP_SPEED = 10L;
    private static int moveCount = 0;
    private static int episodeMoveCount = 0;
    private NeuralNetwork brain;
    private static final long speed = 1000L;
    private final long learningSpeed = 10L;
    private Tetromino nextTetromino = null;
    private static final TetrominoFactory sf = TetrominoFactory.getInstance();
    private StackUI stackUI;
    private StackManager stackManager;
    private StackMetrics stackMetrics;
    private StackMetrics simStackMetrics;
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
        tickBackground = new GameTimeTicker(80);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        initializeComponents(learning);
        this.gameInput = gameInput;
        this.learning = learning;
        if (this.learning) {
            try {
                brain = NeuralNetwork.loadFromFile();
            } catch (Exception e) {
                System.out.println("Creating new Neural Network");
                brain = new NeuralNetwork(
                        LAYER_NAMES,
                        LAYER_SIZES,
                        LAYER_ACTIVATIONS,
                        WEIGHT_INIT_STRATEGIES,
                        BATCH_NORMS,
                        L2_REGULARIZATION
                );
            }
        }
    }

    private void initializeComponents(boolean learning) {
        stackManager = new StackManager(learning);
        stackMetrics = new StackMetrics();
        stackUI = new StackUI(learning);
        stackManager.initializeStackComponents(stackUI, stackManager, stackMetrics);
        stackUI.initializeStackComponents(stackUI, stackManager, stackMetrics);
        stackMetrics.initializeStackComponents(stackUI, stackManager, stackMetrics);
        stackManager.start();
    }

    /**
     * Tetris start.
     */
    public void start() {
        stackManager.nextIteration();
        if (learning) {
            stackManager.setCurrentSpeed(learningSpeed);
        } else {
            stackManager.setCurrentSpeed(speed);
        }
        GameColorPalette.getInstance().setRandomPalette();
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        tickDown = new GameTimeTicker(stackManager.getCurrentSpeed());
        nextTetromino = createNextTetromino();
        if (!learning) {
            gameAudio.musicBackgroundPlay();
        }
        musicOn = true;
        nextTetromino();
        lastAction = 0;
        lastState = getFeedData();
        moveCount = 0;
        episodeMoveCount = 0;
        stackManager.start();
    }

    private void nextTetromino() {
        Tetromino currentTetromino = nextTetromino;
        nextTetromino = createNextTetromino();
        stackManager.setTetrominos(currentTetromino, nextTetromino);
    }

    private Tetromino createNextTetromino() {
        Tetromino newNextTetromino = sf.getRandomTetromino(-1);
        if (newNextTetromino != null) {
            newNextTetromino.setColPosition((COLS / 2) - 2);
            newNextTetromino.setRowPosition(0);
            newNextTetromino.rotateRight();
        }
        return newNextTetromino;
    }

    /**
     * Update.
     */
    public void update() {
        if (tickBackground.tick() && !learning) {
            starField.update();
        }
        if (tickControl.tick()) {
            handleInput();
        }
        if (tickDown.tick()) {
            if (stackManager.getGameState() != GameState.DELETINGROWS) {
                if (stackManager.getGameLevel() == 0) {
                    stackManager.nextLevel();
                }
                if (stackManager.getGameState() != GameState.GAMEOVER && stackManager.getGameState() !=
                        GameState.PAUSED && stackManager.getGameState() != GameState.CHANGINGLEVEL) {
                    if (stackManager.getCurrentTetromino() == null) {
                        nextTetromino();
                    } else {
                        if (stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino())) {
                            if (learning) {
                                learning(true, false);
                                moveCount = 0;
                            }
                        }
                    }
                }
            }
        }
        if (stackManager.getGameState() == GameState.GAMEOVER) {
            if (learning) {
                learning(true, true);
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
            if (stackManager.getGameState() == GameState.RUNNING) {
                stackManager.setState(GameState.PAUSED);
            } else if (stackManager.getGameState() == GameState.PAUSED) {
                stackManager.setState(GameState.RUNNING);
            }
            return;
        }
        if (gameInput.letterR()) {
            start();
            return;
        }
        if (stackManager.getGameState() == GameState.RUNNING && stackManager.getCurrentTetromino() != null) {
            if (learning) {
                double[] currentState = getFeedData();
                int action = brain.selectAction(currentState);
                lastAction = action;
                lastState = currentState;
                switch (action) {
                    case 0:
                        stackManager.rotateTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                        learning(false, false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 1:
                        stackManager.rotateTetrominoLeft(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                        learning(false, false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 2:
                        stackManager.moveTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                        learning(false, false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 3:
                        stackManager.moveTetrominoLeft(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                        learning(false, false);
                        moveCount++;
                        episodeMoveCount++;
                        break;
                    case 4:
                        tickDown.setPeriodMilliSecond(DROP_SPEED);
                        stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                        learning(false, false);
                        break;
                    default:
                        tickDown.setPeriodMilliSecond(stackManager.getCurrentSpeed());
                        break;
                }
            } else {
                if (gameInput.left()) {
                    stackManager.moveTetrominoLeft(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }

                if (gameInput.right()) {
                    stackManager.moveTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }

                if (gameInput.space()) {
                    stackManager.rotateTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }

                if (gameInput.downRepeat()) {
                    tickDown.setPeriodMilliSecond(DROP_SPEED);
                } else {
                    tickDown.setPeriodMilliSecond(stackManager.getCurrentSpeed());
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
        if (!learning) {
            starField.render(g2d);
        }
        stackUI.setTickAnim(tickAnim.tick());
        stackUI.render(g2d);
    }

    private double[] getFeedData() {
        double[] feedData = new double[FEED_DATA_SIZE];
        int k = 0;

        feedData[k++] = (stackManager.getCurrentTetromino() != null) ? stackManager.getCurrentTetromino().getId() : -1;
        feedData[k++] = (nextTetromino != null) ? nextTetromino.getId() : -1;

        // Oszlopok
        double[] columns = stackMetrics.getMetricColumnHeights();
        for (double column : columns) {
            feedData[k++] = column;
        }

        // Aktuális elem x és y poziciója
        if (stackManager.getCurrentTetromino() != null) {
            feedData[k++] = stackManager.getCurrentTetromino().getStackCol();
            feedData[k++] = stackManager.getCurrentTetromino().getStackRow();
            feedData[k++] = stackManager.getTetrominoRotation();
        } else {
            feedData[k++] = 0;
            feedData[k++] = 0;
            feedData[k++] = 0;
        }

        // Összes sor
        feedData[k++] = stackManager.getAllFullRows();

        // Majdnem teli sorok
        feedData[k++] = stackMetrics.getMetricNearlyFullRows();

        // Lyukak száma
        feedData[k++] = stackMetrics.getMetricNumberOfHoles();

        // Ledobott elemek
        feedData[k++] = stackMetrics.getMetricDroppedElements();

        // Átlagsűrűség
        feedData[k++] = stackMetrics.getMetricAvgDensity();

        // Lyukak száma
        feedData[k++] = stackMetrics.getMetricNumberOfHoles();

        // Körbevett lukak
        feedData[k++] = stackMetrics.getMetricSurroundingHoles();

        // Blokkolt sorok
        feedData[k++] = stackMetrics.getMetricBlockedRows();

        // Egyenetlenség
        feedData[k++] = stackMetrics.getMetricBumpiness();

        // Maximum magasság
        feedData[k++] = stackMetrics.getMetricMaxHeight();

        // Magasság különbségek
        feedData[k++] = stackMetrics.getMetricAvgColumnHeight();

        // Terület tömörsége
        feedData[k++] = stackMetrics.getMetricAvgDensity();

        // Mozgatások száma
        feedData[k] = moveCount;

        if (NORMALIZE_FEED_DATA) {
            InputNormalizerMinmax normalizer = new InputNormalizerMinmax(FEED_DATA_SIZE);
            return normalizer.normalizeAutomatically(feedData);
        } else {
            return feedData;
        }
    }

    private double calculateReward() {
        double reward = 0;
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());

        // Jutalom teljes kirakott sorokra
        double fullRows = stackManager.getAllFullRows();
        reward += (fullRows - lastFullRows) * REWARD_FULLROW;

        // Jutalom a közel teli sorokra
        double nearlyFullRows = stackMetrics.getMetricNearlyFullRows();
        reward += (nearlyFullRows - lastNearlyFullRows) * REWARD_NEARLY_FULLROW;

        // Jutalom a ledobott elemekért
        double droppedElements = stackMetrics.getMetricDroppedElements();
        reward += (droppedElements - lastDroppedElements) * REWARD_DROPPED_ELEMENTS;

        // Jutalom az átlagsűrűségért
        double avgDensity = stackMetrics.getMetricAvgDensity();
        reward += (avgDensity - lastAvgDensity) * REWARD_AVG_DENSITY;

        // Jutalom a lyuk nélküli elhelyezésért
        if (stackMetrics.getMetricNumberOfHoles() == 0) {
            reward += REWARD_PLACE_WITHOUT_HOLE;
        }

        if (stackManager.getCurrentTetromino() != null) {
            reward += stackManager.getCurrentTetromino().getStackRow() * 1;
        }

        // Jutalom az alacsonyra lerakásért
        /*
        if (stack.getMetricMaxHeight() <= 6) {
            reward += REWARD_DROP_LOWER;
        } else {
            reward -= REWARD_DROP_HIGHER;
        }

         */

        // Büntetés a lyukakért
        double numberofHoles = stackMetrics.getMetricNumberOfHoles();
        reward -= (numberofHoles - lastNumberofHoles) * REWARD_NUMBER_OF_HOLES;

        // Büntetés a végleges lyukakért
        double surroundingHoles = stackMetrics.getMetricSurroundingHoles();
        reward -= (surroundingHoles - lastSurroundingHoles) * REWARD_SURROUNDED_HOLES;

        // Büntetés a blokkolt sorokért
        double blockedRows = stackMetrics.getMetricBlockedRows();
        reward -= (blockedRows - lastBlockedRows) * REWARD_BLOCKED_ROW;

        // Büntetés az egyenetlenségért
        double bumpiness = stackMetrics.getMetricBumpiness();
        reward -= (bumpiness - lastBumpiness) * REWARD_BUMPINESS;

        // Büntetés a magasra helyezésért
        double maxHeight = stackMetrics.getMetricMaxHeight();
        //reward -= (maxHeight - lastMaxHeight) * REWARD_MAXIMUM_HEIGHT;
        // Büntetés az átlagos magasságért
        double avgColumnHeights = stackMetrics.getMetricAvgColumnHeight();
        reward -= (avgColumnHeights - lastAvgColumnHeights) * REWARD_AVG_COLUMN_HEIGHT;

        lastFullRows = fullRows;
        lastMaxHeight = maxHeight;
        lastNearlyFullRows = nearlyFullRows;
        lastDroppedElements = droppedElements;
        lastNumberofHoles = numberofHoles;
        lastSurroundingHoles = surroundingHoles;
        lastAvgColumnHeights = avgColumnHeights;
        lastBlockedRows = blockedRows;
        lastBumpiness = bumpiness;
        lastAvgDensity = avgDensity;
        return reward;
    }

    private double calculateFinishReward() {
        double reward = 0;
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());

        // Jutalom a teljes kirakott sorokra
        double fullRows = stackManager.getAllFullRows();
        reward += fullRows * REWARD_FULLROW;

        // Jutalom a közel teli sorokra
        double nearlyFullRows = stackMetrics.getMetricNearlyFullRows();
        reward += nearlyFullRows * REWARD_NEARLY_FULLROW;

        // Jutalom a ledobott elemekért
        double droppedElements = stackMetrics.getMetricDroppedElements();
        reward += droppedElements * REWARD_DROPPED_ELEMENTS;

        // Jutalom az átlagsűrűségért
        double avgDensity = stackMetrics.getMetricAvgDensity();
        reward += avgDensity * REWARD_AVG_DENSITY;

        // Jutalom a játékidőért
        double elapsedTime = stackManager.getElapsedTimeLong() / 1000.0;
        reward += Math.log(elapsedTime + 1) * 0.1;

        // Büntetés a lyukakért
        double numberofHoles = stackMetrics.getMetricNumberOfHoles();
        reward -= numberofHoles * REWARD_NUMBER_OF_HOLES;

        // Büntetés a végleges lyukakért
        double surroundingHoles = stackMetrics.getMetricSurroundingHoles();
        reward -= surroundingHoles * REWARD_SURROUNDED_HOLES;

        // Büntetés a blokkolt sorokért
        double blockedRows = stackMetrics.getMetricBlockedRows();
        reward -= blockedRows * REWARD_BLOCKED_ROW;

        // Büntetés az egyenetlenségért
        double bumpiness = stackMetrics.getMetricBumpiness() * REWARD_BUMPINESS;
        reward -= bumpiness;

        // Büntetés az átlagos magasságért
        double avgColumnHeights = stackMetrics.getMetricAvgColumnHeight();
        reward -= avgColumnHeights * REWARD_AVG_COLUMN_HEIGHT;

        if (droppedElements > 0) {
            reward += droppedElements / episodeMoveCount;
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
        return reward;
    }

    private void learning(boolean dropped, boolean gameOver) {
        double[] currentState = getFeedData();
        double reward = 0;
        if (gameOver) {
            reward = calculateFinishReward();
        } else {
            if (dropped) {
                reward = calculateReward();
            } else {
                reward += moveCount * 0.01 * -1;
                stackMetrics.calculateGameMetrics(stackManager.getSimStackArea());

                if (stackMetrics.getMetricMaxHeight() <= 6) {
                    reward += REWARD_DROP_LOWER;
                } else {
                    reward -= REWARD_DROP_HIGHER;
                }

                if (stackMetrics.getMetricNumberOfHoles() == 0) {
                    reward += REWARD_PLACE_WITHOUT_HOLE;
                }

                double fullRows = stackManager.getAllFullRows();
                reward += fullRows * REWARD_FULLROW;

                double nearlyFullRows = stackMetrics.getMetricNearlyFullRows();
                reward += nearlyFullRows * REWARD_NEARLY_FULLROW;

                double bumpiness = stackMetrics.getMetricBumpiness();
                reward -= bumpiness * REWARD_BUMPINESS;

                double maxHeight = stackMetrics.getMetricMaxHeight();
                reward -= maxHeight * REWARD_MAXIMUM_HEIGHT;

                double blockedRows = stackMetrics.getMetricBlockedRows();
                reward -= blockedRows * REWARD_BLOCKED_ROW;

                double avgDensity = stackMetrics.getMetricAvgDensity();
                reward -= avgDensity * REWARD_AVG_DENSITY;

                double numberOfHoles = stackMetrics.getMetricNumberOfHoles();
                reward -= numberOfHoles * REWARD_NUMBER_OF_HOLES;

                double surroundedHoles = stackMetrics.getMetricSurroundingHoles();
                reward -= surroundedHoles * REWARD_SURROUNDED_HOLES;

                double avgColumnHeights = stackMetrics.getMetricAvgColumnHeight();
                reward -= avgColumnHeights * REWARD_AVG_COLUMN_HEIGHT;

                reward = 60 + reward;
            }
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
