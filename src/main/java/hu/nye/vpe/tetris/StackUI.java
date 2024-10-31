package hu.nye.vpe.tetris;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Random;

import hu.nye.vpe.gaming.GameConstans;
import hu.nye.vpe.gaming.GameElement;
import hu.nye.vpe.gaming.GamePanel;
import hu.nye.vpe.gaming.GameState;

/**
 * Stack class.
 */
public class StackUI implements GameElement, StackComponent {
    private StackManager stackManager;
    private StackMetrics stackMetrics;
    private boolean upSideDown;
    private boolean tickAnim;
    private int levelTextAlpha = 200;
    private final boolean learning;

    private GamePanel nextPanel;
    private GamePanel penaltyPanel;
    private GamePanel scorePanel;
    private GamePanel levelPanel;
    private GamePanel infoPanel;
    private GamePanel statPanel;

    private float[] blockOffsetX;
    private float[] blockOffsetY;
    private float[] blockVelocityX;
    private float[] blockVelocityY;
    private float[] blockRotation;
    private float[] blockAlpha;
    private boolean animationInitialized;
    private static final float GRAVITY = 1.0f;
    private static final float INITIAL_VELOCITY = 15f;
    private static final int PARTICLE_COUNT = 8;
    private static final float ALPHA_DECREASE_RATE = 15f;
    private static final float ROTATION_SPEED = 15.0f;


    public StackUI(boolean learning) {
        this.learning = learning;
        initGameElement();
        initExplosionArrays();
    }

    private void initExplosionArrays() {
        blockOffsetX = new float[GameConstans.COLS * PARTICLE_COUNT];
        blockOffsetY = new float[GameConstans.COLS * PARTICLE_COUNT];
        blockVelocityX = new float[GameConstans.COLS * PARTICLE_COUNT];
        blockVelocityY = new float[GameConstans.COLS * PARTICLE_COUNT];
        blockRotation = new float[GameConstans.COLS * PARTICLE_COUNT];
        blockAlpha = new float[GameConstans.COLS * PARTICLE_COUNT];
    }

    private void initializeExplosion(int row) {
        Random random = new Random();
        for (int j = 0; j < GameConstans.COLS; j++) {
            for (int p = 0; p < PARTICLE_COUNT; p++) {
                int index = j * PARTICLE_COUNT + p;
                blockOffsetX[index] = 0;
                blockOffsetY[index] = 0;
                double angle = random.nextDouble() * 2 * Math.PI;
                float speed = random.nextFloat() * INITIAL_VELOCITY;
                blockVelocityX[index] = (float) (Math.cos(angle) * speed);
                blockVelocityY[index] = (float) (Math.sin(angle) * speed);
                blockRotation[index] = random.nextFloat() * 360;
                blockAlpha[index] = 255;
            }
        }
        animationInitialized = true;
    }

    private void initGameElement() {
        initNextPanel();
        initPenaltyPanel();
        initScorePanel();
        initLevelPanel();
        initInfoPanel();
        initStatPanel();
    }

    private void initNextPanel() {
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelHeight = 4 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        int panelX = GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE;
        nextPanel = new GamePanel(panelX, GameConstans.BLOCK_SIZE, panelWidth, panelHeight, panelBorderWidth, GameConstans.PANEL_COLOR,
                GameConstans.BLOCK_SIZE, "Next tetromino", GameConstans.FONT_NAME);
    }

    private void initPenaltyPanel() {
        int panelX = GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE;
        int penaltyPanelOffsetY = GameConstans.BLOCK_SIZE * 6;
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        penaltyPanel = new GamePanel(panelX, GameConstans.BLOCK_SIZE + penaltyPanelOffsetY, panelWidth,
                GameConstans.BLOCK_SIZE, panelBorderWidth, GameConstans.PANEL_COLOR, GameConstans.BLOCK_SIZE,
                "Penalty row", GameConstans.FONT_NAME);
    }

    private void initScorePanel() {
        int scorePanelOffsetY = GameConstans.BLOCK_SIZE * 9;
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelHeight = 2 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        scorePanel = new GamePanel(GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE,
                GameConstans.BLOCK_SIZE + scorePanelOffsetY, panelWidth, panelHeight,
                panelBorderWidth, GameConstans.PANEL_COLOR, GameConstans.BLOCK_SIZE, "Score",
                GameConstans.FONT_NAME);
    }

