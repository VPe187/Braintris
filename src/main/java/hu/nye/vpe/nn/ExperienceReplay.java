package hu.nye.vpe.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExperienceReplay implements Serializable {
    private final List<Experience> memory;
    private final int capacity;
    private final Random random;

    public ExperienceReplay(int capacity) {
        this.memory = new ArrayList<>(capacity);
        this.capacity = capacity;
        this.random = new Random();
    }

    public void add(Experience experience) {
        if (memory.size() >= capacity) {
            memory.remove(0);
        }
        memory.add(experience);
    }

    public List<Experience> sample(int batchSize) {
        batchSize = Math.min(batchSize, memory.size());
        List<Experience> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            int index = random.nextInt(memory.size());
            batch.add(memory.get(index));
        }
        return batch;
    }

    public int size() {
        return memory.size();
    }
}