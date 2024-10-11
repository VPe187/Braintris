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
public class Visualization implements GameElement {
    private static final int NODE_SIZE = 6;
    private static final int LAYER_DISTANCE = 150;
    private static final int STAT_X = 500;
    private static final int UP_OFFSET = 460;
    private static String FONT_NAME = "Truly Madly Dpad";
    private static final Color INACTIVE_NODE_COLOR = new Color(100, 100, 100, 100);
    private static final Color ACTIVE_NODE_COLOR = new Color(255, 255, 255, 200);
    private static final Color INACTIVE_OUTPUT_NODE_COLOR = new Color(0, 100, 0, 100);
    private static final Color ACTIVE_OUTPUT_NODE_COLOR = new Color(0, 255, 0, 200);
    private static final Color FONT_COLOR = new Color(255, 255, 255, 120);
    private final NeuralNetwork network;
    private final int width;
    private final int height;
    private double[][][] weights;
    private int[] layerSizes;
    private double[][] activations;
    private double rms;
    private String[] layerNames;
    private double[] layerMins;
    private double[] layerMaxs;
    private double[] layerMeans;

    public Visualization(NeuralNetwork network, int width, int height) {
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
        this.layerSizes = network.getLayerSizes();
        this.activations = network.getLastActivations();
        calculateStatistics();
    }

    private void calculateStatistics() {
        this.rms = calculateRMS(activations[activations.length - 1]);
        this.layerNames = new String[activations.length];
        this.layerMins = new double[activations.length];
        this.layerMaxs = new double[activations.length];
        this.layerMeans = new double[activations.length];

        for (int i = 0; i < activations.length; i++) {
            String name = "";
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0;

            for (double activation : activations[i]) {
                name = network.getLayerNames()[i];
                min = Math.min(min, activation);
                max = Math.max(max, activation);
                sum += activation;
            }

            layerNames[i] = name;
            layerMins[i] = min;
            layerMaxs[i] = max;
            layerMeans[i] = sum / activations[i].length;
        }
    }

    private double calculateRMS(double[] outputs) {
        double sum = 0;
        for (double output : outputs) {
            sum += output * output;
        }
        return Math.sqrt(sum / outputs.length);
    }

    private void drawNetwork(Graphics2D g2d) {
        int totalLayers = layerSizes.length;
        for (int i = 0; i < totalLayers - 1; i++) {
            int nodeCount = layerSizes[i];
            int nextNodeCount = layerSizes[i + 1];
            int startY = height / 2 - (nodeCount * (NODE_SIZE + 5)) / 2;
            int nextStartY = height / 2 - (nextNodeCount * (NODE_SIZE + 5)) / 2;

            // Kapcsolatok rajzolása a következő réteggel
            for (int j = 0; j < nodeCount; j++) {
                int x = i * LAYER_DISTANCE + 50;
                int y = startY + j * (NODE_SIZE + 5);

                for (int k = 0; k < nextNodeCount; k++) {
                    int nextX = (i + 1) * LAYER_DISTANCE + 50;
                    int nextY = nextStartY + k * (NODE_SIZE + 5);
                    if (i < weights.length && k < weights[i].length && j < weights[i][k].length) {
                        double weight = weights[i][k][j];
                        Color lineColor = getColorForWeight(weight);
                        g2d.setColor(lineColor);
                        g2d.draw(new Line2D.Double(x + NODE_SIZE, y + NODE_SIZE / 2, nextX, nextY + NODE_SIZE / 2));
                    }
                }
            }

            // Csomópontok rajzolása
            for (int j = 0; j < nodeCount; j++) {
                int x = i * LAYER_DISTANCE + 50;
                int y = startY + j * (NODE_SIZE + 5);

                Color nodeColor;
                if (i < activations.length && j < activations[i].length) {
                    double activation = activations[i][j];
                    nodeColor = getColorForActivation(activation);
                } else {
                    nodeColor = Color.GRAY; // Default color for nodes without activation
                }

                g2d.setColor(nodeColor);
                g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
            }
        }

        // Az utolsó réteg csomópontjainak rajzolása
        int lastLayer = totalLayers - 1;
        int nodeCount = layerSizes[lastLayer];
        int startY = height / 2 - (nodeCount * (NODE_SIZE + 5)) / 2;
        for (int j = 0; j < nodeCount; j++) {
            int x = lastLayer * LAYER_DISTANCE + 50;
            int y = startY + j * (NODE_SIZE + 5);

            Color nodeColor;
            if (lastLayer < activations.length && j < activations[lastLayer].length) {
                double activation = activations[lastLayer][j];
                nodeColor = getColorForActivation(activation);
            } else {
                nodeColor = Color.GRAY;
            }

            g2d.setColor(nodeColor);
            g2d.fill(new Rectangle2D.Double(x, y, NODE_SIZE, NODE_SIZE));
        }
    }

