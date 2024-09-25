package hu.nye.vpe.gaming;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * Starfield class.
 */
public class GameStarfield implements GameElement {
    private static final int MAX_STARS = 500;
    private final int width;
    private final int height;
    private final ArrayList<int[]> stars;
    private Color[] colorPalette = {
            Color.decode("#374570"), Color.decode("#11678F"), Color.decode("#08B7D6"), Color.decode("#89F1F5"),
            Color.decode("#FFFFFF"), Color.decode("#FFDCC1"), Color.decode("#F9A361"), Color.decode("#FB7292"),
            Color.decode("#CC4778"), Color.decode("#7C2749")
    };

    public GameStarfield(int width, int height) {
        this.width = width;
        this.height = height;
        stars = new ArrayList<>(MAX_STARS);
        Random r = new Random();
        for (int i = 0; i < MAX_STARS; i++) {
            int[] star = {r.nextInt(this.width), r.nextInt(this.height), 1 + r.nextInt(3), r.nextInt(9)};
            stars.add(star);
        }
    }

    /**
     * Render method.
     *
     * @param g2D Graphics2D
     *
     */
    public void render(Graphics2D g2D) {
        for (int[] entry : stars) {
            g2D.setColor(Color.WHITE);
            Color[] c = colorPalette;
            g2D.setColor(c[entry[3]]);
            g2D.fillOval(entry[0], entry[1], entry[2], entry[2]);
        }
    }

    /**
     * Update method.
     */
    public void update() {
        for (int[] entry : stars) {
            entry[0] = entry[0] - 1;
            if (entry[0] < 0) {
                entry[0] = this.width;
            }
            entry[1] = entry[1] - 1;
            if (entry[1] < 0) {
                entry[1] = this.height;
            }
        }
    }

    public void setColorPalette(Color[] palette) {
        this.colorPalette = palette;
    }

}
