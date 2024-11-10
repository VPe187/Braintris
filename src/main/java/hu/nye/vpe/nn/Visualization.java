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
    private static final Color INACTIVE_OUTPUT_NODE_COLOR = new Color(120, 120, 120, 100);
    private static final Color ACTIVE_OUTPUT_NODE_COLOR = new Color(227, 156, 21, 200);
    private static final Color HIGHLIGHTED_OUTPUT_NODE_COLOR = new Color(255, 255, 0, 255);
    private static final Color FONT_COLOR = new Color(255, 255, 255, 120);
    private static final Color RMS_GREEN = new Color(0, 255, 0);
    private static final Color RMS_ORANGE = new Color(255, 165, 0);
    private static final Color RMS_RED = new Color(255, 0, 0);
    private static final Color POSITIVE_CHANGE_COLOR = new Color(255, 255, 255, 200);
    private static final Color NEGATIVE_CHANGE_COLOR = new Color(0, 0, 0, 200);
    private static final Color INCREASE_COLOR = new Color(200, 255, 100, 200);
    private static final Color DECREASE_COLOR = new Color(255, 100, 200, 200);
    private static final int REFRESH_RATE = 50;

    private final NeuralNetwork network;
    private final int width;
    private final int height;
    private double[][][] weights;
    private int[] layerSizes;
    private double[][] activations;
    private double rms;
    private double maxRms;
    private final String[] layerNames;
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
    private double averageWeightChange;
    private double[][][] weightChanges;
    private double lastKnownMovingAverage;
    private double currentMovingAverage;
    private double maxMovingAverage;
    private boolean isMovingAverageUpdated;

    private static final int COLOR_CACHE_SIZE = 100;
    private static final Color[] weightColorCache = new Color[COLOR_CACHE_SIZE];
    private static final Color[] activationColorCache = new Color[COLOR_CACHE_SIZE];
    private static final Color[] outputActivationColorCache = new Color[COLOR_CACHE_SIZE];

    static {
        for (int i = 0; i < COLOR_CACHE_SIZE; i++) {
            double normalizedValue = i / (double) (COLOR_CACHE_SIZE - 1);

            double weight = (normalizedValue * 2 - 1);
            int value = (int) (255 * Math.abs(weight));
            int alpha = (int) (20 + 20 * Math.abs(weight));
            weightColorCache[i] = new Color(value, value, value, alpha);

            int r = (int) (INACTIVE_NODE_COLOR.getRed() + (ACTIVE_NODE_COLOR.getRed() - INACTIVE_NODE_COLOR.getRed()) * normalizedValue);
            int g = (int) (INACTIVE_NODE_COLOR.getGreen() + (ACTIVE_NODE_COLOR.getGreen() - INACTIVE_NODE_COLOR.getGreen()) *
                    normalizedValue);
            int b = (int) (INACTIVE_NODE_COLOR.getBlue() + (ACTIVE_NODE_COLOR.getBlue() - INACTIVE_NODE_COLOR.getBlue()) * normalizedValue);
            int a = (int) (INACTIVE_NODE_COLOR.getAlpha() + (ACTIVE_NODE_COLOR.getAlpha() - INACTIVE_NODE_COLOR.getAlpha()) *
                    normalizedValue);
            activationColorCache[i] = new Color(r, g, b, a);

            r = (int) (INACTIVE_OUTPUT_NODE_COLOR.getRed() + (ACTIVE_OUTPUT_NODE_COLOR.getRed() - INACTIVE_OUTPUT_NODE_COLOR.getRed()) *
                    normalizedValue);
            g = (int) (INACTIVE_OUTPUT_NODE_COLOR.getGreen() + (ACTIVE_OUTPUT_NODE_COLOR.getGreen() - INACTIVE_OUTPUT_NODE_COLOR.getGreen())
                    * normalizedValue);
            b = (int) (INACTIVE_OUTPUT_NODE_COLOR.getBlue() + (ACTIVE_OUTPUT_NODE_COLOR.getBlue() - INACTIVE_OUTPUT_NODE_COLOR.getBlue())
                    * normalizedValue);
            a = (int) (INACTIVE_OUTPUT_NODE_COLOR.getAlpha() + (ACTIVE_OUTPUT_NODE_COLOR.getAlpha() - INACTIVE_OUTPUT_NODE_COLOR.getAlpha())
                    * normalizedValue);
            outputActivationColorCache[i] = new Color(r, g, b, a);
        }
    }

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
        this.averageWeightChange = Double.NEGATIVE_INFINITY;
        this.weightChanges = null;
        this.lastKnownMovingAverage = network.getLastMovingAverage();
        this.currentMovingAverage = this.lastKnownMovingAverage;
        this.isMovingAverageUpdated = false;
        this.maxMovingAverage = network.getMaxMovingAverage();
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

        network.updateStatistics();

        this.rms = network.getRMS();
        this.maxRms = network.getMaxRMS();
        this.layerMins = network.getLayerMins();
        this.layerMaxs = network.getLayerMaxs();
        this.layerMeans = network.getLayerMeans();
        this.averageWeightChange = network.getAverageWeightChange();

        double newMovingAverage = network.getMovingAverage();

        if (newMovingAverage != currentMovingAverage) {
            lastKnownMovingAverage = currentMovingAverage;
            currentMovingAverage = newMovingAverage;
            maxMovingAverage = network.getMaxMovingAverage();
            isMovingAverageUpdated = true;
        }

        this.weightChanges = network.getWeightChanges();
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

        int startX = networkStartX + layerIndex * layerDistance;
        int nextX = networkStartX + (layerIndex + 1) * layerDistance;

        boolean hasWeightChanges = weightChanges != null &&
                layerIndex < weightChanges.length &&
                weightChanges[layerIndex] != null;

        for (int j = 0; j < nodeCount; j++) {
            int y = networkStartY + calculateNodeY(j, nodeCount);
            int startY = y + nodeSize / 2;

            for (int k = 0; k < nextNodeCount; k++) {
                if (layerIndex < weights.length && k < weights[layerIndex].length && j < weights[layerIndex][k].length) {
                    double weight = weights[layerIndex][k][j];

                    int colorIndex = (int) ((Math.tanh(weight) + 1.2) * (COLOR_CACHE_SIZE - 1) / 2);
                    colorIndex = Math.min(COLOR_CACHE_SIZE - 1, Math.max(0, colorIndex));
                    Color lineColor = weightColorCache[colorIndex];

                    if (hasWeightChanges && k < weightChanges[layerIndex].length &&
                            j < weightChanges[layerIndex][k].length) {
                        double change = weightChanges[layerIndex][k][j];
                        if (Math.abs(change) > 0.001) {
                            double min = Math.min(1.0, Math.abs(change) * 25);
                            lineColor = blendColors(lineColor,
                                    change > 0 ? POSITIVE_CHANGE_COLOR : NEGATIVE_CHANGE_COLOR,
                                    min);
                        }
                    }

                    int nextY = networkStartY + calculateNodeY(k, nextNodeCount) + nodeSize / 2;
                    g2d.setColor(lineColor);
                    g2d.draw(new Line2D.Double(startX + nodeSize, startY, nextX, nextY));
                }
            }
        }
    }

    private Color blendColors(Color c1, Color c2, double ratio) {
        if (ratio < 0.01) {
            return c1;
        }
        if (ratio > 0.99) {
            return c2;
        }
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }

    private void drawLayerNodes(Graphics2D g2d, int layerIndex) {
        int nodeCount = layerSizes[layerIndex];
        boolean isOutputLayer = (layerIndex == layerSizes.length - 1);

        int maxOutputIndexFirst12 = -1;
        int maxOutputIndexLast3 = -1;
        if (isOutputLayer) {
            maxOutputIndexFirst12 = findMaxOutputIndex(activations[layerIndex], 0, 12);
            maxOutputIndexLast3 = findMaxOutputIndex(activations[layerIndex], 12, 15);
        }

        for (int j = 0; j < nodeCount; j++) {
            int x = networkStartX + layerIndex * layerDistance;
            int y = networkStartY + calculateNodeY(j, nodeCount);

            Color nodeColor;
            if (layerIndex < activations.length && j < activations[layerIndex].length) {
                double activation = activations[layerIndex][j];
                if (isOutputLayer && j == maxOutputIndexFirst12 || j == maxOutputIndexLast3) {
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

    private int findMaxOutputIndex(double[] outputActivations, int start, int end) {
        if (outputActivations == null || outputActivations.length == 0) {
            return -1;
        }
        if (start < 0 || end > outputActivations.length || start >= end) {
            return -1;
        }
        int maxIndex = start;
        for (int i = start + 1; i < end; i++) {
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

    private Color getColorForActivation(double activation, boolean isOutputLayer) {
        activation = Math.max(0, Math.min(1, activation));
        int colorIndex = (int) (activation * (COLOR_CACHE_SIZE - 1));
        return isOutputLayer ? outputActivationColorCache[colorIndex] : activationColorCache[colorIndex];
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(FONT_COLOR);
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        FontMetrics metrics = g2d.getFontMetrics();
        drawLeftColumn(g2d);
        drawRightColumn(g2d, metrics);
    }

    private void drawLeftColumn(Graphics2D g2d) {
        //String maxQ = String.format("Max RMS: %.8f", network.getMaxRMS());
        String nextQ = String.format("NextQ: %.4f", network.getNextQ());
        g2d.drawString(nextQ, STAT_X, height - 9 * Y_OFFSET);

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
        int rightColumnX = width - STAT_X - RIGHT_COLUMN_OFFSET;

        if (network.getMovingAverage() != 0) {
            double stabilityRatio = network.getRMS() / Math.abs(network.getMovingAverage());
            String stability = String.format("Stability Ratio: %.4f", stabilityRatio);
            g2d.drawString(stability, rightColumnX - metrics.stringWidth(stability), height - 180);
        }

        String episodes = String.format("Episodes: %d", network.getEpisodeCount());
        g2d.drawString(episodes, rightColumnX - metrics.stringWidth(episodes), height - 160);

        double currentMovingAverage = network.getMovingAverage();
        String movingAverage = String.format("Average reward: %.4f (%.4f)", network.getMovingAverage(), maxMovingAverage);
        if (isMovingAverageUpdated) {
            if (currentMovingAverage > lastKnownMovingAverage) {
                g2d.setColor(INCREASE_COLOR);
            } else if (currentMovingAverage < lastKnownMovingAverage) {
                g2d.setColor(DECREASE_COLOR);
            } else {
                g2d.setColor(FONT_COLOR);
            }
        } else {
            g2d.setColor(FONT_COLOR);
        }
        g2d.drawString(movingAverage, rightColumnX - metrics.stringWidth(movingAverage), height - 140);
        g2d.setColor(FONT_COLOR);

        //String bestReward = String.format("Best episode reward: %.0f", network.getBestReward());
        //g2d.drawString(bestReward, rightColumnX - metrics.stringWidth(bestReward), height - 120);

        //String reward = String.format("Step reward: %.4f",  network.getLastReward());
        //g2d.drawString(reward, rightColumnX - metrics.stringWidth(reward), height - 120);

        String lossAvg = String.format("Avg loss: %.4f",  network.getAverageDelta());
        g2d.drawString(lossAvg, rightColumnX - metrics.stringWidth(lossAvg), height - 120);

        String qlearning = String.format("Q learning rate: %.4f", network.getQlearningRate());
        g2d.drawString(qlearning, rightColumnX - metrics.stringWidth(qlearning), height - 100);

        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        g2d.drawString(learningRate, rightColumnX - metrics.stringWidth(learningRate), height - 80);

        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        g2d.drawString(epsilon, rightColumnX - metrics.stringWidth(epsilon), height - 60);

        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());
        g2d.drawString(discountFactor, rightColumnX - metrics.stringWidth(discountFactor), height - 40);

        String avgWeightChange = String.format("Avg Weight Change: %.8f", this.averageWeightChange);
        g2d.drawString(avgWeightChange, rightColumnX - metrics.stringWidth(avgWeightChange), height - 20);
    }
}
