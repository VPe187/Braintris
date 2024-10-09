package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Experience relay class.
 */
public class ExperienceReplay implements Serializable {
    private static final int BUFFER_SIZE = 10000;
    private List<Experience> buffer;
    private Random random;

    public ExperienceReplay() {
        this.buffer = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Add experience.
     *
     * @param state Input data, metrics
     *
     * @param action Action
     *
     * @param reward Reward
     *
     * @param nextState NextState
     *
     * @param done Done?
     */
    public void addExperience(double[] state, int action, double reward, double[] nextState, boolean done) {
        if (buffer.size() >= BUFFER_SIZE) {
            buffer.remove(0);
        }
        buffer.add(new Experience(state, action, reward, nextState, done));
    }

    /**
     * Sample batch.
     *
     * @param batchSize Batch array size
     *
     * @return Experiences list
     */
    public List<Experience> sampleBatch(int batchSize) {
        List<Experience> batch = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            if (!buffer.isEmpty()) {
                int index = random.nextInt(buffer.size());
                batch.add(buffer.get(index));
            }
        }
        return batch;
    }

    public int size() {
        return buffer.size();
    }

    /**
     * Experience class.
     */
    public static class Experience implements Serializable {
        public double[] state;
        public int action;
        public double reward;
        public double[] nextState;
        public boolean done;

        public Experience(double[] state, int action, double reward, double[] nextState, boolean done) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
        }
    }
}
