package hu.nye.vpe.nn;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import hu.nye.vpe.gaming.GameElement;
import hu.nye.vpe.gaming.GameTimeTicker;

/***
 * Neural network visualization class.
 */
public class Visualization implements GameElement {
    private static final int STAT_X = 5;
    private static final int RIGHT_COLUMN_OFFSET = 20;
    private static final int Y_OFFSET = 20;
    private static final int INFO_SECTION_HEIGHT = 200;
    private static final int MARGIN = 20;
    private static final String FONT_NAME = "Truly Madly Dpad";
    private static final Color INACTIVE_NODE_COLOR = new Color(100, 100, 100, 100);
    private static final Color ACTIVE_NODE_COLOR = new Color(255, 255, 255, 200);
    private static final Color INACTIVE_OUTPUT_NODE_COLOR = new Color(255, 165, 0, 100);
    private static final Color ACTIVE_OUTPUT_NODE_COLOR = new Color(227, 156, 21, 200);
    private static final Color HIGHLIGHTED_OUTPUT_NODE_COLOR = new Color(255, 255, 0, 255);
    private static final Color FONT_COLOR = new Color(255, 255, 255, 120);
    private static final Color RMS_GREEN = new Color(0, 255, 0);
    private static final Color RMS_ORANGE = new Color(255, 165, 0);
    private static final Color RMS_RED = new Color(255, 0, 0);
    private static final int REFRESH_RATE = 100;

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
    private int nodeSize;
    private int nodeDistance;
    private int layerDistance;
    private int networkHeight;
    private int networkStartX;
    private int networkStartY;
    private static GameTimeTicker infoTicker;

    public Visualization(NeuralNetwork network, int width, int height) {
        this.network = network;
        this.width = width;
        this.height = height;

        int layerCount = network.getLayerSizes().length;
        this.layerMins = new double[layerCount];
        this.layerMaxs = new double[layerCount];
        this.layerMeans = new double[layerCount];
        this.layerNames = network.getLayerNames();
        infoTicker = new GameTimeTicker(REFRESH_RATE);

        updateNetworkData();
        calculateDynamicSizing();
    }

