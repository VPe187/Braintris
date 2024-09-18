package hu.nye.vpe.gaming;

/**
 * Time ticker class.
 */
public class GameTimeTicker {

    private long periodMilliSecond;
    private long lastTime;

    public GameTimeTicker(long periodMilliSecond) {
        this.periodMilliSecond = periodMilliSecond;
        lastTime = System.currentTimeMillis();
    }

    /**
     * Tick.
     *
     * @return boolean
     */
    public boolean tick() {
        if ((System.currentTimeMillis() - lastTime) > periodMilliSecond) {
            lastTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public void setPeriodMilliSecond(long ms) {
        this.periodMilliSecond = ms;
    }

}
