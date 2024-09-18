package hu.nye.vpe.gaming;

import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;

/**
 * Display class.
 */
public class GameDisplay {

    private JFrame frame;
    private Canvas canvas;
    private String title;
    private int width;
    private int height;
    private GameInput gameInput;

    public GameDisplay(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
        createDisplay();
    }

    private void createDisplay() {
        frame = new JFrame(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        JFrame.setDefaultLookAndFeelDecorated(true);
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setMaximumSize(new Dimension(width, height));
        canvas.setMinimumSize(new Dimension(width, height));
        canvas.setFocusable(true);
        gameInput = new GameInput();
        canvas.addKeyListener(gameInput);
        frame.add(canvas);
        frame.pack();
        canvas.requestFocus();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public JFrame getFrame() {
        return frame;
    }

    public GameInput getInput() {
        return gameInput;
    }
}
