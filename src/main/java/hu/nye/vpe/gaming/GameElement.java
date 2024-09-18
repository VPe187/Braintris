package hu.nye.vpe.gaming;

import java.awt.Graphics2D;

/**
 * GameElement interface.
 */
public interface GameElement {

    public void update();

    public void render(Graphics2D g2D);
}
