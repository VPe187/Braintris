package hu.nye.vpe;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;

import java.awt.*;

public class Tetris {
    private final long SPEED_ACCELERATION = 50L;
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

    public Tetris(int width, int height, GameInput gameInput) {
        tickDown = new GameTimeTicker(1000);
        tickBackground = new GameTimeTicker(60);
        tickControl = new GameTimeTicker(10);
        starField = new GameStarfield(width, height);
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        this.gameInput = gameInput;
    }

    public void start() {
        stack = new Stack();
        stack.reset();
        nextShape = createNextShape();
        startTime = System.currentTimeMillis();
        gameAudio.musicBackgroundPlay();
        stateRunning = true;
        nextShape();
    }

    public void nextShape() {
        currentShape = nextShape;
        nextShape = createNextShape();
        stack.addShapes(currentShape, nextShape);
        stack.putShape();
    }

    private Shape createNextShape() {
        Shape nb = sf.getRandomShape();
        if (nb != null) {
            nb.setColPosition((cols/2)-2);
            nb.setRowPosition(0);
            nb.rotateRight();
        }
        return nb;
    }

    public void update() {
        if (tickBackground.tick()) {
            starField.update();
        }
        if (tickControl.tick()) {
        }
        if (tickDown.tick()) {
            stack.moveShapeDown();
        }
    }

    public void render(Graphics2D g2d) {
        starField.render(g2d);
        stack.render(g2d);
    }
}
