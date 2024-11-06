package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Replay experiences.
 */
public class ExperienceReplay implements Serializable {
    private final List<Experience> memory;
    private final int capacity;
    private final Random random;

    public ExperienceReplay(int capacity) {
        this.memory = new ArrayList<>(capacity);
        this.capacity = capacity;
        this.random = new Random();
    }

    /**
     * Add experinces to buffer.
     *
     * @param experience experience data
     */
    public void add(Experience experience) {
        if (memory.size() >= capacity) {
            memory.remove(0);
        }
        experience.priority = 1.0;
        memory.add(experience);
    }

    /**
     * List experiences.
     *
     * @param batchSize batch size
     *
     * @return experience batch
     */
    public List<Experience> sample(int batchSize) {
        batchSize = Math.min(batchSize, memory.size());
        List<Experience> batch = new ArrayList<>(batchSize);

        double totalPriority = memory.stream().mapToDouble(e -> e.priority).sum();

        for (int i = 0; i < batchSize; i++) {
            double rand = random.nextDouble() * totalPriority;
            double cumulativePriority = 0.0;
            for (Experience experience : memory) {
                cumulativePriority += experience.priority;
                if (cumulativePriority >= rand) {
                    batch.add(experience);
                    break;
                }
            }

        }
        //int index = random.nextInt(memory.size());
        //batch.add(memory.get(index));
        return batch;
    }

    /**
     * Normalize priorities to be within a specified range.
     *
     * @param minPriority minimum normalized priority
     * @param maxPriority maximum normalized priority
     */
    public void normalizePriorities(double minPriority, double maxPriority) {
        double minCurrentPriority = memory.stream().mapToDouble(e -> e.priority).min().orElse(1.0);
        double maxCurrentPriority = memory.stream().mapToDouble(e -> e.priority).max().orElse(1.0);

        if (maxCurrentPriority > minCurrentPriority) {
            for (Experience experience : memory) {
                experience.priority = minPriority +
                        (experience.priority - minCurrentPriority) *
                                (maxPriority - minPriority) /
                                (maxCurrentPriority - minCurrentPriority);
            }
        } else {
            // If all priorities are the same, set them to the minPriority
            for (Experience experience : memory) {
                experience.priority = minPriority;
            }
        }
    }

    /**
     * Get all experiences from memory.
     *
     * @return List of all experiences
     */
    public List<Experience> getExperiences() {
        return new ArrayList<>(memory);
    }

    /**
     * Clear all experiences from memory.
     */
    public void clear() {
        memory.clear();
    }

    public int size() {
        return memory.size();
    }
}