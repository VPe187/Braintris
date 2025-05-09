package hu.nye.vpe.gaming;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * Display class.
 */
public class GameDisplay {
    private static final String FONT_FILE = "fonts/trulymadlydpad.ttf";
    private Canvas canvas;
    private final String title;
    private final int width;
    private final int height;
    private GameInput gameInput;

    public GameDisplay(String title, int width, int height) {
        loadFont();
        this.title = title;
        this.width = width;
        this.height = height;
        createDisplay();
    }

    private void createDisplay() {
        JFrame frame = new JFrame(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        JFrame.setDefaultLookAndFeelDecorated(true);
        ImageIcon img = new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("images/tetris_ico.png")));
        frame.setIconImage(img.getImage());
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

    public GameInput getInput() {
        return gameInput;
    }

    private void loadFont() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(GameDisplay.FONT_FILE);
            if (stream != null) {
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
            }
        } catch (IOException | FontFormatException e) {
            System.out.println("Font load error: " + e);
        }
    }

}
