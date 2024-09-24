package hu.nye.vpe.nn;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Neural network pool class.
 */
public class NeuralNetworkPool {
    private final Queue<NeuralNetwork> pool;
    private final int maxSize;
    private final int inputNodes;
    private final int hiddenNodes1;
    private final int hiddenNodes2;
    private final int outputNodes;

    public NeuralNetworkPool(int maxSize, int inputNodes, int hiddenNodes1, int hiddenNodes2, int outputNodes) {
        this.maxSize = maxSize;
        this.pool = new ConcurrentLinkedQueue<>();
        this.inputNodes = inputNodes;
        this.hiddenNodes1 = hiddenNodes1;
        this.hiddenNodes2 = hiddenNodes2;
        this.outputNodes = outputNodes;

        // Pre-populate the pool
        for (int i = 0; i < maxSize; i++) {
            pool.offer(new NeuralNetwork(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes, false));
        }
    }

    /**
     * Acquire from pool.
     *
     * @return NeuralNetwork
     */
    public NeuralNetwork acquire() {
        NeuralNetwork network = pool.poll();
        if (network == null) {
            network = new NeuralNetwork(inputNodes, hiddenNodes1, hiddenNodes2, outputNodes, false);
        }
        return network;
    }

    /**
     * Release network.
     *
     * @param network NeuralNetwork
     */
    public void release(NeuralNetwork network) {
        if (pool.size() < maxSize) {
            network.reset();
            pool.offer(network);
        }
    }
}