    private void initLevelPanel() {
        int panelX = GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE;
        int levelPanelOffsetY = GameConstans.BLOCK_SIZE * 13;
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        levelPanel = new GamePanel(panelX, GameConstans.BLOCK_SIZE + levelPanelOffsetY, panelWidth, GameConstans.BLOCK_SIZE,
                panelBorderWidth, GameConstans.PANEL_COLOR, GameConstans.BLOCK_SIZE, "Level", GameConstans.FONT_NAME);
    }

    private void initInfoPanel() {
        int panelX = GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE;
        int infoPanelOffsetY = GameConstans.BLOCK_SIZE * 16;
        int infoPanelHeight = ((learning) ? 7 : 3) * GameConstans.BLOCK_SIZE;
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        Color panelColor = new Color(30, 30, 30, 100);
        infoPanel = new GamePanel(panelX, GameConstans.BLOCK_SIZE + infoPanelOffsetY, panelWidth, infoPanelHeight,
                panelBorderWidth, panelColor, GameConstans.BLOCK_SIZE, "Info", GameConstans.FONT_NAME);
    }

    private void initStatPanel() {
        int panelX = GameConstans.STACK_W + 4 * GameConstans.BLOCK_SIZE;
        int statPanelOffsetY = GameConstans.BLOCK_SIZE * 21;
        int statPanelHeight = 2 * GameConstans.BLOCK_SIZE;
        int panelWidth = 6 * GameConstans.BLOCK_SIZE;
        int panelBorderWidth = 5;
        statPanel = new GamePanel(panelX, GameConstans.BLOCK_SIZE + statPanelOffsetY, panelWidth, statPanelHeight,
                panelBorderWidth, GameConstans.PANEL_COLOR, GameConstans.BLOCK_SIZE, "Stat", GameConstans.FONT_NAME);
    }


    @Override
    public void update() {
    }

    /**
     * Render game elements.
     *
     * @param g2D Graphics2D
     */
    @Override
    public void render(Graphics2D g2D) {
        renderPlayArea(g2D);
        renderStack(g2D);
        renderNextTetrominoPanel(g2D);
        renderScorePanel(g2D);
        renderPenaltyPanel(g2D);
        renderLevelPanel(g2D);
        renderInfoPanel(g2D);
        if (!learning) {
            renderStatisticPanel(g2D);
        }

        if (stackManager.getGameState() == GameState.CHANGINGLEVEL) {
            renderLevelText(g2D, "L E V E L  " + stackManager.getGameLevel() + ".");
        }
        if (stackManager.getGameState() == GameState.GAMEOVER) {
            renderText(g2D, "G A M E  O V E R");
        }
        if (stackManager.getGameState() == GameState.PAUSED) {
            renderText(g2D, "P A U S E D");
        }
    }

    /**
     * Render play area.
     *
     * @param g2D Graphics2D
     */
    private void renderPlayArea(Graphics2D g2D) {
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2D.setColor(GameConstans.COLOR_STACK_BACKGROUND);
        g2D.setColor(new Color(
                GameConstans.COLOR_STACK_BACKGROUND.getRed(),
                GameConstans.COLOR_STACK_BACKGROUND.getGreen(),
                GameConstans.COLOR_STACK_BACKGROUND.getBlue(),
                190
        ));
        g2D.fillRect(GameConstans.STACK_X, GameConstans.STACK_Y, GameConstans.STACK_W, GameConstans.STACK_H);

        for (int k = 1; k <= GameConstans.COLS; k++) {
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y - GameConstans.BLOCK_SIZE, false);
        }

        for (int k = 1; k <= GameConstans.COLS; k++) {
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y + ((GameConstans.ROWS - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE),
                    false);
        }

