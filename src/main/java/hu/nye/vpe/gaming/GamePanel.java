package hu.nye.vpe.gaming;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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
    private String title;

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
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2D.setColor(new Color(0, 0, 0, 40));
        g2D.fillRect(panelX + 4, panelY + 4, panelWidth, panelHeight + titleHeight);

        g2D.setColor(new Color(50, 50, 50, 180));
        g2D.fillRect(panelX, panelY, panelWidth, panelHeight + titleHeight);

        renderTitleSegments(g2D);
        renderContentSegments(g2D);
        renderRivets(g2D);

        g2D.setColor(new Color(20, 20, 20, 140));
        g2D.fillRect(panelX + borderWidth, panelY + titleHeight + borderWidth,
                panelWidth - (borderWidth * 2), panelHeight - (borderWidth * 2));

        renderTitle(g2D);
    }

    private void renderTitleSegments(Graphics2D g2D) {
        int halfWidth = panelWidth / 2;

        Color baseColor = new Color(80, 80, 80, 180);
        Color lightColor = new Color(100, 100, 100, 180);

        g2D.setColor(lightColor);
        g2D.fill3DRect(panelX, panelY, halfWidth - 1, titleHeight / 2, true);

        g2D.setColor(baseColor);
        g2D.fill3DRect(panelX + halfWidth + 1, panelY, halfWidth - 1, titleHeight / 2, true);

        g2D.setColor(baseColor);
        g2D.fill3DRect(panelX, panelY + titleHeight / 2, halfWidth - 1, titleHeight / 2, true);

        g2D.setColor(lightColor);
        g2D.fill3DRect(panelX + halfWidth + 1, panelY + titleHeight / 2, halfWidth - 1, titleHeight / 2, true);
    }

    private void renderContentSegments(Graphics2D g2D) {
        int halfWidth = panelWidth / 2;
        int contentY = panelY + titleHeight;
        int halfHeight = panelHeight / 2;

        Color baseColor = new Color(60, 60, 60, 160);
        Color darkColor = new Color(40, 40, 40, 160);

        g2D.setColor(baseColor);
        g2D.fill3DRect(panelX, contentY, halfWidth - 1, halfHeight - 1, true);

        g2D.setColor(darkColor);
        g2D.fill3DRect(panelX + halfWidth + 1, contentY, halfWidth - 1, halfHeight - 1, true);

        g2D.setColor(darkColor);
        g2D.fill3DRect(panelX, contentY + halfHeight + 1, halfWidth - 1, halfHeight - 1, true);

        g2D.setColor(baseColor);
        g2D.fill3DRect(panelX + halfWidth + 1, contentY + halfHeight + 1, halfWidth - 1, halfHeight - 1, true);
    }

    private void renderRivets(Graphics2D g2D) {
        int rivetSize = 8;
        int rivetPadding = 6;

        Color rivetBase = new Color(100, 100, 100, 200);
        Color rivetHighlight = new Color(180, 180, 180, 150);
        Color rivetShadow = new Color(30, 30, 30, 100);

        int[][] rivetPositions = {
                {panelX + rivetPadding, panelY + rivetPadding},
                {panelX + panelWidth - rivetPadding - rivetSize, panelY + rivetPadding},
                {panelX + rivetPadding, panelY + titleHeight - rivetPadding - rivetSize},
                {panelX + panelWidth - rivetPadding - rivetSize, panelY + titleHeight - rivetPadding - rivetSize},
                {panelX + rivetPadding, panelY + titleHeight + panelHeight - rivetPadding - rivetSize},
                {panelX + panelWidth - rivetPadding - rivetSize, panelY + titleHeight + panelHeight - rivetPadding - rivetSize}
        };

        for (int[] pos : rivetPositions) {
            g2D.setColor(rivetShadow);
            g2D.fillOval(pos[0] + 1, pos[1] + 1, rivetSize, rivetSize);

            g2D.setColor(rivetBase);
            g2D.fillOval(pos[0], pos[1], rivetSize, rivetSize);

            g2D.setColor(rivetHighlight);
            g2D.fillOval(pos[0] + 2, pos[1] + 2, rivetSize / 2, rivetSize / 2);
        }
    }

    private void renderTitle(Graphics2D g2D) {
        g2D.setFont(new Font(this.fontName, Font.BOLD, titleHeight / 2 + (titleHeight / 10)));
        g2D.setColor(new Color(0, 0, 0, 100));
        g2D.drawString(title,
                panelX + titleHeight - (titleHeight / 2) - (titleHeight / 8) + 1,
                panelY + titleHeight - (titleHeight / 4) - (titleHeight / 12) + 1);

        g2D.setColor(new Color(220, 220, 220, 220));
        g2D.drawString(title,
                panelX + titleHeight - (titleHeight / 2) - (titleHeight / 8),
                panelY + titleHeight - (titleHeight / 4) - (titleHeight / 12));
    }


    public int getPanelX() {
        return panelX;
    }

    public int getPanelY() {
        return panelY;
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getPanelHeight() {
        return panelHeight;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public int getTitleHeight() {
        return titleHeight;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}