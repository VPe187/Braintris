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

    private static final String[] LAYER_NAMES = GlobalConfig.getInstance().getLayerNames();
    private static final int[] LAYER_SIZES = GlobalConfig.getInstance().getLayerSizes();
    private static final Activation[] LAYER_ACTIVATIONS = GlobalConfig.getInstance().getLayerActivations();
    private static final WeightInitStrategy[] WEIGHT_INIT_STRATEGIES = GlobalConfig.getInstance().getWeightInitStrategies();
    private static final BatchNormParameters[] BATCH_NORMS = GlobalConfig.getInstance().getBatchNorms();
    private static final double[] L2_REGULARIZATION = GlobalConfig.getInstance().getL2Regularization();
    private static final int ROTATION_OUTPUTS = 3;
    private static final long DROP_SPEED = 1L;

    private NeuralNetwork brain;
    private static final long speed = 1000L;
    private final long learningSpeed = 1L;
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
    private double previousFullRows = 0;

    public Tetris(int width, int height, GameInput gameInput, boolean learning) {
        tickBackground = new GameTimeTicker(80);
        tickControl = new GameTimeTicker((learning) ? 1 : 20);
        tickAnim = new GameTimeTicker((learning) ? 1 : 20);
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
                    if (learning) {

                        // 1. előző jutalom kiszámítása
                        double reward = 0;
                        if (lastState != null && lastAction != null) {
                            stackMetrics.calculateGameMetrics(stackManager.getStackArea());
                            reward = calculateReward(false);
                        }

                        // 2. Új elem generálása
                        nextTetromino();

                        // 3. Új állapotok kiszámítása
                        StackMetrics metrics = new StackMetrics();
                        double[][] possibleStates = stackManager.simulateAllPossibleActions(
                                stackManager.getStackArea(),
                                stackManager.getCurrentTetromino(),
                                metrics
                        );

                        // 4. Tanuljunk az előző akcióból (ha volt)
                        if (lastState != null && lastAction != null) {
                            brain.learn(
                                    lastState,
                                    lastAction,
                                    reward,
                                    possibleStates.length > 0 ? possibleStates[0] : null,
                                    false,
                                    possibleStates
                            );
                        }

                        // 5. Válasszuk ki az új akciót
                        if (possibleStates.length > 0) {
                            int[] action = brain.selectAction(possibleStates);
                            int targetX = action[0];
                            int rotationCount = action[1];
                            int newStateIndex = targetX * ROTATION_OUTPUTS + rotationCount;
                            if (newStateIndex < possibleStates.length) {
                                // Az új állapot és akció mentése
                                lastState = possibleStates[newStateIndex];
                                lastAction = action;

                                // 6. Hajtsuk végre az akciót
                                stackManager.moveAndRotateTetrominoTo(
                                        stackManager.getStackArea(),
                                        stackManager.getCurrentTetromino(),
                                        targetX,
                                        rotationCount
                                );
                                stackMetrics.calculateGameMetrics(stackManager.getStackArea());
                            }
                        }
                    }
                } else {
                    stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino(), false);
                }
            }
        }
        if (stackManager.getGameState() == GameState.GAMEOVER) {
            if (learning) {
                brain.learn(
                        lastState,
                        lastAction,
                        calculateReward(true),
                        null,
                        true,
                        null
                );
                try {
                    brain.saveToFile();
                } catch (Exception e) {
                    System.out.println("Error saving Q-Learning Neural Network: " + e.getMessage());
                }
                lastState = null;
                lastAction = null;
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

            if (gameInput.letterM()) {
                if (musicOn) {
                    gameAudio.musicBackgroundStop();
                    musicOn = false;
                } else {
                    gameAudio.musicBackgroundPlay();
                    musicOn = true;
                }
            }
            if (gameInput.downRepeat()) {
                tickDown.setPeriodMilliSecond(DROP_SPEED);
            } else {
                tickDown.setPeriodMilliSecond(stackManager.getCurrentSpeed());
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

    private double calculateReward(Boolean gameOver) {
        double reward;
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());
        if (!gameOver) {
            int rows = stackManager.getAllFullRows();
            reward = 1.0 + (rows * rows) * COLS;
            reward -= stackMetrics.getMetricNumberOfHoles();
            reward -= stackMetrics.getMetricNumberOfHoles() / (ROWS * COLS);
            reward -= stackMetrics.getMetricBumpiness() / ROWS;
            reward -= stackMetrics.getMetricMaxHeight() / ROWS;
            return reward;
        } else {
            return -1;
        }
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
