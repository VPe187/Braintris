package hu.nye.vpe.tetris;

import java.awt.Graphics2D;
import java.util.Objects;

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
import hu.nye.vpe.nn.InputNormalizerZScore;
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
    private static final String FEED_DATA_NORMALIZER = GlobalConfig.getInstance().getFeedDataNormalizer();

    private static final String[] LAYER_NAMES = GlobalConfig.getInstance().getLayerNames();
    private static final int[] LAYER_SIZES = GlobalConfig.getInstance().getLayerSizes();
    private static final Activation[] LAYER_ACTIVATIONS = GlobalConfig.getInstance().getLayerActivations();
    private static final WeightInitStrategy[] WEIGHT_INIT_STRATEGIES = GlobalConfig.getInstance().getWeightInitStrategies();
    private static final BatchNormParameters[] BATCH_NORMS = GlobalConfig.getInstance().getBatchNorms();
    private static final double[] L2_REGULARIZATION = GlobalConfig.getInstance().getL2Regularization();

    private static final long DROP_SPEED = 10L;
    private NeuralNetwork brain;
    private static final long speed = 1000L;
    private final long learningSpeed = 10L;
    private Tetromino nextTetromino = null;
    private static final TetrominoFactory sf = TetrominoFactory.getInstance();
    private StackUI stackUI;
    private StackManager stackManager;
    private StackMetrics stackMetrics;
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
    private int[] lastAction;

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
        stackUI = new StackUI(learning);
        stackMetrics = new StackMetrics();
        stackManager.initializeStackComponents(stackUI, stackManager, stackMetrics);
        stackMetrics.initializeStackComponents(stackUI, stackManager, stackMetrics);
        stackUI.initializeStackComponents(stackUI, stackManager, stackMetrics);
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
        stackManager.start();
        if (stackManager.getGameLevel() == 0) {
            stackManager.nextLevel();
        }
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
            if (!learning) {
                handleInput();
            }
        }
        if (tickDown.tick()) {
            if (stackManager.getGameState() == GameState.RUNNING) {
                if (stackManager.getCurrentTetromino() == null) {
                    nextTetromino();
                    if (learning) {
                        if (stackManager.getCurrentTetromino() != null) {
                            double[] currentState = getFeedData();
                            int[] action = brain.selectAction(currentState);
                            int targetX = action[0];
                            int targetRotation = (action[1] * 90) % 360;
                            lastAction = action;
                            lastState = currentState;
                            stackManager.moveAndRotateTetrominoTo(stackManager.getStackArea(),
                                    stackManager.getCurrentTetromino(),
                                    targetX, targetRotation);
                            stackManager.setTetrominoRotation(targetRotation);
                            double reward = calculateReward(false);
                            double[] nextState = getFeedData();
                            brain.learn(lastState, lastAction, reward, nextState, stackManager.getGameState() == GameState.GAMEOVER);
                        }
                    }
                } else {
                    stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }
            }
        }
        if (stackManager.getGameState() == GameState.GAMEOVER) {
            if (learning) {
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
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());

        if (stackManager.getCurrentTetromino() != null) {
            feedData[k++] = Math.tanh(1 + stackManager.getCurrentTetromino().getId());
            feedData[k++] = Math.tanh(1 + stackManager.getNextTetromino().getId());
            feedData[k++] = stackManager.getTetrominoRotation() / 100;
        } else {
            feedData[k++] = -0.01;
            feedData[k++] = -0.01;
            feedData[k++] = -0.01;
        }

        double[] columns = stackMetrics.getMetricColumnHeights();
        for (double column : columns) {
            feedData[k++] = column / 10;
        }

        feedData[k++] = stackManager.getAllFullRows();
        feedData[k++] = stackMetrics.getMetricNearlyFullRows();
        feedData[k++] = stackMetrics.getMetricAvgDensity() * 4;
        feedData[k++] = stackMetrics.getMetricNumberOfHoles() / 20;
        feedData[k++] = stackMetrics.getMetricSurroundingHoles() / 20;
        feedData[k++] = stackMetrics.getMetricBlockedRows() / 20;
        feedData[k++] = stackMetrics.getMetricBumpiness() / 20;
        feedData[k++] = stackMetrics.getMetricMaxHeight() / 10;
        feedData[k] = stackMetrics.getMetricAvgColumnHeight() * 10;

        if (NORMALIZE_FEED_DATA) {
            if (Objects.equals(FEED_DATA_NORMALIZER, "MINMAX")) {
                InputNormalizerMinmax normalizer = new InputNormalizerMinmax(FEED_DATA_SIZE);
                return normalizer.normalizeAutomatically(feedData);
            } else if (Objects.equals(FEED_DATA_NORMALIZER, "ZSCORE")) {
                InputNormalizerZScore normalizer = new InputNormalizerZScore(FEED_DATA_SIZE);
                return normalizer.normalizeAutomatically(feedData);
            } else {
                throw new IllegalArgumentException("Unsupported normalization type: " + FEED_DATA_NORMALIZER);
            }
        } else {
            return feedData;
        }
    }

    private double calculateReward(Boolean gameOver) {
        double reward = 0;
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());

        // Jutalom teljes kirakott sorokra
        double fullRows = stackManager.getAllFullRows();
        reward += fullRows * REWARD_FULLROW;

        // Jutalom a közel teli sorokra
        double nearlyFullRows = stackMetrics.getMetricNearlyFullRows();
        reward += nearlyFullRows * REWARD_NEARLY_FULLROW;

        // Jutalom az átlagsűrűségért
        double avgDensity = stackMetrics.getMetricAvgDensity();
        reward += avgDensity * REWARD_AVG_DENSITY;

        // Jutalom a lyuk nélküli elhelyezésért
        reward += REWARD_PLACE_WITHOUT_HOLE;

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
        double bumpiness = stackMetrics.getMetricBumpiness();
        reward -= bumpiness * REWARD_BUMPINESS;

        // Büntetés a magasra helyezésért
        double maxHeight = stackMetrics.getMetricMaxHeight();
        reward -= maxHeight * REWARD_MAXIMUM_HEIGHT;

        // Büntetés az átlagos magasságért
        double avgColumnHeights = stackMetrics.getMetricAvgColumnHeight();
        reward -= avgColumnHeights * REWARD_AVG_COLUMN_HEIGHT;

        return reward;
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
