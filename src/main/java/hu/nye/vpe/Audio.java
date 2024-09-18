package hu.nye.vpe;

import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Audio class.
 */
public class Audio {

    private final Clip clipBackground;
    private final Clip clipDown;
    private final Clip clipClear;
    private final Clip clipLose;
    private final Clip clipPenalty;
    private final Clip clipNextLevel;

    public Audio() {
        this.clipBackground = open("sounds/background.wav");
        this.clipPenalty = open("sounds/penalty.wav");
        this.clipDown = open("sounds/down.wav");
        this.clipClear = open("sounds/clear.wav");
        this.clipLose = open("sounds/lose.wav");
        this.clipNextLevel = open("sounds/nextlevel.wav");
    }

    /**
     * Open audio file.
     *
     * @param audioFile
     *
     * @return Clip
     */
    public final Clip open(String audioFile) {
        Clip clip = null;
        AudioInputStream audioIn;
        try {
            audioIn = AudioSystem.getAudioInputStream(this.getClass().getClassLoader().getResource(audioFile));
            clip = AudioSystem.getClip();
            clip.open(audioIn);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            //e.printStackTrace();
        }
        return clip;
    }

    /**
     * Play method.
     *
     * @param clip sound
     *
     * @param loop loop
     */
    public synchronized void play(Clip clip, boolean loop) {
        if (loop) {
            clip.loop(Integer.MAX_VALUE);
        } else {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void stop(Clip clip) {
        clip.stop();
    }

    public void soundDown() {
        play(clipDown, false);
    }

    public void soundClear() {
        play(clipClear, false);
    }

    public void soundLose() {
        play(clipLose, false);
    }

    public void soundPenalty() {
        play(clipPenalty, false);
    }

    public void soundNextLevel() {
        play(clipNextLevel, false);
    }

    public void musicBackgroundPlay() {
        play(clipBackground, true);
    }

    public void musicBackgroundStop() {
        stop(clipBackground);
    }

}

