package hu.nye.vpe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Stack class.
 */
public class Stack implements GameElement {
    private final Color colorStackBackground = new Color(15, 15, 15);
    private final Color colorStackBorder = new Color(100, 100, 100, 100);
    private final int rowOffset = 2;
    private final int stackX;
    private final int stackY;
    private final int stackW;
    private final int stackH;
    private final int blockSize = 30;
    private final int cols = 12;
    private final int rows = 24;

    public Stack() {
        stackX = 2 * this.blockSize;
        stackY = 2 * this.blockSize;
        stackW = (this.cols * this.blockSize);
        stackH = ((this.rows - rowOffset) * this.blockSize);
    }

    @Override
    public void update() {

    }

    @Override
    public void render(Graphics2D g2D) {
        renderPlayArea(g2D);
    }

    private void renderPlayArea(Graphics2D g2D) {
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setColor(colorStackBackground);
        g2D.setColor(new Color(colorStackBackground.getRed(), colorStackBackground.getGreen(), colorStackBackground.getBlue(), 190));
        g2D.fillRect(stackX, stackY, stackW, stackH);
        g2D.setColor(colorStackBorder);
        for (int k = 1; k <= cols; k++) {
            g2D.fill3DRect((stackX - blockSize) + (k * blockSize), stackY - blockSize, blockSize, blockSize, true);
            g2D.fill3DRect((stackX - blockSize) + (k * blockSize), stackY + ((rows - rowOffset) * blockSize), blockSize, blockSize, true);
        }
        for (int k = rowOffset; k <= rows + 1; k++) {
            g2D.fill3DRect(stackX - blockSize, (stackY - blockSize) + ((k - rowOffset) * blockSize), blockSize, blockSize, true);
            g2D.fill3DRect(
                    (stackX - blockSize) + ((cols + 1) * blockSize),
                    (stackY - blockSize) + ((k - rowOffset) * blockSize),
                    blockSize, blockSize,
                    true
            );
        }
    }

}