        for (int k = GameConstans.ROW_OFFSET; k <= GameConstans.ROWS + 1; k++) {
            int y = (GameConstans.STACK_Y - GameConstans.BLOCK_SIZE) + ((k - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE);
            renderBorderBlock(g2D, GameConstans.STACK_X - GameConstans.BLOCK_SIZE, y, true);
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + ((GameConstans.COLS + 1) * GameConstans.BLOCK_SIZE),
                    y, true);
        }
    }

    private void renderBorderBlock(Graphics2D g2D, int x, int y, boolean isVertical) {
        int size = GameConstans.BLOCK_SIZE;
        int margin = 1;
        Color baseColor = new Color(70, 70, 70, 180);
        Color lighterColor = new Color(85, 85, 85, 180);
        Color darkerColor = new Color(55, 55, 55, 180);
        g2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2D.setColor(baseColor);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        GradientPaint topGradient = new GradientPaint(
                x + size / 2f, y,
                new Color(100, 100, 100, 40),
                x + size / 2f, y + size / 2f,
                new Color(70, 70, 70, 0)
        );
        g2D.setPaint(topGradient);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size / 2);
        GradientPaint rightShade = new GradientPaint(
                x + size * 0.7f, y,
                new Color(0, 0, 0, 0),
                x + size - margin, y,
                new Color(0, 0, 0, 30)
        );
        g2D.setPaint(rightShade);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        g2D.setStroke(new BasicStroke(1.0f));
        g2D.setColor(lighterColor);
        g2D.drawLine(x + margin, y + margin, x + size - margin, y + margin);
        g2D.drawLine(x + margin, y + margin, x + margin, y + size - margin);
        g2D.setColor(darkerColor);
        g2D.drawLine(x + size - margin, y + margin, x + size - margin, y + size - margin);
        g2D.drawLine(x + margin, y + size - margin, x + size - margin, y + size - margin);
        int highlightSize = 2;
        g2D.setColor(new Color(100, 100, 100, 80));
        g2D.fillOval(x + margin + 3,  y + margin + 3, highlightSize, highlightSize);
    }

    /**
     * Render stack.
     *
     * @param g2D Graphics2D
     */
    private void renderStack(Graphics2D g2D) {
        if (stackManager.getGameState() == GameState.RUNNING ||
                stackManager.getGameState() == GameState.DELETINGROWS ||
                stackManager.getGameState() == GameState.GAMEOVER ||
                stackManager.getGameState() == GameState.PAUSED) {

            for (int i = 0; i < GameConstans.ROWS; i++) {
                for (int j = 0; j < GameConstans.COLS; j++) {
                    if (i >= GameConstans.ROW_OFFSET) {
                        int i1 = GameConstans.STACK_Y + ((GameConstans.ROWS + 1 - i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE);
                        if (stackManager.getStackArea()[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                            if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.ERASED.getTetrominoTypeId()) {
                                if (stackManager.getGameState() == GameState.DELETINGROWS) {
                                    if (!animationInitialized) {
                                        initializeExplosion(i);
                                    }

                                    renderExplosion(g2D, i, j);

                                    if (tickAnim) {
                                        updateExplosionPhysics();
                                        tickAnim = false;

                                        boolean allFaded = true;
                                        for (float alpha : blockAlpha) {
                                            if (alpha > 0) {
                                                allFaded = false;
                                                break;
                                            }
                                        }

                                        if (allFaded) {
                                            animationInitialized = false;
                                            stackManager.setGameState(GameState.RUNNING);
                                            stackManager.clearRows();
                                        }
                                    }
                                }
                            } else {
                                renderNormalBlock(g2D, i, j, i1);
                            }
                        }
                    }
                }
            }
            renderHelper(g2D);
        }
    }

    private void renderExplosion(Graphics2D g2D, int row, int col) {
        Color blockColor = stackManager.getStackArea()[row][col].getColor();
        int baseX = GameConstans.STACK_X + col * GameConstans.BLOCK_SIZE;
        int baseY = GameConstans.STACK_Y + (row - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;

        for (int p = 0; p < PARTICLE_COUNT; p++) {
            int index = col * PARTICLE_COUNT + p;
            if (blockAlpha[index] > 0) {
                Color particleColor = new Color(
                        blockColor.getRed(),
                        blockColor.getGreen(),
                        blockColor.getBlue(),
                        (int) blockAlpha[index]
                );

                g2D.setColor(particleColor);
                AffineTransform originalTransform = g2D.getTransform();
                g2D.translate(
                        baseX + blockOffsetX[index] + (double) GameConstans.BLOCK_SIZE / 2,
                        baseY + blockOffsetY[index] + (double) GameConstans.BLOCK_SIZE / 2
                );
                g2D.rotate(Math.toRadians(blockRotation[index]));
                int particleSize = GameConstans.BLOCK_SIZE / 3;
                g2D.fillRect(-particleSize / 2, -particleSize / 2, particleSize, particleSize);
                g2D.setTransform(originalTransform);
            }
        }
    }

    private void updateExplosionPhysics() {
        for (int i = 0; i < blockOffsetX.length; i++) {
            blockOffsetX[i] += (float) (blockVelocityX[i] * 1.5);
            blockOffsetY[i] += (float) (blockVelocityY[i] * 1.5);
            blockVelocityY[i] += GRAVITY;
            blockRotation[i] += ROTATION_SPEED;
            blockAlpha[i] = Math.max(0, blockAlpha[i] - ALPHA_DECREASE_RATE);
        }
    }

    private void renderNormalBlock(Graphics2D g2D, int i, int j, int i1) {
        int x;
        int y;
        if (upSideDown) {
            x = GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE;
            y = i1;
        } else {
            x = GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE;
            y = GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;
        }

        if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.LOADED.getTetrominoTypeId()) {
            renderBrokenBlock(g2D, x, y);
        } else if (stackManager.getStackArea()[i][j].getBonus() != BonusType.NONE) {
            renderBonusBlock(g2D, x, y, stackManager.getStackArea()[i][j].getColor());
        } else {
            renderCommonBlock(g2D, x, y, stackManager.getStackArea()[i][j].getColor());
        }
    }

    private void renderBonusBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;
        int margin = 1;
        Color darkerColor = new Color(
                Math.max(0, baseColor.getRed() - 40),
                Math.max(0, baseColor.getGreen() - 40),
                Math.max(0, baseColor.getBlue() - 40)
        );
        Color lighterColor = new Color(
                Math.min(255, baseColor.getRed() + 40),
                Math.min(255, baseColor.getGreen() + 40),
                Math.min(255, baseColor.getBlue() + 40)
        );
        g2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2D.setColor(baseColor);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        GradientPaint bottomGradient = new GradientPaint(
                x + size / 2f, y + size,
                new Color(255, 255, 255, 100),
                x + size / 2f, y + size / 2f,
                new Color(255, 255, 255, 0)
        );
        g2D.setPaint(bottomGradient);
        g2D.fillRect(x + margin, y + size / 2, size - 2 * margin, size / 2 - margin);
        GradientPaint leftShade = new GradientPaint(
                x + size * 0.3f, y,
                new Color(0, 0, 0, 0),
                x + margin, y,
                new Color(0, 0, 0, 40)
        );
        g2D.setPaint(leftShade);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        g2D.setStroke(new BasicStroke(1.5f));
        g2D.setColor(darkerColor);
        g2D.drawLine(x + margin, y + margin, x + size - margin, y + margin);
        g2D.drawLine(x + margin, y + margin, x + margin, y + size - margin);
        g2D.setColor(lighterColor);
        g2D.drawLine(x + size - margin, y + margin, x + size - margin, y + size - margin);
        g2D.drawLine(x + margin, y + size - margin, x + size - margin, y + size - margin);
        int highlightSize = 3;
        g2D.setColor(new Color(255, 255, 255, 120));
        g2D.fillOval(
                x + size - margin - highlightSize - 3,
                y + size - margin - highlightSize - 3,
                highlightSize,
                highlightSize
        );
    }

    private void renderCommonBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;
        int margin = 1;
        Color lighterColor = new Color(
                Math.min(255, baseColor.getRed() + 40),
                Math.min(255, baseColor.getGreen() + 40),
                Math.min(255, baseColor.getBlue() + 40)
        );
        Color darkerColor = new Color(
                Math.max(0, baseColor.getRed() - 30),
                Math.max(0, baseColor.getGreen() - 30),
                Math.max(0, baseColor.getBlue() - 30)
        );
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setColor(baseColor);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        GradientPaint topGradient = new GradientPaint(
                x + size / 2f, y,
                new Color(255, 255, 255, 100),
                x + size / 2f, y + size / 2f,
                new Color(255, 255, 255, 0)
        );
        g2D.setPaint(topGradient);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size / 2);
        GradientPaint rightShade = new GradientPaint(
                x + size * 0.7f, y,
                new Color(0, 0, 0, 0),
                x + size - margin, y,
                new Color(0, 0, 0, 40)
        );
        g2D.setPaint(rightShade);
        g2D.fillRect(x + margin, y + margin, size - 2 * margin, size - 2 * margin);
        g2D.setStroke(new BasicStroke(1.5f));
        g2D.setColor(lighterColor);
        g2D.drawLine(x + margin, y + margin, x + size - margin, y + margin);
        g2D.drawLine(x + margin, y + margin, x + margin, y + size - margin);
        g2D.setColor(darkerColor);
        g2D.drawLine(x + size - margin, y + margin, x + size - margin, y + size - margin);
        g2D.drawLine(x + margin, y + size - margin, x + size - margin, y + size - margin);
        int highlightSize = 3;
        g2D.setColor(new Color(255, 255, 255, 120));
        g2D.fillOval(
                x + margin + 3,
                y + margin + 3,
                highlightSize,
                highlightSize
        );
    }

    /**
     * Render helper.
     *
     * @param g2D Graphics2D
     */
    private void renderHelper(Graphics2D g2D) {
        if (stackManager.getCurrentTetromino() != null) {
            int yoffset = stackManager.howFarFromDown(stackManager.getStackArea(), stackManager.getCurrentTetromino()) -
                    GameConstans.ROW_OFFSET + 1;
            for (int j = 0; j < stackManager.getCurrentTetromino().getPixels()[0].length; j++) {
                boolean hasBlockInColumn = false;
                for (int i = 0; i < stackManager.getCurrentTetromino().getPixels().length; i++) {
                    if (stackManager.getCurrentTetromino().getPixels()[i][j] != 0) {
                        hasBlockInColumn = true;
                        break;
                    }
                }

                if (hasBlockInColumn) {
                    int x = GameConstans.STACK_X + (j + stackManager.getCurrentTetromino().getStackCol()) * GameConstans.BLOCK_SIZE;
                    int currentY;
                    int targetY;

                    if (upSideDown) {
                        currentY = GameConstans.STACK_Y + ((GameConstans.ROWS + 1) -
                                (stackManager.getCurrentTetromino().getStackRow() + GameConstans.ROW_OFFSET)) * GameConstans.BLOCK_SIZE;
                        targetY = GameConstans.STACK_Y + ((GameConstans.ROWS + 1) -
                                (yoffset + stackManager.getCurrentTetromino().getStackRow() + GameConstans.ROW_OFFSET)) *
                                GameConstans.BLOCK_SIZE;
                    } else {
                        currentY = GameConstans.STACK_Y +
                                (stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET) *
                                        GameConstans.BLOCK_SIZE;
                        targetY = GameConstans.STACK_Y +
                                (yoffset + stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET) *
                                        GameConstans.BLOCK_SIZE;
                    }

                    g2D.setColor(GameConstans.COLOR_HELPER_LINE);
                    g2D.fillRect(x, Math.min(currentY, targetY),
                            GameConstans.BLOCK_SIZE,
                            Math.abs(targetY - currentY) + GameConstans.BLOCK_SIZE);
                }
            }

            for (int i = 0; i < stackManager.getCurrentTetromino().getPixels().length; i++) {
                for (int j = 0; j < stackManager.getCurrentTetromino().getPixels()[i].length; j++) {
                    if (stackManager.getCurrentTetromino().getPixels()[i][j] != 0) {
                        int x = GameConstans.STACK_X + (j + stackManager.getCurrentTetromino().getStackCol()) * GameConstans.BLOCK_SIZE;
                        int y;

                        if (upSideDown) {
                            y = GameConstans.STACK_Y + ((GameConstans.ROWS + 1) -
                                    (yoffset + (i + stackManager.getNextTetromino().getStackRow() + GameConstans.ROW_OFFSET))) *
                                    GameConstans.BLOCK_SIZE;
                        } else {
                            y = GameConstans.STACK_Y +
                                    (yoffset + (i + stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET)) *
                                            GameConstans.BLOCK_SIZE;
                        }

                        Color shadowColor = GameConstans.COLOR_HELPER;
                        g2D.setColor(shadowColor);
                        g2D.fill3DRect(x, y, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                    }
                }
            }
        }
    }

    private void renderBrokenBlock(Graphics2D g2D, int x, int y) {
        int size = GameConstans.BLOCK_SIZE;
        Color darkGray = new Color(50, 50, 50);
        Color darkerGray = new Color(30, 30, 30);
        int[][] segments = {
                {0, 0, size / 2, size / 2},
                {size / 2, 0, size / 2, size / 2},
                {0, size / 2, size / 2, size / 2},
                {size / 2, size / 2, size / 2, size / 2}
        };
        int[] offsets = {-2, 2, -1, 1};

        for (int k = 0; k < segments.length; k++) {
            int[] segment = segments[k];
            int offsetX = k % 2 == 0 ? offsets[k] : 0;
            int offsetY = k < 2 ? offsets[k] : 0;

            g2D.setColor(darkerGray);
            g2D.fill3DRect(
                    x + segment[0] + offsetX + 1,
                    y + segment[1] + offsetY + 1,
                    segment[2],
                    segment[3],
                    false
            );

            g2D.setColor(darkGray);
            g2D.fill3DRect(
                    x + segment[0] + offsetX,
                    y + segment[1] + offsetY,
                    segment[2],
                    segment[3],
                    true
            );
        }

        g2D.setColor(darkerGray);
        g2D.setStroke(new BasicStroke(1));

        g2D.drawLine(x + size / 2 - 2, y, x + size / 2 + 2, y + size);
        g2D.drawLine(x, y + size / 2 - 1, x + size, y + size / 2 + 1);

        g2D.drawLine(x + size / 4, y + size / 4, x + size / 2 - 2, y + size / 2);
        g2D.drawLine(x + 3 * size / 4, y + size / 4, x + size / 2 + 2, y + size / 2);
    }

    private void renderText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.BOLD, GameConstans.BLOCK_SIZE * 2));
        textWidth = g2D.getFontMetrics().stringWidth(text);
        g2D.setColor(new Color(250, 250, 250, 230));
        g2D.drawString(text, GameConstans.STACK_X + (GameConstans.STACK_W / 2 - textWidth / 2), GameConstans.STACK_Y +
                (GameConstans.STACK_H / 2 - textWidth / 2));
    }

    private void renderLevelText(Graphics2D g2D, String text) {
        int textWidth;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.BOLD, GameConstans.BLOCK_SIZE * 2));
        textWidth = g2D.getFontMetrics().stringWidth(text);
        g2D.setColor(new Color(250, 250, 250, levelTextAlpha));
        g2D.drawString(text, GameConstans.STACK_X + (GameConstans.STACK_W / 2 - textWidth / 2), GameConstans.STACK_Y +
                (GameConstans.STACK_H / 2 - textWidth / 2));
        if (levelTextAlpha > 0) {
            if (tickAnim) {
                levelTextAlpha -= 2;
                tickAnim = false;
            }
        } else {
            levelTextAlpha = 200;
            stackManager.setGameState(GameState.RUNNING);
        }
    }

    private void renderNextTetrominoPanel(Graphics2D g2D) {
        nextPanel.render(g2D);

        if (stackManager.getGameState() != GameState.PAUSED && stackManager.getNextTetromino() != null) {
            int nbW = stackManager.getNextTetromino().getPixels()[0].length * GameConstans.BLOCK_SIZE;
            int nbH = stackManager.getNextTetromino().getPixels().length * GameConstans.BLOCK_SIZE;
            int nbX = nextPanel.getPanelX() + (nextPanel.getPanelWidth() / 2 - nbW / 2);
            int nbY = nextPanel.getPanelY() + GameConstans.BLOCK_SIZE + (nextPanel.getPanelHeight() / 2 - nbH / 2);

            for (int i = 0; i < stackManager.getNextTetromino().getPixels().length; i++) {
                for (int j = 0; j < stackManager.getNextTetromino().getPixels()[i].length; j++) {
                    if (stackManager.getNextTetromino().getPixels()[i][j] != 0) {
                        int blockX = nbX + j * GameConstans.BLOCK_SIZE;
                        int blockY = nbY + i * GameConstans.BLOCK_SIZE;
                        renderCommonBlock(g2D, blockX, blockY, stackManager.getNextTetromino().getColor());
                    }
                }
            }
        }
    }

    private void renderPenaltyPanel(Graphics2D g2D) {
        penaltyPanel.render(g2D);
        float penaltyWidth = penaltyPanel.getPanelWidth() - penaltyPanel.getBorderWidth() * 2;
        penaltyWidth = Math.round((penaltyWidth / (GameConstans.PENALTY_NO_FULL_ROW - 1)) * stackManager.getNoFullRows());
        int penaltyHeight = penaltyPanel.getTitleHeight() - penaltyPanel.getBorderWidth() * 2;
        int ppX = penaltyPanel.getPanelX() + penaltyPanel.getBorderWidth();
        int ppY = penaltyPanel.getPanelY() + GameConstans.BLOCK_SIZE + penaltyPanel.getBorderWidth();
        int redValue = Math.min(255, 55 + (int) ((200.0 / GameConstans.PENALTY_NO_FULL_ROW) * stackManager.getNoFullRows()));
        g2D.setColor(new Color(redValue, 0, 0, 100));
        if (stackManager.getNoFullRows() > 0) {
            g2D.fillRect(ppX, ppY, (int) penaltyWidth, penaltyHeight);
        }
    }

    private void renderScorePanel(Graphics2D g2D) {
        scorePanel.render(g2D);
        String gamePointsStr = String.valueOf(stackManager.getGameScore());
        int stringHeight = GameConstans.BLOCK_SIZE - 2;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.PLAIN, stringHeight));
        int stringWidth = g2D.getFontMetrics().stringWidth(gamePointsStr);
        g2D.setColor(Color.LIGHT_GRAY);
        int scoreX = scorePanel.getPanelX() + (scorePanel.getPanelWidth() / 2 - stringWidth / 2);
        int scoreY = scorePanel.getPanelY() + GameConstans.BLOCK_SIZE + (scorePanel.getPanelHeight() / 2 - stringHeight / 2) - 2;
        g2D.drawString(gamePointsStr, scoreX, scoreY + stringHeight - 3);
    }

    private void renderLevelPanel(Graphics2D g2D) {
        if (stackManager.getGameLevel() > 0) {
            levelPanel.setTitle("Level " + stackManager.getGameLevel());
        }
        levelPanel.render(g2D);
        int levelHeight = levelPanel.getPanelHeight() - levelPanel.getBorderWidth() * 2;
        float nextLevelWidth = levelPanel.getPanelWidth() - levelPanel.getBorderWidth() * 2;
        nextLevelWidth = Math.round((nextLevelWidth / (stackManager.getGameLevel() * GameConstans.LEVEL_CHANGE_ROWS)) *
                (stackManager.getAllFullRows() + 1));
        int lpX = levelPanel.getPanelX() + levelPanel.getBorderWidth();
        int lpY = levelPanel.getPanelY() + GameConstans.BLOCK_SIZE + levelPanel.getBorderWidth();
        int levelStringHeight = GameConstans.BLOCK_SIZE - 12;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.PLAIN, levelStringHeight));
        g2D.setColor(new Color(0, 55 + (200 / (Math.max(stackManager.getGameLevel(), 1) *
                GameConstans.LEVEL_CHANGE_ROWS)) * stackManager.getAllFullRows(), 0, 100));
        if (stackManager.getAllFullRows() > 0) {
            g2D.fillRect(lpX, lpY, (int) nextLevelWidth, levelHeight);
        }
    }

    private void renderInfoPanel(Graphics2D g2D) {
        infoPanel.render(g2D);
        String infoStrM;
        String infoStrP;
        String infoStrC;
        String infoStrR;
        String infoStrA;
        String infoStrN;
        String infoStrB;
        String infoStrH;
        String infoStrO;
        String infoStrD;
        String infoStrV;
        if (learning) {
            stackMetrics.calculateGameMetrics(stackManager.getStackArea());
            infoStrM = "Height: " + stackMetrics.getMetricMaxHeight();
            infoStrP = "Holes: " + stackMetrics.getMetricNumberOfHoles();
            infoStrC = "Bumpiness: " + String.format("%.0f", stackMetrics.getMetricBumpiness());
            infoStrR = "Iteration: " + stackManager.getIteration();
            infoStrA = "Height (avg): " + String.format("%.2f", stackMetrics.getMetricAvgColumnHeight());
            infoStrN = "Nearly full rows: " + String.format("%.0f", stackMetrics.getMetricNearlyFullRows());
            infoStrB = "Blocked rows: " + String.format("%.0f", stackMetrics.getMetricBlockedRows());
            infoStrH = "Surrounding holes: " + String.format("%.0f", stackMetrics.getMetricSurroundedHoles());
            infoStrO = "Tetromino rotation: " + String.format("%.0f", stackManager.getTetrominoRotation());
            infoStrD = "Dropped elements: " + String.format("%d", stackManager.getDroppedElements());
            infoStrV = "Average density: " + String.format("%.2f", stackMetrics.getMetricAvgDensity());
        } else {
            infoStrM = "M: Music On/Off";
            infoStrP = "P: Pause/Resume";
            infoStrC = "Arrows: Move";
            infoStrR = "Space: Rotate";
            infoStrA = "";
            infoStrN = "";
            infoStrB = "";
            infoStrH = "";
            infoStrO = "";
            infoStrD = "";
            infoStrV = "";
        }
        int stringHeight = GameConstans.BLOCK_SIZE - 14;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int infoX;
        if (learning) {
            infoX = infoPanel.getPanelX() + GameConstans.BLOCK_SIZE - 14;
        } else {
            infoX = infoPanel.getPanelX() + GameConstans.BLOCK_SIZE;
        }
        int infoY = infoPanel.getPanelY() + GameConstans.BLOCK_SIZE + GameConstans.BLOCK_SIZE / 2;
        int rowOffset = (GameConstans.BLOCK_SIZE / 2) + 2;
        g2D.drawString(infoStrM, infoX, infoY + stringHeight - 5);
        g2D.drawString(infoStrP, infoX, infoY + rowOffset + stringHeight - 5);
        g2D.drawString(infoStrC, infoX, infoY + rowOffset * 2 + stringHeight - 5);
        g2D.drawString(infoStrR, infoX, infoY + rowOffset * 3 + stringHeight - 5);
        if (learning) {
            g2D.drawString(infoStrA, infoX, infoY + rowOffset * 4 + stringHeight - 5);
            g2D.drawString(infoStrN, infoX, infoY + rowOffset * 5 + stringHeight - 5);
            g2D.drawString(infoStrB, infoX, infoY + rowOffset * 6 + stringHeight - 5);
            g2D.drawString(infoStrH, infoX, infoY + rowOffset * 7 + stringHeight - 5);
            g2D.drawString(infoStrO, infoX, infoY + rowOffset * 8 + stringHeight - 5);
            g2D.drawString(infoStrD, infoX, infoY + rowOffset * 9 + stringHeight - 5);
            g2D.drawString(infoStrV, infoX, infoY + rowOffset * 10 + stringHeight - 5);
        }
    }

    private void renderStatisticPanel(Graphics2D g2D) {
        statPanel.render(g2D);
        String allRowsStr = "Rows: " + stackManager.getAllFullRows();
        String timeStr = "Time: " + stackManager.getElapsedTime();
        int stringHeight = GameConstans.BLOCK_SIZE - 14;
        g2D.setFont(new Font(GameConstans.FONT_NAME, Font.PLAIN, stringHeight));
        g2D.setColor(Color.GRAY);
        int scoreX = statPanel.getPanelX() + GameConstans.BLOCK_SIZE;
        int scoreY = statPanel.getPanelY() + GameConstans.BLOCK_SIZE + GameConstans.BLOCK_SIZE / 2;
        g2D.drawString(allRowsStr, scoreX, scoreY + stringHeight - 5);
        g2D.drawString(timeStr, scoreX, scoreY + GameConstans.BLOCK_SIZE / 2 + stringHeight - 5);
    }

    public void setTickAnim(boolean tick) {
        this.tickAnim = tick;
    }

    @Override
    public void initializeStackComponents(StackUI stackUI, StackManager manager, StackMetrics metrics) {
        this.stackManager = manager;
        this.stackMetrics = metrics;
    }
}