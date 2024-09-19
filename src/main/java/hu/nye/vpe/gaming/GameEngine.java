package hu.nye.vpe.gaming;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

import hu.nye.vpe.Tetris;

/**
 * Game class. Runnable.
 */
public class GameEngine implements Runnable {
    private final String fontName = "Truly Madly Dpad";
    private final String fontFile = "fonts/trulymadlydpad.ttf";
    public static final int FPS = 60;
    private GameDisplay gameDisplay;
    public int width;
    public int height;
    public String title;
    private boolean running = false;
    private Thread thread;
    private BufferStrategy bs;
    private Graphics2D graphics2D;
    private final Tetris tetris;

    public GameEngine(String title, int width, int height) {
        this.width = width;
        this.height = height;
        this.title = title;
        gameDisplay = new GameDisplay(title, width, height);
        tetris = new Tetris(width, height, gameDisplay.getInput());
    }

    /**
     * Game start.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this);
        thread.start();
        tetris.start();
        gameDisplay.getCanvas().requestFocus();
    }

    /**
     * Game stop.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void update() {
        tetris.update();
    }

    private void render() {
        bs = gameDisplay.getCanvas().getBufferStrategy();
        if (bs == null) {
            gameDisplay.getCanvas().createBufferStrategy(3);
            return;
        }
        graphics2D = (Graphics2D) bs.getDrawGraphics();
        graphics2D.setColor(Color.BLACK);
        graphics2D.clearRect(0, 0, width, height);
        graphics2D.fillRect(0, 0, width, height);
        tetris.render(graphics2D);
        bs.show();
        graphics2D.dispose();
    }

    @Override
    public void run() {
        double timePerTick = 1000000000 / FPS;
        double delta = 0;
        long now;
        long lastTime = System.nanoTime();
        long timer = 0;
        int ticks = 0;
        while (running) {
            now = System.nanoTime();
            delta += (now - lastTime) / timePerTick;
            timer += now - lastTime;
            lastTime = now;
            if (delta >= 1) {
                update();
                render();
                ticks++;
                delta--;
            }
            if (timer >= 1000000000) {
                ticks = 0;
                timer = 0;
            }
        }
        stop();
    }

    public String getFontName() {
        return fontName;
    }
}
