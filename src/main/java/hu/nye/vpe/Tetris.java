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
    private static final int FEED_DATA_SIZE = ROWS * COLS + 32 + 37;
    private static final int HIDDEN_NODES1 = 128;
    private static final int HIDDEN_NODES2 = 64;
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
                brain = new NeuralNetworkQLearning(FEED_DATA_SIZE, HIDDEN_NODES1, HIDDEN_NODES2, OUTPUT_NODES);
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
        Shape nb = sf.getRandomShape();
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
        Cell[][] stackArea = stack.getStackArea();
        double[] feedData = new double[FEED_DATA_SIZE];
        int k = 0;
        for (Cell[] cells : stackArea) {
            for (Cell cell : cells) {
                feedData[k++] = cell.getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId() ? 1.0 : 0.0;
            }
        }
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getStackRow();
        } else {
            feedData[k++] = 0.0;
        }
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getStackCol();
        } else {
            feedData[k++] = 0.0;
        }
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

        feedData[k++] = stack.countNearlyFullRows();
        feedData[k++] = stack.countBlockedRows();

        int columnHeights[] = stack.calculateColumnHeights();
        for (double height : columnHeights) {
            feedData[k++] = height;
        }

        int[] heightDifferences = stack.calculateHeightDifferences();
        for (double diff : heightDifferences) {
            feedData[k++] = diff;
        }
        feedData[k++] = stack.countHoles();
        feedData[k++] = stack.calculateHolesSurroundings();
        feedData[k++] = stack.countStableRows();
        feedData[k++] = stack.getNoFullRows();
        feedData[k++] = stack.getGameScore();
        feedData[k++] = stack.getGameLevel();
        feedData[k++] = stack.getAllFullRows();
        feedData[k++] = stack.countHoles();
        feedData[k++] = stack.calculateMaxHeight();
        feedData[k++] = stack.calculateBumpiness();
        InputNormalizer normalizer = new InputNormalizer(FEED_DATA_SIZE);
        double[] normalizedData = normalizer.normalizeAutomatically (feedData);
        for (int i = 0; i < normalizedData.length; i++) {
            if (Double.isNaN(normalizedData[i])) {
                System.out.println("NaN detektálva normalizálás után! Index: " + k);
            }
        }
        return normalizedData;
    }

    private int interpretOutput(double[] output) {
        int bestMove = 0;
        double maxValue = output[0];
        for (int i = 1; i < output.length; i++) {
            if (output[i] > maxValue) {
                maxValue = output[i];
                bestMove = i;
            }
        }
        return bestMove;
    }

    private double calculateReward() {
        int score = stack.getGameScore();
        int clearedLines = stack.getAllFullRows();
        int holes = stack.countHoles();
        int maxHeight = stack.calculateMaxHeight();
        double bumpiness = stack.calculateBumpiness();
        int nearlyFullRows = stack.countNearlyFullRows();
        int blockedRows = stack.countBlockedRows();
        int stableRows = stack.countStableRows();
        double elapsedTime = stack.getElapsedTimeLong();

        double reward = 0;
        reward += clearedLines * 10;
        reward += 65 - holes;
        reward += 24 - maxHeight;
        reward += 24 - bumpiness;
        reward += nearlyFullRows * 0.2;
        reward += 24 - blockedRows;
        reward += stableRows * 0.2;
        reward += score * 0.05;
        reward += Math.log(elapsedTime + 1) * 0.05;
        //reward = Math.max(-100, Math.min(100, reward));
        return reward - 30;
    }

}
