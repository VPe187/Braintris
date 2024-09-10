package hu.nye.vpe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

/**
 * Game class. Runnable.
 */
public class Game implements Runnable {
    public static final int FPS = 60;
    private Display display;
    public int width;
    public int height;
    public String title;
    private boolean running = false;
    private Thread thread;
    private BufferStrategy bs;
    private Graphics2D graphics2D;
    private final Starfield starField;
    private final Stack stack = new Stack();

    public Game(String title, int width, int height) {
        this.width = width;
        this.height = height;
        this.title = title;
        starField = new Starfield(width, height);
        starField.setColorPalette(ColorPalette.getInstance().getCurrentPalette());
    }

    private void init() {
        display = new Display(title, width, height);
        //display.getFrame().addKeyListener(keyManager);
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

    private void tick() {
        stack.update();
        starField.update();
    }

    private void render() {
        bs = display.getCanvas().getBufferStrategy();
        if (bs == null) {
            display.getCanvas().createBufferStrategy(3);
            return;
        }
        graphics2D = (Graphics2D) bs.getDrawGraphics();
        graphics2D.setColor(Color.BLACK);
        graphics2D.clearRect(0, 0, width, height);
        graphics2D.fillRect(0, 0, width, height);
        // Render game elements
        starField.render(graphics2D);
        stack.render(graphics2D);
        // End render game elements
        bs.show();
        graphics2D.dispose();
    }

    @Override
    public void run() {
        init();
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
                tick();
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