    private Color getColorForWeight(double weight) {
        double normalizedWeight = Math.tanh(weight);
        int red, green, blue;
        if (normalizedWeight > 0) {
            // Pozitív súlyok: kék árnyalat
            red = (int)(255 * normalizedWeight);
            green = (int)(255 * normalizedWeight);
            blue = (int)(255 * normalizedWeight);
        } else {
            // Negatív súlyok: piros árnyalat
            red = (int)(255 * -normalizedWeight);
            green = (int)(255 * -normalizedWeight);
            blue = (int)(255 * -normalizedWeight);
        }

        // Az átlátszóság a súly abszolút értékétől függ
        int alpha = (int)(40 * Math.abs(normalizedWeight)) + 20;

        return new Color(red, green, blue, alpha);
    }

    private Color getColorForActivation(double activation) {
        activation = Math.max(0, Math.min(1, activation));

        int r = (int) (INACTIVE_NODE_COLOR.getRed() + (ACTIVE_NODE_COLOR.getRed() - INACTIVE_NODE_COLOR.getRed()) * activation);
        int g = (int) (INACTIVE_NODE_COLOR.getGreen() + (ACTIVE_NODE_COLOR.getGreen() - INACTIVE_NODE_COLOR.getGreen()) * activation);
        int b = (int) (INACTIVE_NODE_COLOR.getBlue() + (ACTIVE_NODE_COLOR.getBlue() - INACTIVE_NODE_COLOR.getBlue()) * activation);
        int a = (int) (INACTIVE_NODE_COLOR.getAlpha() + (ACTIVE_NODE_COLOR.getAlpha() - INACTIVE_NODE_COLOR.getAlpha()) * activation);
        return new Color(r, g, b, a);
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(FONT_COLOR);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));

        String maxQ = String.format("Max Q: %.8f", network.getMaxQValue());
        g2d.drawString(maxQ, STAT_X - UP_OFFSET, height - 720);

        if (rms > 1.0) {
            g2d.setColor(Color.ORANGE);
        } else if (rms < 1e-7) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.GREEN);
        }

        g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        String rmsString = String.format("RMS: %.8f", rms);
        g2d.drawString(rmsString, STAT_X - UP_OFFSET, height - 700);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        g2d.setColor(FONT_COLOR);

        for (int i = 0; i < layerSizes.length; i++) {
            String layerStats = String.format("%s: Min: %.4f, Max: %.4f, Mean: %.4f",
                    layerNames[i], layerMins[i], layerMaxs[i], layerMeans[i]);
            g2d.drawString(layerStats, STAT_X - UP_OFFSET, height - 680 + i * 20);
        }

        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());
        String reward = String.format("Reward: %.0f", network.getLastReward());
        String bestReward = String.format("Best reward: %.0f", network.getBestScore());
        String episodes = String.format("Episodes: %d", network.getEpisodeCount());
        g2d.drawString(episodes, STAT_X, height - 140);
        g2d.drawString(bestReward, STAT_X, height - 120);
        g2d.drawString(reward, STAT_X, height - 100);
        g2d.drawString(learningRate, STAT_X, height - 80);
        g2d.drawString(epsilon, STAT_X, height - 60);
        g2d.drawString(discountFactor, STAT_X, height - 40);
    }
}
