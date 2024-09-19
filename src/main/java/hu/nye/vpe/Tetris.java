package hu.nye.vpe;

import java.awt.Graphics2D;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;

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
    private final GameTimeTicker tickDown;
    private final GameTimeTicker tickBackground;
    private final GameTimeTicker tickControl;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private final long dropSpeed = 10L;
    private final long startSpeed = 1000L;
    private long currentSpeed = 1000L;
    private boolean musicOn = true;

    public Tetris(int width, int height, GameInput gameInput) {
        tickDown = new GameTimeTicker(1000);
        tickBackground = new GameTimeTicker(60);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        this.gameInput = gameInput;
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack = new Stack();
        stack.start();
        nextShape = createNextShape();
        startTime = System.currentTimeMillis();
        gameAudio.musicBackgroundPlay();
        musicOn = true;
        currentSpeed = startSpeed;
        nextShape();
    }

    private void nextShape() {
        currentShape = nextShape;
        nextShape = createNextShape();
        stack.setShapes(currentShape, nextShape);
        stack.putShape();
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
                if (stack.getState() != State.GAMEOVER && stack.getState() != State.PAUSED) {
                    if (stack.getCurrentShape() == null) {
                        nextShape();
                    } else {
                        stack.moveShapeDown();
                    }
                }
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
        if (stack.getState() == State.RUNNING) {
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
                tickDown.setPeriodMilliSecond(1000);
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
}
