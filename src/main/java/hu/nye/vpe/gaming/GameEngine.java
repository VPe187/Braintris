package hu.nye.vpe.gaming;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

import hu.nye.vpe.nn.Visualization;
import hu.nye.vpe.tetris.RunMode;
import hu.nye.vpe.tetris.Tetris;

/**
 * Game class. Runnable.
 */
public class GameEngine implements Runnable {
    public static final int FPS = 24;
    private final GameDisplay gameDisplay;
    private final int gameWidth;
    private final int fullWidth;
    public int height;
    public String title;
    private boolean running = false;
    private Thread thread;
    private final Tetris tetris;
    private GameElement nnVisualization;
    private RunMode runMode;

    public GameEngine(String title, int width, int height, RunMode runMode) {
        this.runMode = runMode;
        this.fullWidth = width;
        this.gameWidth = runMode == RunMode.TRAIN_AI ? width / 2 : width;
        this.height = height;
        this.title = title;
        gameDisplay = new GameDisplay(title, this.fullWidth, height);
        tetris = new Tetris(gameWidth, height, gameDisplay.getInput(), this.runMode);
        if (runMode == RunMode.TRAIN_AI) {
            nnVisualization = new Visualization(tetris.getBrain(), gameWidth, height);
        }
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
        if (runMode == RunMode.TRAIN_AI) {
            nnVisualization.update();
        }
    }

    private void render() {
        BufferStrategy bs = gameDisplay.getCanvas().getBufferStrategy();
        if (bs == null) {
            gameDisplay.getCanvas().createBufferStrategy(3);
            return;
        }
        Graphics2D graphics2D = (Graphics2D) bs.getDrawGraphics();
        graphics2D.setColor(Color.BLACK);
        graphics2D.clearRect(0, 0, gameWidth, height);
        graphics2D.fillRect(0, 0, gameWidth, height);
        tetris.render(graphics2D);
        graphics2D.translate(gameWidth, 0);
        if (runMode == RunMode.TRAIN_AI) {
            nnVisualization.render(graphics2D);
        }
        bs.show();
        graphics2D.dispose();
    }

    @Override
    public void run() {
        double timePerTick = (double) 1000000000 / FPS;
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
}
