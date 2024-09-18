package hu.nye.vpe;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * GamePanel class.
 */
public class GamePanel implements GameElement {
    private final String fontName;
    private final int panelX;
    private final int panelY;
    private final int panelWidth;
    private final int panelHeight;
    private final int borderWidth;
    private final Color backColor;
    private final int titleHeight;
    private final String title;

    public GamePanel(int panelX, int panelY, int panelWidth, int panelHeight, int borderWidth, Color backColor,
                     int titleHeight, String title, String fontName) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.borderWidth = borderWidth;
        this.backColor = backColor;
        this.titleHeight = titleHeight;
        this.title = title;
        this.fontName = fontName;
    }

    @Override
    public void update() {

    }

    /**
     * Render method.
     *
     * @param g2D Graphics2D
     *
     */
    public void render(Graphics2D g2D) {
        g2D.setColor(new Color(100, 100, 100, 100));
        g2D.fill3DRect(panelX, panelY, panelWidth, titleHeight, true);
        g2D.setColor(Color.GRAY);
        g2D.setFont(new Font(this.fontName, Font.BOLD, titleHeight / 2 + (titleHeight / 10)));
        g2D.drawString(title, panelX + titleHeight - (titleHeight / 2) - (titleHeight / 8),
                panelY + titleHeight - (titleHeight / 4) - (titleHeight / 12));
        g2D.setColor(new Color(100, 100, 100, 50));
        g2D.fill3DRect(panelX, panelY + titleHeight, panelWidth, panelHeight, true);
        g2D.setColor(new Color(backColor.getRed(), backColor.getGreen(), backColor.getBlue(), 100));
        g2D.fillRect(panelX + borderWidth, panelY + titleHeight + borderWidth,
                panelWidth - (borderWidth * 2), panelHeight - (borderWidth * 2));
    }
}