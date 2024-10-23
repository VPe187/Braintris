package hu.nye.vpe.tetris;

import java.awt.*;
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
    private static final float GRAVITY = 1.0f; // növelve 0.5-ről 1.0-ra
    private static final float INITIAL_VELOCITY = 15f; // növelve 10-ről 15-re
    private static final int PARTICLE_COUNT = 8;
    private static final float ALPHA_DECREASE_RATE = 15f; // új konstans az átlátszóság gyorsabb csökkenéséhez
    private static final float ROTATION_SPEED = 15.0f; // új konstans a gyorsabb forgáshoz


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
        if (stackManager.getNextTetromino() != null) {
            renderNextTetrominoPanel(g2D);
        }
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

        // Játéktér háttere
        g2D.setColor(GameConstans.COLOR_STACK_BACKGROUND);
        g2D.setColor(new Color(
                GameConstans.COLOR_STACK_BACKGROUND.getRed(),
                GameConstans.COLOR_STACK_BACKGROUND.getGreen(),
                GameConstans.COLOR_STACK_BACKGROUND.getBlue(),
                190
        ));
        g2D.fillRect(GameConstans.STACK_X, GameConstans.STACK_Y, GameConstans.STACK_W, GameConstans.STACK_H);

        // Keret blokkok renderelése
        // Felső sor
        for (int k = 1; k <= GameConstans.COLS; k++) {
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y - GameConstans.BLOCK_SIZE, false);
        }

        // Alsó sor
        for (int k = 1; k <= GameConstans.COLS; k++) {
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y + ((GameConstans.ROWS - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE),
                    false);
        }

        // Oldalsó oszlopok
        for (int k = GameConstans.ROW_OFFSET; k <= GameConstans.ROWS + 1; k++) {
            int y = (GameConstans.STACK_Y - GameConstans.BLOCK_SIZE) + ((k - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE);
            // Bal oldali blokk
            renderBorderBlock(g2D, GameConstans.STACK_X - GameConstans.BLOCK_SIZE, y, true);
            // Jobb oldali blokk
            renderBorderBlock(g2D,
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + ((GameConstans.COLS + 1) * GameConstans.BLOCK_SIZE),
                    y, true);
        }
    }

    private void renderBorderBlock(Graphics2D g2D, int x, int y, boolean isVertical) {
        int size = GameConstans.BLOCK_SIZE;
        int quarterSize = size / 2;
        int gap = 2;

        // Halványabb színek a kerethez
        Color baseColor = new Color(70, 70, 70, 180);      // Alap szürke, enyhén átlátszó
        Color lightColor = new Color(90, 90, 90, 160);     // Világosabb, még átlátszóbb
        Color highlightColor = new Color(110, 110, 110, 140); // Legvilágosabb, legjobban átlátszó
        Color shadowColor = new Color(50, 50, 50, 200);    // Sötétebb, kevésbé átlátszó

        // Háttér az egész blokknak
        g2D.setColor(shadowColor);
        g2D.fillRect(x, y, size, size);

        // A négy negyed pozíciói
        int[][] quarters = {
                {x + gap, y + gap},                    // bal felső
                {x + quarterSize, y + gap},            // jobb felső
                {x + gap, y + quarterSize},            // bal alsó
                {x + quarterSize, y + quarterSize}     // jobb alsó
        };

        // Minden negyed renderelése finomabb megjelenéssel
        for (int k = 0; k < 4; k++) {
            int qx = quarters[k][0];
            int qy = quarters[k][1];

            // Alap forma, kevésbé kiemelt 3D hatással
            g2D.setColor(baseColor);
            g2D.fill3DRect(qx, qy, quarterSize - gap, quarterSize - gap, true);

            // Visszafogottabb átlós minta
            if (isVertical) {
                // Függőleges átlós vonal
                g2D.setColor(lightColor);
                g2D.drawLine(
                        qx + quarterSize - gap - 8,
                        qy + 4,
                        qx + 4,
                        qy + quarterSize - gap - 8
                );
            } else {
                // Vízszintes átlós vonal
                g2D.setColor(lightColor);
                g2D.drawLine(
                        qx + 4,
                        qy + 4,
                        qx + quarterSize - gap - 8,
                        qy + quarterSize - gap - 8
                );
            }

            // Minimális sarokpont
            g2D.setColor(highlightColor);
            g2D.fillOval(qx + 3, qy + 3, 2, 2);
        }

        // Kisebb középső pont
        int centerSize = 3;
        g2D.setColor(highlightColor);
        g2D.fillOval(
                x + size/2 - centerSize/2,
                y + size/2 - centerSize/2,
                centerSize,
                centerSize
        );
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
                            // Törölt blokkok
                            if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.ERASED.getTetrominoTypeId()) {
                                if (stackManager.getGameState() == GameState.DELETINGROWS) {
                                    if (!animationInitialized) {
                                        initializeExplosion(i);
                                    }

                                    renderExplosion(g2D, i, j);

                                    if (tickAnim) {
                                        updateExplosionPhysics();
                                        tickAnim = false;

                                        // Ellenőrizzük, hogy minden részecske elhalványult-e
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
                                // Normál blokkok renderelése változatlan marad
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
                        (int)blockAlpha[index]
                );

                g2D.setColor(particleColor);

                // Mentjük az eredeti transzformációt
                AffineTransform originalTransform = g2D.getTransform();

                // Alkalmazzuk az új transzformációt
                g2D.translate(
                        baseX + blockOffsetX[index] + GameConstans.BLOCK_SIZE / 2,
                        baseY + blockOffsetY[index] + GameConstans.BLOCK_SIZE / 2
                );
                g2D.rotate(Math.toRadians(blockRotation[index]));

                // Kisebb részecskéket rajzolunk
                int particleSize = GameConstans.BLOCK_SIZE / 3;
                g2D.fillRect(-particleSize/2, -particleSize/2, particleSize, particleSize);

                // Visszaállítjuk az eredeti transzformációt
                g2D.setTransform(originalTransform);
            }
        }
    }

    private void updateExplosionPhysics() {
        for (int i = 0; i < blockOffsetX.length; i++) {
            // Gyorsabb mozgás
            blockOffsetX[i] += blockVelocityX[i] * 1.5; // 50%-kal gyorsabb mozgás
            blockOffsetY[i] += blockVelocityY[i] * 1.5;

            // Erősebb gravitáció
            blockVelocityY[i] += GRAVITY;

            // Gyorsabb forgás
            blockRotation[i] += ROTATION_SPEED;

            // Gyorsabb halványodás
            blockAlpha[i] = Math.max(0, blockAlpha[i] - ALPHA_DECREASE_RATE);
        }
    }

    private void renderNormalBlock(Graphics2D g2D, int i, int j, int i1) {
        int x, y;
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
            renderPulsingBlock(g2D, x, y, stackManager.getStackArea()[i][j].getColor());
        } else {
            renderShinyBlock(g2D, x, y, stackManager.getStackArea()[i][j].getColor());
        }
    }

    private void renderPulsingBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;
        int quarterSize = size / 2;
        int gap = 2;

        // Különleges színek a pulzáló blokkhoz
        Color superBright = new Color(
                Math.min(255, baseColor.getRed() + 100),
                Math.min(255, baseColor.getGreen() + 100),
                Math.min(255, baseColor.getBlue() + 100)
        );
        Color bright = new Color(
                Math.min(255, baseColor.getRed() + 70),
                Math.min(255, baseColor.getGreen() + 70),
                Math.min(255, baseColor.getBlue() + 70)
        );
        Color glow = new Color(
                Math.min(255, baseColor.getRed() + 40),
                Math.min(255, baseColor.getGreen() + 40),
                Math.min(255, baseColor.getBlue() + 40)
        );

        // Alap háttér
        g2D.setColor(baseColor);
        g2D.fillRect(x, y, size, size);

        // A négy negyed pozíciói
        int[][] quarters = {
                {x + gap, y + gap},                    // bal felső
                {x + quarterSize, y + gap},            // jobb felső
                {x + gap, y + quarterSize},            // bal alsó
                {x + quarterSize, y + quarterSize}     // jobb alsó
        };

        // Középpont
        int centerX = x + size/2;
        int centerY = y + size/2;

        // Rajzolunk egy belső kereszt alakú mintát
        g2D.setColor(bright);
        int crossWidth = 8;
        g2D.fillRect(centerX - crossWidth/2, y + gap, crossWidth, size - 2*gap);  // függőleges
        g2D.fillRect(x + gap, centerY - crossWidth/2, size - 2*gap, crossWidth);  // vízszintes

        // Minden negyed renderelése külön
        for (int k = 0; k < 4; k++) {
            int qx = quarters[k][0];
            int qy = quarters[k][1];

            // A negyed alapja
            g2D.setColor(glow);
            g2D.fill3DRect(qx, qy, quarterSize - gap, quarterSize - gap, true);

            // Fényes sarok elemek
            g2D.setColor(superBright);
            int cornerSize = 8;

            // Külső sarok
            if (k == 0) { // bal felső
                g2D.fillArc(qx, qy, cornerSize*2, cornerSize*2, 90, 90);
            } else if (k == 1) { // jobb felső
                g2D.fillArc(qx + quarterSize - gap - cornerSize*2, qy, cornerSize*2, cornerSize*2, 0, 90);
            } else if (k == 2) { // bal alsó
                g2D.fillArc(qx, qy + quarterSize - gap - cornerSize*2, cornerSize*2, cornerSize*2, 180, 90);
            } else { // jobb alsó
                g2D.fillArc(qx + quarterSize - gap - cornerSize*2, qy + quarterSize - gap - cornerSize*2, cornerSize*2, cornerSize*2, 270, 90);
            }

            // Belső sarok a középpont felé
            int innerCornerX = k % 2 == 0 ? qx + quarterSize - gap - cornerSize : qx;
            int innerCornerY = k < 2 ? qy + quarterSize - gap - cornerSize : qy;
            g2D.fillArc(innerCornerX, innerCornerY, cornerSize, cornerSize,
                    k == 0 ? 315 : k == 1 ? 225 : k == 2 ? 45 : 135, 90);
        }

        // Középső fényes elem
        int centerSize = 12;
        g2D.setColor(superBright);
        g2D.fillOval(centerX - centerSize/2, centerY - centerSize/2, centerSize, centerSize);

        // Dekoratív fényes vonalak a középpontból a sarkokig
        g2D.setColor(new Color(superBright.getRed(), superBright.getGreen(), superBright.getBlue(), 150));
        for (int angle = 45; angle < 360; angle += 90) {
            double rad = Math.toRadians(angle);
            int rayLength = quarterSize - gap;
            g2D.drawLine(
                    centerX,
                    centerY,
                    centerX + (int)(Math.cos(rad) * rayLength),
                    centerY + (int)(Math.sin(rad) * rayLength)
            );
        }
    }

    private void renderShinyBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;
        int quarterSize = size / 2;
        int gap = 2;

        // Extra fényes színek a bónusz blokkhoz
        Color superLight = new Color(
                Math.min(255, baseColor.getRed() + 80),
                Math.min(255, baseColor.getGreen() + 80),
                Math.min(255, baseColor.getBlue() + 80)
        );
        Color lighterColor = new Color(
                Math.min(255, baseColor.getRed() + 50),
                Math.min(255, baseColor.getGreen() + 50),
                Math.min(255, baseColor.getBlue() + 50)
        );
        Color darkerColor = new Color(
                Math.max(0, baseColor.getRed() - 20),
                Math.max(0, baseColor.getGreen() - 20),
                Math.max(0, baseColor.getBlue() - 20)
        );

        // Háttér az egész blokknak
        g2D.setColor(baseColor);
        g2D.fillRect(x, y, size, size);

        // A négy negyed pozíciói és elforgatási szögei
        int[][] quarters = {
                {x + gap, y + gap},                    // bal felső
                {x + quarterSize, y + gap},            // jobb felső
                {x + gap, y + quarterSize},            // bal alsó
                {x + quarterSize, y + quarterSize}     // jobb alsó
        };

        // Minden negyed renderelése külön
        for (int k = 0; k < 4; k++) {
            int qx = quarters[k][0];
            int qy = quarters[k][1];
            int elevation = 3; // Nagyobb kiemelkedés mint a normál blokknál

            // Alap forma
            g2D.setColor(lighterColor);
            g2D.fill3DRect(qx, qy, quarterSize - gap, quarterSize - gap, true);

            // Fényes átlós csík minden negyedben
            g2D.setColor(superLight);
            int[] xPoints = {qx + 4, qx + quarterSize - gap - 4, qx + quarterSize - gap - 8, qx + 8};
            int[] yPoints = {qy + 4, qy + quarterSize - gap - 4, qy + quarterSize - gap - 8, qy + 8};
            g2D.fillPolygon(xPoints, yPoints, 4);

            // Extra fényes pont a sarokban
            g2D.fillOval(qx + 3, qy + 3, 5, 5);

            // Keret a negyednek
            g2D.setColor(darkerColor);
            g2D.drawRect(qx, qy, quarterSize - gap - 1, quarterSize - gap - 1);
        }

        // Középpontban egy fényes kereszteződés
        g2D.setColor(superLight);
        int centerSize = 6;
        g2D.fillOval(x + size/2 - centerSize/2, y + size/2 - centerSize/2, centerSize, centerSize);

        // Finom fénysugarak a középpontból
        g2D.setColor(new Color(superLight.getRed(), superLight.getGreen(), superLight.getBlue(), 150));
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int rayLength = 8;
            g2D.drawLine(
                    x + size/2,
                    y + size/2,
                    x + size/2 + (int)(Math.cos(rad) * rayLength),
                    y + size/2 + (int)(Math.sin(rad) * rayLength)
            );
        }
    }

    private void renderModernBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;
        int quarterSize = size / 2;
        int gap = 2; // Rés mérete a negyedek között

        // Színek kiszámítása
        Color lighterColor = new Color(
                Math.min(255, baseColor.getRed() + 30),
                Math.min(255, baseColor.getGreen() + 30),
                Math.min(255, baseColor.getBlue() + 30)
        );

        Color darkerColor = new Color(
                Math.max(0, baseColor.getRed() - 30),
                Math.max(0, baseColor.getGreen() - 30),
                Math.max(0, baseColor.getBlue() - 30)
        );

        // Háttér az egész blokknak
        g2D.setColor(darkerColor);
        g2D.fillRect(x, y, size, size);

        // A négy negyed pozíciói
        int[][] quarters = {
                {x + gap, y + gap},                    // bal felső
                {x + quarterSize, y + gap},            // jobb felső
                {x + gap, y + quarterSize},            // bal alsó
                {x + quarterSize, y + quarterSize}     // jobb alsó
        };

        // Minden negyed renderelése külön
        for (int k = 0; k < 4; k++) {
            int qx = quarters[k][0];
            int qy = quarters[k][1];
            int elevation = 2; // Kiemelkedés mértéke

            // Kiemelkedés oldala (sötétebb)
            g2D.setColor(darkerColor);
            int[] xPoints = {qx, qx + quarterSize - gap, qx + quarterSize - gap - elevation, qx - elevation};
            int[] yPoints = {qy + quarterSize - gap, qy + quarterSize - gap, qy + quarterSize - gap - elevation, qy + quarterSize - gap - elevation};
            g2D.fillPolygon(xPoints, yPoints, 4);

            // Kiemelkedés teteje (világosabb)
            g2D.setColor(baseColor);
            g2D.fill3DRect(qx, qy, quarterSize - gap, quarterSize - gap, true);

            // Fényes él a tetején (legvilágosabb)
            g2D.setColor(lighterColor);
            g2D.drawLine(qx, qy, qx + quarterSize - gap - 1, qy);
            g2D.drawLine(qx, qy, qx, qy + quarterSize - gap - 1);
        }
    }

    /**
     * Render helper.
     *
     * @param g2D Graphics2D
     */
    private void renderHelper(Graphics2D g2D) {
        if (stackManager.getCurrentTetromino() != null) {
            // Kiszámoljuk, hogy hol fog földet érni a tetromino
            int yoffset = stackManager.howFarFromDown(stackManager.getStackArea(), stackManager.getCurrentTetromino()) -
                    GameConstans.ROW_OFFSET + 1;

            // Először rajzoljuk meg a függőleges kiemelő sávokat
            for (int j = 0; j < stackManager.getCurrentTetromino().getPixels()[0].length; j++) {
                // Ellenőrizzük, hogy van-e blokk ebben az oszlopban
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
                                (yoffset + stackManager.getCurrentTetromino().getStackRow() + GameConstans.ROW_OFFSET)) * GameConstans.BLOCK_SIZE;
                    } else {
                        currentY = GameConstans.STACK_Y +
                                (stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;
                        targetY = GameConstans.STACK_Y +
                                (yoffset + stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;
                    }

                    // Rajzoljuk meg a függőleges kiemelő sávot
                    g2D.setColor(GameConstans.COLOR_HELPER_LINE);
                    g2D.fillRect(x, Math.min(currentY, targetY),
                            GameConstans.BLOCK_SIZE,
                            Math.abs(targetY - currentY) + GameConstans.BLOCK_SIZE);
                }
            }

            // Árnyék tetromino renderelése
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

                        // Árnyék blokk megjelenítése
                        Color shadowColor =GameConstans.COLOR_HELPER;
                        g2D.setColor(shadowColor);
                        g2D.fill3DRect(x, y, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                    }
                }
            }
        }
    }

    /**
     * Render bonus element.
     *
     * @param g2D Graphics2D
     * @param i cell coordinate
     * @param j cell coordinate
     */
    private void renderBonus(Graphics2D g2D, int i, int j) {
        int x = GameConstans.STACK_X + (j * GameConstans.BLOCK_SIZE);
        int y;
        if (upSideDown) {
            y = GameConstans.STACK_Y + (GameConstans.ROWS + 1 - i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;
        } else {
            y = GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE;
        }

        if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.LOADED.getTetrominoTypeId()) {
            // Büntető blokk - töredezett megjelenés
            renderBrokenBlock(g2D, x, y);
        } else if (stackManager.getStackArea()[i][j].getBonus() == BonusType.BOMB) {
            // Bónusz blokk - kristályos effekt
            renderCrystalBlock(g2D, x, y, stackManager.getStackArea()[i][j].getColor());
        }
    }

    private void renderBrokenBlock(Graphics2D g2D, int x, int y) {
        int size = GameConstans.BLOCK_SIZE;

        // Sötétebb szürke a büntető blokkoknak
        Color darkGray = new Color(50, 50, 50);
        Color darkerGray = new Color(30, 30, 30);

        // A blokk négy részre van törve, mindegyik rész kicsit "elcsúszva"
        int[][] segments = {
                {0, 0, size/2, size/2},          // bal felső
                {size/2, 0, size/2, size/2},     // jobb felső
                {0, size/2, size/2, size/2},     // bal alsó
                {size/2, size/2, size/2, size/2} // jobb alsó
        };

        int[] offsets = {-2, 2, -1, 1}; // Az egyes szegmensek elcsúszásának mértéke

        // Rajzoljuk meg a négy szegmenst külön-külön, eltolva
        for (int k = 0; k < segments.length; k++) {
            int[] segment = segments[k];
            int offsetX = k % 2 == 0 ? offsets[k] : 0;
            int offsetY = k < 2 ? offsets[k] : 0;

            // Árnyék hatás
            g2D.setColor(darkerGray);
            g2D.fill3DRect(
                    x + segment[0] + offsetX + 1,
                    y + segment[1] + offsetY + 1,
                    segment[2],
                    segment[3],
                    false
            );

            // A szegmens maga
            g2D.setColor(darkGray);
            g2D.fill3DRect(
                    x + segment[0] + offsetX,
                    y + segment[1] + offsetY,
                    segment[2],
                    segment[3],
                    true
            );
        }

        // Repedések rajzolása
        g2D.setColor(darkerGray);
        g2D.setStroke(new BasicStroke(1));

        // Középső repedések
        g2D.drawLine(x + size/2 - 2, y, x + size/2 + 2, y + size);
        g2D.drawLine(x, y + size/2 - 1, x + size, y + size/2 + 1);

        // Néhány kisebb repedés
        g2D.drawLine(x + size/4, y + size/4, x + size/2 - 2, y + size/2);
        g2D.drawLine(x + 3*size/4, y + size/4, x + size/2 + 2, y + size/2);
    }

    private void renderCrystalBlock(Graphics2D g2D, int x, int y, Color baseColor) {
        int size = GameConstans.BLOCK_SIZE;

        // Világosabb színek a kristályos hatáshoz
        Color brightColor = new Color(
                Math.min(255, baseColor.getRed() + 50),
                Math.min(255, baseColor.getGreen() + 50),
                Math.min(255, baseColor.getBlue() + 50)
        );
        Color brighterColor = new Color(
                Math.min(255, baseColor.getRed() + 100),
                Math.min(255, baseColor.getGreen() + 100),
                Math.min(255, baseColor.getBlue() + 100)
        );

        // Hatszögletű szegmensek pozíciói
        int[][] segments = {
                // Középső hatszög pontjai
                {size/2, size/4},      // felső
                {3*size/4, size/3},    // jobb felső
                {3*size/4, 2*size/3},  // jobb alsó
                {size/2, 3*size/4},    // alsó
                {size/4, 2*size/3},    // bal alsó
                {size/4, size/3}       // bal felső
        };

        // Árnyékolás a 3D hatásért
        g2D.setColor(baseColor);
        g2D.fill3DRect(x, y, size, size, true);

        // Külső hatszög
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        for (int i = 0; i < 6; i++) {
            xPoints[i] = x + segments[i][0];
            yPoints[i] = y + segments[i][1];
        }

        // Kristályos hatás renderelése
        g2D.setColor(brightColor);
        g2D.fillPolygon(xPoints, yPoints, 6);

        // Belső fényes részek
        g2D.setColor(brighterColor);
        // Felső háromszög
        g2D.fillPolygon(
                new int[]{x + size/2, x + 3*size/5, x + 2*size/5},
                new int[]{y + size/3, y + size/2, y + size/2},
                3
        );
        // Alsó háromszög
        g2D.fillPolygon(
                new int[]{x + size/2, x + 3*size/5, x + 2*size/5},
                new int[]{y + 2*size/3, y + size/2, y + size/2},
                3
        );

        // Finom körvonalak a kristály éleihez
        g2D.setColor(baseColor);
        g2D.setStroke(new BasicStroke(1));
        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            int x1 = x + segments[i][0];
            int y1 = y + segments[i][1];
            int x2 = x + segments[next][0];
            int y2 = y + segments[next][1];
            g2D.drawLine(x1, y1, x2, y2);
        }

        // Néhány belső vonal a kristályos hatásért
        g2D.setColor(brighterColor);
        g2D.drawLine(x + size/2, y + size/4, x + size/2, y + 3*size/4);
        g2D.drawLine(x + size/4, y + size/2, x + 3*size/4, y + size/2);
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
        int nbW = stackManager.getNextTetromino().getPixels()[0].length * GameConstans.BLOCK_SIZE;
        int nbH = stackManager.getNextTetromino().getPixels().length * GameConstans.BLOCK_SIZE;
        int nbX = nextPanel.getPanelX() + (nextPanel.getPanelWidth() / 2 - nbW / 2);
        int nbY = nextPanel.getPanelY() + GameConstans.BLOCK_SIZE + (nextPanel.getPanelHeight() / 2 - nbH / 2);
        if (stackManager.getGameState() != GameState.PAUSED) {
            for (int i = 0; i < stackManager.getNextTetromino().getPixels().length; i++) {
                for (int j = 0; j < stackManager.getNextTetromino().getPixels()[i].length; j++) {
                    if (stackManager.getNextTetromino().getPixels()[i][j] != 0) {
                        g2D.setColor(stackManager.getNextTetromino().getColor());
                        g2D.fill3DRect(nbX + j * GameConstans.BLOCK_SIZE, nbY + i * GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE,
                                GameConstans.BLOCK_SIZE, true);
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
        g2D.setColor(new Color(55 + (200 / GameConstans.PENALTY_NO_FULL_ROW) * stackManager.getNoFullRows(), 0, 0, 100));
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
            infoStrH = "Surrounding holes: " + String.format("%.0f", stackMetrics.getMetricSurroundingHoles());
            infoStrO = "Tetromino rotation: " + String.format("%.0f", stackManager.getTetrominoRotation());
            infoStrD = "Dropped elements: " + String.format("%.0f", stackMetrics.getMetricDroppedElements());
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