    @Override
    public void update() {
        if (infoTicker.tick()) {
            updateNetworkData();
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        drawNetwork(g2d);
        drawStats(g2d);
    }

    private void updateNetworkData() {
        this.layerSizes = network.getLayerSizes();
        this.activations = network.getLastActivations();
        this.weights = network.getAllWeights();
        calculateStatistics();
    }

    private void calculateDynamicSizing() {
        int maxLayerSize = 0;
        for (int size : layerSizes) {
            maxLayerSize = Math.max(maxLayerSize, size);
        }

        int availableHeight = height - INFO_SECTION_HEIGHT - 2 * MARGIN;
        int availableWidth = width - 2 * MARGIN - STAT_X - RIGHT_COLUMN_OFFSET;

        nodeSize = Math.min(12, availableHeight / (maxLayerSize * 2));
        nodeDistance = Math.max(2, (availableHeight - maxLayerSize * nodeSize) / (maxLayerSize + 1));
        layerDistance = availableWidth / (layerSizes.length - 1);
        networkHeight = maxLayerSize * (nodeSize + nodeDistance) - nodeDistance;
        networkStartX = MARGIN + STAT_X;
        networkStartY = MARGIN + (availableHeight - networkHeight) / 2;
    }

    private void drawNetwork(Graphics2D g2d) {
        for (int i = 0; i < layerSizes.length - 1; i++) {
            drawLayerConnections(g2d, i);
        }
        for (int i = 0; i < layerSizes.length; i++) {
            drawLayerNodes(g2d, i);
        }
    }

    private void drawLayerConnections(Graphics2D g2d, int layerIndex) {
        int nodeCount = layerSizes[layerIndex];
        int nextNodeCount = layerSizes[layerIndex + 1];

        for (int j = 0; j < nodeCount; j++) {
            int x = networkStartX + layerIndex * layerDistance;
            int y = networkStartY + calculateNodeY(j, nodeCount);

            for (int k = 0; k < nextNodeCount; k++) {
                int nextX = networkStartX + (layerIndex + 1) * layerDistance;
                int nextY = networkStartY + calculateNodeY(k, nextNodeCount);
                if (layerIndex < weights.length && k < weights[layerIndex].length && j < weights[layerIndex][k].length) {
                    double weight = weights[layerIndex][k][j];
                    Color lineColor = getColorForWeight(weight);
                    g2d.setColor(lineColor);
                    g2d.draw(new Line2D.Double(x + nodeSize, y + (double) nodeSize / 2, nextX,
                            nextY + (double) nodeSize / 2));
                }
            }
        }
    }

    private void drawLayerNodes(Graphics2D g2d, int layerIndex) {
        int nodeCount = layerSizes[layerIndex];
        boolean isOutputLayer = (layerIndex == layerSizes.length - 1);

        int maxOutputIndex = -1;
        if (isOutputLayer) {
            maxOutputIndex = findMaxOutputIndex(activations[layerIndex]);
        }

        for (int j = 0; j < nodeCount; j++) {
            int x = networkStartX + layerIndex * layerDistance;
            int y = networkStartY + calculateNodeY(j, nodeCount);

            Color nodeColor;
            if (layerIndex < activations.length && j < activations[layerIndex].length) {
                double activation = activations[layerIndex][j];
                if (isOutputLayer && j == maxOutputIndex) {
                    nodeColor = HIGHLIGHTED_OUTPUT_NODE_COLOR;
                } else {
                    nodeColor = getColorForActivation(activation, isOutputLayer);
                }
            } else {
                nodeColor = layerIndex == layerSizes.length - 1 ? INACTIVE_OUTPUT_NODE_COLOR : INACTIVE_NODE_COLOR;
            }

            g2d.setColor(nodeColor);
            g2d.fill(new Rectangle2D.Double(x, y, nodeSize, nodeSize));
        }
    }

    private int findMaxOutputIndex(double[] outputActivations) {
        int maxIndex = 0;
        for (int i = 1; i < outputActivations.length; i++) {
            if (outputActivations[i] > outputActivations[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private int calculateNodeY(int nodeIndex, int layerSize) {
        int layerHeight = layerSize * (nodeSize + nodeDistance) - nodeDistance;
        int layerStartY = (networkHeight - layerHeight) / 2;
        return layerStartY + nodeIndex * (nodeSize + nodeDistance);
    }

    private void calculateStatistics() {
        this.rms = calculateRMS(activations[activations.length - 1]);

        for (int i = 0; i < activations.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0;

            for (double activation : activations[i]) {
                min = Math.min(min, activation);
                max = Math.max(max, activation);
                sum += activation;
            }

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

    private Color getColorForWeight(double weight) {
        double normalizedWeight = Math.tanh(weight * 2);
        int red;
        int green;
        int blue;
        if (normalizedWeight > 0) {
            red = (int) (255 * normalizedWeight);
            green = (int) (255 * normalizedWeight);
            blue = (int) (255 * normalizedWeight);
        } else {
            red = (int) (255 * -normalizedWeight);
            green = (int) (255 * -normalizedWeight);
            blue = (int) (255 * -normalizedWeight);
        }
        int alpha = (int) (10 * Math.abs(normalizedWeight)) + 10;
        return new Color(red, green, blue, alpha);
    }

    private Color getColorForActivation(double activation, boolean isOutputLayer) {
        activation = Math.max(0, Math.min(1, activation));
        int r;
        int g;
        int b;
        int a;
        if (isOutputLayer) {
            r = (int) (INACTIVE_OUTPUT_NODE_COLOR.getRed() + (ACTIVE_OUTPUT_NODE_COLOR.getRed() -
                    INACTIVE_OUTPUT_NODE_COLOR.getRed()) * activation);
            g = (int) (INACTIVE_OUTPUT_NODE_COLOR.getGreen() + (ACTIVE_OUTPUT_NODE_COLOR.getGreen() -
                    INACTIVE_OUTPUT_NODE_COLOR.getGreen()) * activation);
            b = (int) (INACTIVE_OUTPUT_NODE_COLOR.getBlue() + (ACTIVE_OUTPUT_NODE_COLOR.getBlue() -
                    INACTIVE_OUTPUT_NODE_COLOR.getBlue()) * activation);
            a = (int) (INACTIVE_OUTPUT_NODE_COLOR.getAlpha() + (ACTIVE_OUTPUT_NODE_COLOR.getAlpha() -
                    INACTIVE_OUTPUT_NODE_COLOR.getAlpha()) * activation);
        } else {
            r = (int) (INACTIVE_NODE_COLOR.getRed() + (ACTIVE_NODE_COLOR.getRed() - INACTIVE_NODE_COLOR.getRed()) * activation);
            g = (int) (INACTIVE_NODE_COLOR.getGreen() + (ACTIVE_NODE_COLOR.getGreen() - INACTIVE_NODE_COLOR.getGreen()) * activation);
            b = (int) (INACTIVE_NODE_COLOR.getBlue() + (ACTIVE_NODE_COLOR.getBlue() - INACTIVE_NODE_COLOR.getBlue()) * activation);
            a = (int) (INACTIVE_NODE_COLOR.getAlpha() + (ACTIVE_NODE_COLOR.getAlpha() - INACTIVE_NODE_COLOR.getAlpha()) * activation);
        }
        return new Color(r, g, b, a);
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(FONT_COLOR);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        FontMetrics metrics = g2d.getFontMetrics();
        drawLeftColumn(g2d);
        drawRightColumn(g2d, metrics);
    }

    private void drawLeftColumn(Graphics2D g2d) {
        String maxQ = String.format("Max Q: %.8f", network.getMaxQValue());
        g2d.drawString(maxQ, STAT_X, height - 9 * Y_OFFSET);

        Color rmsColor;
        if (rms <= 10) {
            rmsColor = RMS_GREEN;
        } else if (rms < 100) {
            rmsColor = RMS_ORANGE;
        } else {
            rmsColor = RMS_RED;
        }

        g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        g2d.setColor(rmsColor);
        String rmsString = String.format("RMS: %.8f", rms);
        g2d.drawString(rmsString, STAT_X, height - 8 * Y_OFFSET);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        g2d.setColor(FONT_COLOR);

        for (int i = 0; i < layerSizes.length; i++) {
            String layerStats = String.format("%s: Min: %.4f, Max: %.4f, Mean: %.4f",
                    layerNames[i], layerMins[i], layerMaxs[i], layerMeans[i]);
            g2d.drawString(layerStats, STAT_X, (height - 7 * Y_OFFSET) + (i * Y_OFFSET));
        }
    }

    private void drawRightColumn(Graphics2D g2d, FontMetrics metrics) {
        String episodes = String.format("Episodes: %d", network.getEpisodeCount());
        String movingAverage = String.format("Average reward: %.4f", network.getMovingAverage());
        String bestReward = String.format("Best episode reward: %.0f", network.getBestScore());
        String reward = String.format("Step reward: %.4f",  network.getLastReward());
        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());

        int rightColumnX = width - STAT_X - RIGHT_COLUMN_OFFSET;
        g2d.drawString(episodes, rightColumnX - metrics.stringWidth(episodes), height - 160);
        g2d.drawString(movingAverage, rightColumnX - metrics.stringWidth(movingAverage), height - 140);
        g2d.drawString(bestReward, rightColumnX - metrics.stringWidth(bestReward), height - 120);
        g2d.drawString(reward, rightColumnX - metrics.stringWidth(reward), height - 100);
        g2d.drawString(learningRate, rightColumnX - metrics.stringWidth(learningRate), height - 80);
        g2d.drawString(epsilon, rightColumnX - metrics.stringWidth(epsilon), height - 60);
        g2d.drawString(discountFactor, rightColumnX - metrics.stringWidth(discountFactor), height - 40);
    }
}
