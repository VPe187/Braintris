package hu.nye.vpe.nn;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import hu.nye.vpe.gaming.GameElement;

/***
 * Neural network visualization class.
 */
public class NeuralNetworkVisualization implements GameElement {
    private static final int LINE_RATE = 10000;
    private static final int NODE_SIZE = 6;
    private static final int LAYER_DISTANCE = 200;
    private static final int STAT_X = 500;
    private static final int UP_OFFSET = 460;
    private static String FONT_NAME = "Truly Madly Dpad";
    private static final Color INACTIVE_NODE_COLOR = new Color(100, 100, 100, 100);
    private static final Color ACTIVE_NODE_COLOR = new Color(255, 255, 255, 200);
    private static final Color INACTIVE_OUTPUT_NODE_COLOR = new Color(0, 100, 0, 100);
    private static final Color ACTIVE_OUTPUT_NODE_COLOR = new Color(0, 255, 0, 200);
    private static final Color FONT_COLOR = new Color(255, 255, 255, 120);
    private final NeuralNetworkQLearning network;
    private final int width;
    private final int height;
    private double[][][] weights;
    private int[] layerSizes;
    private double[][] activations;


    public NeuralNetworkVisualization(NeuralNetworkQLearning network, int width, int height) {
        this.network = network;
        this.width = width;
        this.height = height;
        updateNetworkData();
    }

    @Override
    public void update() {
        updateNetworkData();
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        drawNetwork(g2d);
        drawStats(g2d);
    }

    private void updateNetworkData() {
        this.weights = network.getAllWeights();
        this.layerSizes = new int[weights.length + 1];
        for (int i = 0; i < weights.length; i++) {
            layerSizes[i] = weights[i].length;
        }
        layerSizes[layerSizes.length - 1] = weights[weights.length - 1][0].length;
        this.activations = network.getLastActivations();
    }

    private void drawNetwork(Graphics2D g2d) {
        for (int i = 0; i < layerSizes.length - 1; i++) {
            int nodeCount = layerSizes[i];
            int nextNodeCount = layerSizes[i + 1];
            int startY = height / 2 - (nodeCount * (NODE_SIZE + 5)) / 2;
            int nextStartY = height / 2 - (nextNodeCount * (NODE_SIZE + 5)) / 2;

            for (int j = 0; j < nodeCount; j++) {
                int x = i * LAYER_DISTANCE + 50;
                int y = startY + j * (NODE_SIZE + 5);

                // Kapcsolatok rajzolása a következő réteggel
                for (int k = 0; k < nextNodeCount; k++) {
                    int nextX = (i + 1) * LAYER_DISTANCE + 50;
                    int nextY = nextStartY + k * (NODE_SIZE + 5);

                    double weight = weights[i][j][k];
                    Color lineColor = getColorForWeight(weight);
                    g2d.setColor(lineColor);
                    g2d.draw(new Line2D.Double(x + NODE_SIZE, y + NODE_SIZE / 2, nextX, nextY + NODE_SIZE / 2));
                }

                // Csomópontok rajzolása (kitöltött négyzetek)
                double activation = activations[i][j];
                Color nodeColor = getColorForActivation(activation);
                g2d.setColor(nodeColor);
                g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
            }
        }

        // Az utolsó réteg csomópontjainak rajzolása
        int lastLayerIndex = layerSizes.length - 1;
        int lastLayerNodeCount = layerSizes[lastLayerIndex];
        int lastLayerStartY = height / 2 - (lastLayerNodeCount * (NODE_SIZE + 5)) / 2;

        int maxOutputIndex = 0;
        double maxOutputValue = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < lastLayerNodeCount; j++) {
            if (activations[lastLayerIndex][j] > maxOutputValue) {
                maxOutputValue = activations[lastLayerIndex][j];
                maxOutputIndex = j;
            }
        }

