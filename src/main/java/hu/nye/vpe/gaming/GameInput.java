package hu.nye.vpe.gaming;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import javax.swing.Timer;

/**
 * Keyboard input class.
 */
public class GameInput implements KeyListener, ActionListener {
    private final boolean[] keys = new boolean[600];
    private final Timer repeatTimer;

    public GameInput() {
        repeatTimer = new Timer(5, this);
        repeatTimer.start();
    }

    /**
     * Left.
     *
     * @return pressed
     */
    public boolean left() {
        boolean key = keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_NUMPAD4];
        keys[KeyEvent.VK_LEFT] = false;
        keys[KeyEvent.VK_NUMPAD4] = false;
        return key;
    }

    /**
     * Right.
     *
     * @return pressed
     */
    public boolean right() {
        boolean key = keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_NUMPAD6];
        keys[KeyEvent.VK_RIGHT] = false;
        keys[KeyEvent.VK_NUMPAD6] = false;
        return key;
    }

    /**
     * Up.
     *
     * @return pressed
     */
    public boolean up() {
        boolean key = keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_NUMPAD8];
        keys[KeyEvent.VK_UP] = false;
        keys[KeyEvent.VK_NUMPAD8] = false;
        return key;
    }

    /**
     * Down.
     *
     * @return pressed
     */
    public boolean down() {
        boolean key = keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_NUMPAD2];
        keys[KeyEvent.VK_DOWN] = false;
        keys[KeyEvent.VK_NUMPAD2] = false;
        return key;
    }

    /**
     * Down repeat.
     *
     * @return boolean
     */
    public boolean downRepeat() {
        boolean key = keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_NUMPAD2];
        return key;
    }

    /**
     * Up repeat.
     *
     * @return boolean
     */
    public boolean upRepeat() {
        boolean key = keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_NUMPAD8];
        return key;
    }

    /**
     * Space.
     *
     * @return boolean
     */
    public boolean space() {
        boolean key = keys[KeyEvent.VK_SPACE] || keys[KeyEvent.VK_NUMPAD5];
        keys[KeyEvent.VK_SPACE] = false;
        keys[KeyEvent.VK_NUMPAD5] = false;
        return key;
    }

    /**
     * P (pause).
     *
     * @return boolean
     */
    public boolean letterP() {
        boolean key = keys[KeyEvent.VK_P];
        keys[KeyEvent.VK_P] = false;
        return key;
    }

    /**
     * M (music on / off).
     *
     * @return boolean
     */
    public boolean letterM() {
        boolean key = keys[KeyEvent.VK_M];
        keys[KeyEvent.VK_M] = false;
        return key;
    }

    /**
     * R (restart).
     *
     * @return boolean
     */
    public boolean letterR() {
        boolean key = keys[KeyEvent.VK_R];
        keys[KeyEvent.VK_R] = false;
        return key;
    }

    public void clearBuffer() {
        Arrays.fill(keys, false);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        keys[keyEvent.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        keys[keyEvent.getKeyCode()] = false;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
    }
}

