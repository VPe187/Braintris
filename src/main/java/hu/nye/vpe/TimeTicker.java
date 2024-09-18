package hu.nye.vpe;

/**
 * Time ticker class.
 */
public class TimeTicker {

    private long periodMilliSecond;
    private long lastTime;

    public TimeTicker(long periodMilliSecond) {
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
