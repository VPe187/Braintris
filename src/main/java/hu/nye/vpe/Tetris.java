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
    private boolean stateRunning;
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
    private final GameStarfield starField;
    private final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private final long dropSpeed = 10L;
    private final long startSpeed = 1000L;

    public Tetris(int width, int height, GameInput gameInput) {
        tickDown = new GameTimeTicker(1000);
        tickBackground = new GameTimeTicker(60);
        tickControl = new GameTimeTicker(10);
        starField = new GameStarfield(width, height);
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        this.gameInput = gameInput;
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack = new Stack();
        stack.reset();
        nextShape = createNextShape();
        startTime = System.currentTimeMillis();
        gameAudio.musicBackgroundPlay();
        stateRunning = true;
        nextShape();
    }

    private void nextShape() {
        currentShape = nextShape;
        nextShape = createNextShape();
        stack.addShapes(currentShape, nextShape);
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

        }
        if (tickDown.tick()) {
            if (stack.getCurrentShape() == null) {
                nextShape();
            } else {
                stack.moveShapeDown();
            }
        }
    }

    public void render(Graphics2D g2d) {
        starField.render(g2d);
        stack.render(g2d);
    }
}
