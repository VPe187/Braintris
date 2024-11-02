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
    private static final String[] LAYER_NAMES = GlobalConfig.getInstance().getLayerNames();
    private static final int[] LAYER_SIZES = GlobalConfig.getInstance().getLayerSizes();
    private static final Activation[] LAYER_ACTIVATIONS = GlobalConfig.getInstance().getLayerActivations();
    private static final WeightInitStrategy[] WEIGHT_INIT_STRATEGIES = GlobalConfig.getInstance().getWeightInitStrategies();
    private static final BatchNormParameters[] BATCH_NORMS = GlobalConfig.getInstance().getBatchNorms();
    private static final double[] L2_REGULARIZATION = GlobalConfig.getInstance().getL2Regularization();
    private static final int ROTATION_OUTPUTS = 3;
    private static final long DROP_SPEED = 1L;
    private static final Boolean TEST_ALGORITHM_ONLY = false;

    private RunMode runMode;
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
    private final GameTimeTicker tickPlay;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private static final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private boolean musicOn = true;
    private double[] lastState;
    private int[] lastAction;
    private double previousFullRows = 0;
    int[] action;

    public Tetris(int width, int height, GameInput gameInput, RunMode runMode) {
        this.runMode = runMode;
        tickBackground = new GameTimeTicker(80);
        tickControl = new GameTimeTicker((runMode == RunMode.TRAIN_AI) ? 1 : 20);
        tickAnim = new GameTimeTicker((runMode == RunMode.TRAIN_AI) ? 1 : 20);
        starField = new GameStarfield(width, height);
        tickPlay = new GameTimeTicker(speed / 10);
        initializeComponents();
        this.gameInput = gameInput;
        if (runMode == RunMode.TRAIN_AI) {
            try {
                brain = new NeuralNetwork(
                        LAYER_NAMES,
                        LAYER_SIZES,
                        LAYER_ACTIVATIONS,
                        WEIGHT_INIT_STRATEGIES,
                        BATCH_NORMS,
                        L2_REGULARIZATION
                );
                brain.loadNetworkStructure("brain_network.json");
                brain.loadTrainingState("brain_training.json");
                System.out.println("Neural Network loaded successfully");
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
        if (runMode == RunMode.PLAY_AI) {
            try {
                brain = new NeuralNetwork(
                        LAYER_NAMES,
                        LAYER_SIZES,
                        LAYER_ACTIVATIONS,
                        WEIGHT_INIT_STRATEGIES,
                        BATCH_NORMS,
                        L2_REGULARIZATION
                );
                brain.loadNetworkStructure("brain_network.json");
                System.out.println("Neural Network loaded successfully");
            } catch (Exception e) {
                System.out.println("Nem sikerült a hálózat betöltése: " + e.getMessage());
            }
        }
    }

    private void initializeComponents() {
        stackManager = new StackManager(runMode);
        stackUI = new StackUI(runMode);
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
        if (runMode == RunMode.TRAIN_AI) {
            stackManager.setCurrentSpeed(learningSpeed);
        } else {
            stackManager.setCurrentSpeed(speed);
        }
        GameColorPalette.getInstance().setRandomPalette();
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        tickDown = new GameTimeTicker(stackManager.getCurrentSpeed());
        tickPlay.setPeriodMilliSecond(stackManager.getCurrentSpeed() / 10);
        nextTetromino = createNextTetromino();
        if (runMode != RunMode.TRAIN_AI) {
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

        if (runMode == RunMode.PLAY_AI) {
            StackMetrics metrics = new StackMetrics();
            double[][] possibleStates = stackManager.simulateAllPossibleActions(
                    stackManager.getStackArea(),
                    stackManager.getCurrentTetromino(),
                    metrics
            );
            action = brain.selectAction(possibleStates);
        }
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
        if (tickBackground.tick() && runMode != RunMode.TRAIN_AI) {
            starField.update();
        }
        if (runMode == RunMode.HUMAN) {
            if (tickControl.tick()) {
                handleInput();
            }
        }
        if (tickDown.tick()) {
            if (stackManager.getGameState() == GameState.RUNNING) {
                if (stackManager.getCurrentTetromino() == null) {
                    nextTetromino();
                    if (runMode == RunMode.TRAIN_AI) {
                        trainStep();
                    }
                } else {
                    stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino(), false);
                }
            }
        }

        if (runMode == RunMode.PLAY_AI && tickPlay.tick() && action != null && action.length == 2 &&
                stackManager.getCurrentTetromino() != null) {
            int targetX = action[0];
            int targetRotation = action[1];
            if ((int) stackManager.getTetrominoRotation() != targetRotation) {
                stackManager.rotateTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
            }
            if (stackManager.getCurrentTetromino().getStackCol() != targetX) {
                int moveDirection = Integer.compare(targetX, stackManager.getCurrentTetromino().getStackCol());
                if (moveDirection > 0) {
                    stackManager.moveTetrominoRight(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }
                if (moveDirection < 0) {
                    stackManager.moveTetrominoLeft(stackManager.getStackArea(), stackManager.getCurrentTetromino());
                }
            }
            stackManager.moveTetrominoDown(stackManager.getStackArea(), stackManager.getCurrentTetromino(), false);
        }

        if (stackManager.getGameState() == GameState.GAMEOVER) {
            if (runMode == RunMode.TRAIN_AI) {
                if (!TEST_ALGORITHM_ONLY) {
                    brain.learn(
                            lastState,
                            lastAction,
                            calculateReward(true),
                            null,
                            true,
                            null
                    );

                    try {
                        if (brain.getEpisodeCount() % 100 == 0) {
                            brain.saveNetworkStructure("brain_network.json");
                            brain.saveTrainingState("brain_training.json");
                        }
                    } catch (Exception e) {
                        System.out.println("Error saving Q-Learning Neural Network: " + e.getMessage());
                    }
                    lastState = null;
                    lastAction = null;
                    start();
                }

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

    private void trainStep() {
        double reward = 0;
        if (!TEST_ALGORITHM_ONLY) {
            // 1. előző jutalom kiszámítása
            if (lastState != null && lastAction != null) {
                stackMetrics.calculateGameMetrics(stackManager.getStackArea());
                reward = calculateReward(false);
            }
        }

        // 2. Új állapotok kiszámítása
        StackMetrics metrics = new StackMetrics();
        double[][] possibleStates = stackManager.simulateAllPossibleActions(
                stackManager.getStackArea(),
                stackManager.getCurrentTetromino(),
                metrics
        );

        if (!TEST_ALGORITHM_ONLY) {
            // 3. Tanuljunk az előző akcióból (ha volt)
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
        }

        // 4. Válasszuk ki az új akciót
        int[] action;
        if (possibleStates.length > 0) {
            if (!TEST_ALGORITHM_ONLY) {
                action = brain.selectAction(possibleStates);
            } else {
                action = selectBestState(possibleStates);
            }
            int targetX = action[0];
            int targetRotation = action[1];

            int newStateIndex = targetX * ROTATION_OUTPUTS + targetRotation;
            if (newStateIndex < possibleStates.length) {
                // 5. Az új állapot és akció mentése
                lastState = possibleStates[newStateIndex];
                lastAction = action;

                // 6. Akció végrehajtása
                stackManager.moveAndRotateTetrominoTo(
                        stackManager.getStackArea(),
                        stackManager.getCurrentTetromino(),
                        targetX,
                        targetRotation
                );
                stackMetrics.calculateGameMetrics(stackManager.getStackArea());
            }
        }
    }

    /**
     * Render.
     *
     * @param g2d Graphics2D
     */
    public void render(Graphics2D g2d) {
        if (runMode != RunMode.TRAIN_AI) {
            starField.render(g2d);
        }
        stackUI.setTickAnim(tickAnim.tick());
        stackUI.render(g2d);
    }

    private double calculateReward(Boolean gameOver) {
        double reward;
        stackMetrics.calculateGameMetrics(stackManager.getStackArea());
        if (!gameOver) {
            double rows = stackManager.getAllFullRows() - previousFullRows;
            previousFullRows = rows;
            reward = Math.pow((rows + 1), 1.5);
            return reward;
        } else {
            previousFullRows = 0;
            return 0.0;
        }
    }

    /**
     * Get neural network.
     *
     * @return neural network
     */
    public NeuralNetwork getBrain() {
        if (runMode != RunMode.TRAIN_AI) {
            throw new IllegalStateException("Neural network is not available when learning is disabled.");
        }
        return brain;
    }

    private static int[] selectBestState(double[][] possibleStates) {
        if (possibleStates == null || possibleStates.length == 0 || possibleStates[0].length < 3) {
            throw new IllegalArgumentException("A bemeneti tömb érvénytelen! Legalább 3 oszlopra van szükség.");
        }
        double maxSum = Double.NEGATIVE_INFINITY;
        int[] result = new int[2];

        for (double[] possibleState : possibleStates) {
            double currentSum = 0;
            for (int i = 2; i < possibleState.length; i++) {
                currentSum += possibleState[i];
            }
            if (currentSum > maxSum) {
                maxSum = currentSum;
                result[0] = (int) possibleState[0];
                result[1] = (int) possibleState[1];
            }
        }

        return result;
    }

}
