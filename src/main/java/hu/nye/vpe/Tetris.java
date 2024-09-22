package hu.nye.vpe;

import java.awt.Graphics2D;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;
import hu.nye.vpe.nn.NeuralNetwork;

/**
 * Tetris class.
 */
public class Tetris {
    private static final int ROWS = 24;
    private static final int COLS = 12;
    private static final long DROP_SPEED = 10L;
    private static final int SHAPE_SIZE = 4 * 4;
    private static final int FEED_DATA_SIZE = ROWS * COLS + SHAPE_SIZE + SHAPE_SIZE + 3;
    private long speed = 1000L;
    private long learningSpeed = 20L;
    private Shape currentShape;
    private Shape nextShape = null;
    private final ShapeFactory sf = ShapeFactory.getInstance();
    private long startTime;
    private Stack stack;
    private GameTimeTicker tickDown;
    private final GameTimeTicker tickBackground;
    private final GameTimeTicker tickControl;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private boolean musicOn = true;
    private NeuralNetwork brain;
    private boolean learning;

    public Tetris(int width, int height, GameInput gameInput, boolean learning) {
        tickBackground = new GameTimeTicker(60);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        this.gameInput = gameInput;
        this.learning = learning;
        if (this.learning) {
            this.brain = new NeuralNetwork(FEED_DATA_SIZE, 140, 70, 4);
        }
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack = new Stack(learning);
        if (learning) {
            stack.setCurrentSpeed(learningSpeed);
        } else {
            stack.setCurrentSpeed(speed);
        }
        GameColorPalette.getInstance().setRandomPalette();
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        tickDown = new GameTimeTicker(stack.getCurrentSpeed());
        stack.start();
        nextShape = createNextShape();
        startTime = System.currentTimeMillis();
        gameAudio.musicBackgroundPlay();
        musicOn = true;
        nextShape();
    }

    private void nextShape() {
        currentShape = nextShape;
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
                        stack.moveShapeDown();
                    }
                }
            }
        }
        if (stack.getState() == State.GAMEOVER) {
            if (learning) {
                double score = stack.getGameScore();
                int clearedLines = stack.getAllFullRows();
                int holes = stack.countHoles();
                int maxHeight = stack.calculateMaxHeight();
                double bumpiness = stack.calculateBumpiness();
                brain.evolve(score, clearedLines, holes, maxHeight, bumpiness);
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
                int move = interpretOutput(brain.feedForward(getFeedData()));
                switch (move) {
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
        stack.removeShape();
        int k = 0;
        for (int i = 0; i < stackArea.length; i++) {
            for (int j = 0; j < stackArea[i].length; j++) {
                feedData[k] = stackArea[i][j].getShapeId();
                k++;
            }
        }
        stack.putShape();
        double currentShapeData[] = ShapeFactory.getInstance().shapeToArray(stack.getCurrentShape());
        for (int i = 0; i < currentShapeData.length; i++) {
            feedData[k] = currentShapeData[i];
            k++;
        }
        double nextShapeData[] = ShapeFactory.getInstance().shapeToArray(nextShape);
        for (int i = 0; i < nextShapeData.length; i++) {
            feedData[k] = nextShapeData[i];
            k++;
        }
        feedData[k] = stack.getNoFullRows();
        k++;
        feedData[k] = stack.getGameScore();
        k++;
        feedData[k] = stack.getGameLevel();
        return feedData;
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
}
