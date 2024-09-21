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
    private final long speedAcceleration = 50L;
    private Shape currentShape;
    private Shape nextShape = null;
    private final ShapeFactory sf = ShapeFactory.getInstance();
    private final int rows = 24;
    private final int cols = 12;
    private long startTime;
    private Stack stack;
    private GameTimeTicker tickDown;
    private final GameTimeTicker tickBackground;
    private final GameTimeTicker tickControl;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private final long dropSpeed = 10L;
    private long currentSpeed = 1000L;
    private boolean musicOn = true;
    private NeuralNetwork brain;

    public Tetris(int width, int height, GameInput gameInput) {
        tickBackground = new GameTimeTicker(60);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        this.gameInput = gameInput;
        this.brain = new NeuralNetwork(rows * cols + 5, 10, 4);
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack = new Stack();
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
            nb.setColPosition((cols / 2) - 2);
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
            brain.evolve(stack.getGameScore(), 0.1, 0.2);
            start();
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
                    tickDown.setPeriodMilliSecond(dropSpeed);
                    break;
                default:
                    tickDown.setPeriodMilliSecond(stack.getCurrentSpeed());
                    break;
            }

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
                tickDown.setPeriodMilliSecond(dropSpeed);
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
        } else {
            gameInput.clearBuffer();
            tickDown.setPeriodMilliSecond(currentSpeed);
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
        double[] feedData = new double[rows * cols + 5];
        int k = 0;
        for (int i = 0; i < stackArea.length; i++) {
            for (int j = 0; j < stackArea[i].length; j++) {
                feedData[k] = stackArea[i][j].getShapeId();
                k++;
            }
        }
        if (currentShape != null) {
            feedData[k] = currentShape.getId();
        } else {
            feedData[k] = 0;
        }
        k++;
        if (nextShape != null) {
            feedData[k] = nextShape.getId();
        } else {
            feedData[k] = 0.0;
        }
        k++;
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
