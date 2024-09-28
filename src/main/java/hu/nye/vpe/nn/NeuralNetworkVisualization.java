package hu.nye.vpe.nn;

import hu.nye.vpe.gaming.GameElement;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class NeuralNetworkVisualization implements GameElement {
    private static final int NODE_SIZE = 6;
    private static final int LAYER_DISTANCE = 150;
    private static final int STAT_X = 500;
    private static String FONT_NAME = "Truly Madly Dpad";
    private static final Color NODE_COLOR = new Color( 255, 255, 255, 200 );
    private static final Color FONT_COLOR = new Color( 255, 255, 255, 120 );
    private final NeuralNetworkQLearning network;
    private final int width;
    private final int height;
    private double[][][] weights;
    private int[] layerSizes;


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
                    g2d.draw(new Line2D.Double(x + NODE_SIZE, y + NODE_SIZE/2, nextX, nextY + NODE_SIZE/2));
                }

                // Csomópontok rajzolása (kitöltött négyzetek)
                g2d.setColor(NODE_COLOR);
                g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
            }
        }

        // Az utolsó réteg csomópontjainak rajzolása
        int lastLayerIndex = layerSizes.length - 1;
        int lastLayerNodeCount = layerSizes[lastLayerIndex];
        int lastLayerStartY = height / 2 - (lastLayerNodeCount * (NODE_SIZE + 5)) / 2;
        for (int j = 0; j < lastLayerNodeCount; j++) {
            int x = lastLayerIndex * LAYER_DISTANCE + 50;
            int y = lastLayerStartY + j * (NODE_SIZE + 5);
            g2d.setColor(NODE_COLOR);
            g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
        }
    }

    private Color getColorForWeight(double weight) {
        weight = Math.tanh(weight * 500);
        int colorValue = (int) (((weight + 1) / 2) * 255);

        return new Color(colorValue, colorValue, colorValue, 50);
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(FONT_COLOR);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());
        String reward = String.format("Reward: %.4f", network.getReward());
        String bestReward = String.format("Best reward: %.4f", network.getBestScoreValue());
        String maxQ = String.format("Nextq: %.4f", network.getMaxNextQValue());

        g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(maxQ, STAT_X, height - 160);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        g2d.setColor(FONT_COLOR);

        g2d.drawString(bestReward, STAT_X, height - 120);
        g2d.drawString(reward, STAT_X, height - 100);
        g2d.drawString(learningRate, STAT_X, height - 80);
        g2d.drawString(epsilon, STAT_X, height - 60);
        g2d.drawString(discountFactor, STAT_X, height - 40);

    }
}