        for (int j = 0; j < lastLayerNodeCount; j++) {
            int x = lastLayerIndex * LAYER_DISTANCE + 50;
            int y = lastLayerStartY + j * (NODE_SIZE + 5);
            Color nodeColor;
            if (j == maxOutputIndex) {
                nodeColor = ACTIVE_OUTPUT_NODE_COLOR;
            } else {
                nodeColor = INACTIVE_OUTPUT_NODE_COLOR;
            }
            g2d.setColor(nodeColor);
            g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
        }
    }

    private Color getColorForWeight(double weight) {
        weight = Math.tanh(weight * LINE_RATE);
        int colorValue = (int) (((weight + 1) / 2) * 255);

        return new Color(colorValue, colorValue, colorValue, 50);
    }

    private Color getColorForActivation(double activation) {
        // Ensure activation is between 0 and 1
        activation = Math.max(0, Math.min(1, network.activateHidden(activation)));

        // Interpolate between INACTIVE_NODE_COLOR and ACTIVE_NODE_COLOR based on activation
        int r = (int) (INACTIVE_NODE_COLOR.getRed() + (ACTIVE_NODE_COLOR.getRed() - INACTIVE_NODE_COLOR.getRed()) * activation);
        int g = (int) (INACTIVE_NODE_COLOR.getGreen() + (ACTIVE_NODE_COLOR.getGreen() - INACTIVE_NODE_COLOR.getGreen()) * activation);
        int b = (int) (INACTIVE_NODE_COLOR.getBlue() + (ACTIVE_NODE_COLOR.getBlue() - INACTIVE_NODE_COLOR.getBlue()) * activation);
        int a = (int) (INACTIVE_NODE_COLOR.getAlpha() + (ACTIVE_NODE_COLOR.getAlpha() - INACTIVE_NODE_COLOR.getAlpha()) * activation);
        return new Color(r, g, b, a);
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(FONT_COLOR);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));

        String maxQ = String.format("Nextq: %.8f", network.getMaxNextQValue());
        g2d.drawString(maxQ, STAT_X - UP_OFFSET, height - 720);
        if (network.getRms() > 1.0) {
            g2d.setColor(Color.ORANGE);
        } else if (network.getRms() < 1e-7) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.GREEN);
        }

        g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        String rms = String.format("RMS: %.8f", network.getRms());
        g2d.drawString(rms, STAT_X - UP_OFFSET, height - 700);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        g2d.setColor(FONT_COLOR);

        String hidden1 = String.format("H1: Min: %.4f, Max: %.4f, Mean: %.4f", network.getHidden1Min(),
                network.getHidden1Max(), network.getHidden1Mean());
        g2d.drawString(hidden1, STAT_X - UP_OFFSET, height - 680);

        String hidden2 = String.format("H2: Min: %.4f, Max: %.4f, Mean: %.4f", network.getHidden2Min(),
                network.getHidden2Max(), network.getHidden2Mean());
        g2d.drawString(hidden2, STAT_X - UP_OFFSET, height - 660);

        String output = String.format("Out: Min: %.4f, Max: %.4f, Mean: %.4f", network.getOutputMin(),
                network.getOutputMax(), network.getOutputMean());
        g2d.drawString(output, STAT_X - UP_OFFSET, height - 640);

        // Down
        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());
        String reward = String.format("Reward: %.0f", network.getReward());
        String bestReward = String.format("Best reward: %.0f", network.getBestScoreValue());
        String episodes = String.format("Episodes: %d", network.getEpisodeCount());
        g2d.drawString(episodes, STAT_X, height - 140);
        g2d.drawString(bestReward, STAT_X, height - 120);
        g2d.drawString(reward, STAT_X, height - 100);
        g2d.drawString(learningRate, STAT_X, height - 80);
        g2d.drawString(epsilon, STAT_X, height - 60);
        g2d.drawString(discountFactor, STAT_X, height - 40);

    }
}