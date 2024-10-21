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
    private static final Color POSITIVE_CHANGE_COLOR = new Color(255, 255, 255, 200);
    private static final Color NEGATIVE_CHANGE_COLOR = new Color(0, 0, 0, 200);
    private static final Color INCREASE_COLOR = new Color(200, 255, 100, 200);  // Zöld szín növekedéshez
    private static final Color DECREASE_COLOR = new Color(255, 100, 200, 200);  // Piros szín csökkenéshez
    private static final int REFRESH_RATE = 50;

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
    private double averageWeightChange;
    private double[][][] weightChanges;
    private double lastKnownMovingAverage;
    private double currentMovingAverage;
    private double maxMovingAverage;
    private boolean isMovingAverageUpdated;

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
        this.maxMovingAverage = Math.max(network.getMaxAverageReward(), this.lastKnownMovingAverage);
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
        this.layerMins = network.getLayerMins();
        this.layerMaxs = network.getLayerMaxs();
        this.layerMeans = network.getLayerMeans();
        this.averageWeightChange = network.getAverageWeightChange();

        double newMovingAverage = network.getMovingAverage();

        if (newMovingAverage != currentMovingAverage) {
            if (newMovingAverage > maxMovingAverage) {
                maxMovingAverage = newMovingAverage;
            }
            lastKnownMovingAverage = currentMovingAverage;
            currentMovingAverage = newMovingAverage;
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

        for (int j = 0; j < nodeCount; j++) {
            int x = networkStartX + layerIndex * layerDistance;
            int y = networkStartY + calculateNodeY(j, nodeCount);

            for (int k = 0; k < nextNodeCount; k++) {
                int nextX = networkStartX + (layerIndex + 1) * layerDistance;
                int nextY = networkStartY + calculateNodeY(k, nextNodeCount);
                if (layerIndex < weights.length && k < weights[layerIndex].length && j < weights[layerIndex][k].length) {
                    double weight = weights[layerIndex][k][j];
                    Color lineColor = getColorForWeight(weight);

                    if (weightChanges != null && layerIndex < weightChanges.length &&
                            k < weightChanges[layerIndex].length && j < weightChanges[layerIndex][k].length) {
                        double change = weightChanges[layerIndex][k][j];
                        double min = Math.min(1.0, Math.abs(change) * 25);
                        if (change > 0) {
                            lineColor = blendColors(getColorForWeight(weight), POSITIVE_CHANGE_COLOR, min);
                        } else if (change < 0) {
                            lineColor = blendColors(getColorForWeight(weight), NEGATIVE_CHANGE_COLOR, min);
                        }
                    }

                    g2d.setColor(lineColor);
                    g2d.draw(new Line2D.Double(x + nodeSize, y + (double) nodeSize / 2, nextX,
                            nextY + (double) nodeSize / 2));
                }
            }
        }
    }

    private Color blendColors(Color c1, Color c2, double ratio) {
        int r = (int) Math.min(255, Math.max(0, c1.getRed() * (1 - ratio) + c2.getRed() * ratio));
        int g = (int) Math.min(255, Math.max(0, c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio));
        int b = (int) Math.min(255, Math.max(0, c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio));
        int a = (int) Math.min(255, Math.max(0, c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio));
        return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
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

    private Color getColorForWeight(double weight) {
        double normalizedWeight = Math.tanh(weight / 4);
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
        int alpha = (int) (20 + 20 * Math.abs(normalizedWeight));
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
        String maxQ = String.format("Max Q: %.8f", network.getMaxQValueX());
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
        double currentMovingAverage = network.getMovingAverage();
        String episodes = String.format("Episodes: %d", network.getEpisodeCount());
        String movingAverage = String.format("Average reward: %.4f (%.4f)", network.getMovingAverage(), maxMovingAverage);
        String bestReward = String.format("Best episode reward: %.0f", network.getBestReward());
        String reward = String.format("Step reward: %.4f",  network.getLastReward());
        String learningRate = String.format("Learning Rate: %.4f", network.getLearningRate());
        String epsilon = String.format("Epsilon: %.4f", network.getEpsilon());
        String discountFactor = String.format("Discount Factor: %.4f", network.getDiscountFactor());
        String avgWeightChange = String.format("Avg Weight Change: %.6f", this.averageWeightChange);

        int rightColumnX = width - STAT_X - RIGHT_COLUMN_OFFSET;

        g2d.drawString(episodes, rightColumnX - metrics.stringWidth(episodes), height - 160);

        if (isMovingAverageUpdated) {
            if (currentMovingAverage > lastKnownMovingAverage) {
                g2d.setColor(INCREASE_COLOR);
            } else if (currentMovingAverage < lastKnownMovingAverage) {
                g2d.setColor(DECREASE_COLOR);
            } else {
                g2d.setColor(FONT_COLOR);  // Ha nem változott, marad az eredeti szín
            }
        } else {
            g2d.setColor(FONT_COLOR);
        }
        g2d.drawString(movingAverage, rightColumnX - metrics.stringWidth(movingAverage), height - 140);
        g2d.setColor(FONT_COLOR);  // Visszaállítjuk az eredeti színt

        g2d.drawString(bestReward, rightColumnX - metrics.stringWidth(bestReward), height - 120);
        g2d.drawString(reward, rightColumnX - metrics.stringWidth(reward), height - 100);
        g2d.drawString(learningRate, rightColumnX - metrics.stringWidth(learningRate), height - 80);
        g2d.drawString(epsilon, rightColumnX - metrics.stringWidth(epsilon), height - 60);
        g2d.drawString(discountFactor, rightColumnX - metrics.stringWidth(discountFactor), height - 40);
        g2d.drawString(avgWeightChange, rightColumnX - metrics.stringWidth(avgWeightChange), height - 20);
    }
}
