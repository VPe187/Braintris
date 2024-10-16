package hu.nye.vpe.tetris;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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
    private int clearBlockSize = GameConstans.BLOCK_SIZE;
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

    public StackUI(boolean learning) {
        this.learning = learning;
        initGameElement();
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
        g2D.setColor(GameConstans.COLOR_STACK_BACKGROUND);
        g2D.setColor(new Color(
                GameConstans.COLOR_STACK_BACKGROUND.getRed(),
                GameConstans.COLOR_STACK_BACKGROUND.getGreen(),
                GameConstans.COLOR_STACK_BACKGROUND.getBlue(),
                190
        ));
        g2D.fillRect(GameConstans.STACK_X, GameConstans.STACK_Y, GameConstans.STACK_W, GameConstans.STACK_H);
        g2D.setColor(GameConstans.COLOR_STACK_BORDER);
        for (int k = 1; k <= GameConstans.COLS; k++) {
            g2D.fill3DRect((GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y - GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
            g2D.fill3DRect((GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + (k * GameConstans.BLOCK_SIZE),
                    GameConstans.STACK_Y + ((GameConstans.ROWS - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE),
                    GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
        }
        for (int k = GameConstans.ROW_OFFSET; k <= GameConstans.ROWS + 1; k++) {
            int y = (GameConstans.STACK_Y - GameConstans.BLOCK_SIZE) + ((k - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE);
            g2D.fill3DRect(GameConstans.STACK_X - GameConstans.BLOCK_SIZE, y, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
            g2D.fill3DRect(
                    (GameConstans.STACK_X - GameConstans.BLOCK_SIZE) + ((GameConstans.COLS + 1) * GameConstans.BLOCK_SIZE),
                    y,
                    GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE,
                    true
            );
        }
    }

    /**
     * Render stack.
     *
     * @param g2D Graphics2D
     */
    private void renderStack(Graphics2D g2D) {
        if (stackManager.getGameState() == GameState.RUNNING || stackManager.getGameState() == GameState.DELETINGROWS ||
                stackManager.getGameState() == GameState.GAMEOVER || stackManager.getGameState() == GameState.PAUSED) { // PAUSE remove

            for (int i = 0; i < GameConstans.ROWS; i++) {
                for (int j = 0; j < GameConstans.COLS; j++) {
                    if (i >= GameConstans.ROW_OFFSET) {
                        int i1 = GameConstans.STACK_Y + ((GameConstans.ROWS + 1 - i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE);
                        if (stackManager.getStackArea()[i][j].getTetrominoId() != TetrominoType.EMPTY.getTetrominoTypeId()) {
                            // Deleted blocks
                            if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.ERASED.getTetrominoTypeId()) {
                                if (stackManager.getGameState() == GameState.DELETINGROWS) {
                                    for (int k = 0; k < GameConstans.COLS; k++) {
                                        g2D.setColor(new Color(stackManager.getStackArea()[i][j].getColor().getRed(),
                                            stackManager.getStackArea()[i][j].getColor().getGreen(),
                                            stackManager.getStackArea()[i][j].getColor().getBlue(), clearBlockSize / 2));
                                        if (upSideDown) {
                                            g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE, i1,
                                                GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                                        } else {
                                            g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE,
                                                GameConstans.STACK_Y + ((i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE),
                                                GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                                        }
                                        if (upSideDown) {
                                            g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE, i1,
                                                GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, false);
                                        } else {
                                            g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE,
                                                GameConstans.STACK_Y + ((i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE),
                                                GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, false);
                                        }
                                        g2D.setColor(stackManager.getStackArea()[i][j].getColor());
                                        if (upSideDown) {
                                            g2D.fill3DRect((GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE) +
                                                (GameConstans.BLOCK_SIZE - clearBlockSize), i1 + (GameConstans.BLOCK_SIZE - clearBlockSize),
                                                GameConstans.BLOCK_SIZE - ((GameConstans.BLOCK_SIZE - clearBlockSize) * 2),
                                                GameConstans.BLOCK_SIZE - ((GameConstans.BLOCK_SIZE - clearBlockSize) * 2), true);
                                        } else {
                                            g2D.fill3DRect((GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE) +
                                            (GameConstans.BLOCK_SIZE - clearBlockSize),
                                                (GameConstans.STACK_Y + ((i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE)) +
                                                (GameConstans.BLOCK_SIZE - clearBlockSize), GameConstans.BLOCK_SIZE -
                                                ((GameConstans.BLOCK_SIZE - clearBlockSize) * 2), GameConstans.BLOCK_SIZE -
                                                ((GameConstans.BLOCK_SIZE - clearBlockSize) * 2), true);
                                        }
                                        if (clearBlockSize >= 0) {
                                            if (tickAnim) {
                                                clearBlockSize--;
                                                tickAnim = false;
                                            }
                                        } else {
                                            clearBlockSize = GameConstans.BLOCK_SIZE;
                                            stackManager.setGameState(GameState.RUNNING);
                                            stackManager.clearRows();
                                        }
                                    }
                                }
                                // Inserted penalty blocks
                            } else if (stackManager.getStackArea()[i][j].getTetrominoId() == TetrominoType.LOADED.getTetrominoTypeId()) {
                                g2D.setColor(Color.DARK_GRAY);
                                if (upSideDown) {
                                    g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE, i1, GameConstans.BLOCK_SIZE,
                                            GameConstans.BLOCK_SIZE, true);
                                } else {
                                    g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE,
                                        GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE,
                                            GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                                }
                                // Ordinary blocks
                            } else {
                                g2D.setColor(stackManager.getStackArea()[i][j].getColor());
                                if (upSideDown) {
                                    g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE, i1,
                                            GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                                } else {
                                    g2D.fill3DRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE,
                                            GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE,
                                            GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                                }
                                if (stackManager.getStackArea()[i][j].getBonus() != BonusType.NONE) {
                                    renderBonus(g2D, i, j);
                                }
                            }
                        } else {
                            if (stackManager.getCurrentTetromino() != null && j >= stackManager.getCurrentTetromino().getStackCol() &&
                                j <= stackManager.getCurrentTetromino().getStackCol() + stackManager.getCurrentTetromino().getHeightPoints() - 1 &&
                                i >= stackManager.getCurrentTetromino().getStackRow()) {
                                g2D.setColor(new Color(stackManager.getCurrentTetromino().getColor().getRed(),
                                    stackManager.getCurrentTetromino().getColor().getGreen(),
                                    stackManager.getCurrentTetromino().getColor().getBlue(), 10));
                                g2D.setColor(GameConstans.COLOR_HELPER_LINE);
                                if (upSideDown) {
                                    g2D.fillRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE, i1,
                                        GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE);
                                } else {
                                    g2D.fillRect(GameConstans.STACK_X + j * GameConstans.BLOCK_SIZE,
                                        GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE,
                                            GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE);
                                }
                            }
                        }
                    }
                }
                renderHelper(g2D);
            }
        }
    }

    /**
     * Render helper.
     *
     * @param g2D Graphics2D
     */
    private void renderHelper(Graphics2D g2D) {
        if (stackManager.getCurrentTetromino() != null) {
            int yoffset = stackManager.howFarFromDown(stackManager.getStackArea(), stackManager.getCurrentTetromino()) - GameConstans.ROW_OFFSET + 1;
            for (int i = 0; i < stackManager.getCurrentTetromino().getPixels().length; i++) {
                for (int j = 0; j < stackManager.getCurrentTetromino().getPixels()[i].length; j++) {
                    if (stackManager.getCurrentTetromino().getPixels()[i][j] != 0) {
                        g2D.setColor(GameConstans.COLOR_HELPER);
                        if (upSideDown) {
                            g2D.fill3DRect(GameConstans.STACK_X + (j + stackManager.getCurrentTetromino().getStackCol()) *
                                            GameConstans.BLOCK_SIZE, GameConstans.STACK_Y + ((GameConstans.ROWS + 1) -
                                            (yoffset + (i + stackManager.getNextTetromino().getStackRow() + GameConstans.ROW_OFFSET))) *
                                            GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                        } else {
                            g2D.fill3DRect(GameConstans.STACK_X + (j + stackManager.getCurrentTetromino().getStackCol()) *
                                    GameConstans.BLOCK_SIZE, GameConstans.STACK_Y +
                                    (yoffset + (i + stackManager.getCurrentTetromino().getStackRow() - GameConstans.ROW_OFFSET)) *
                                    GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, GameConstans.BLOCK_SIZE, true);
                        }
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
        g2D.setColor(Color.BLACK);
        int x = GameConstans.STACK_X + (j * GameConstans.BLOCK_SIZE) + (GameConstans.BLOCK_SIZE / 4);
        if (upSideDown) {
            g2D.fillOval(x,
                    GameConstans.STACK_Y + (GameConstans.ROWS + 1 - i - GameConstans.ROW_OFFSET) *
                            GameConstans.BLOCK_SIZE + (GameConstans.BLOCK_SIZE / 4),
                    GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4) * 2, GameConstans.BLOCK_SIZE -
                            (GameConstans.BLOCK_SIZE / 4) * 2);
        } else {
            g2D.fillOval(x,
                    GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE +
                    (GameConstans.BLOCK_SIZE / 4), GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4) * 2,
                    GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4) * 2);
        }
        g2D.setColor(new Color(stackManager.getStackArea()[i][j].getColor().getRed(), stackManager.getStackArea()[i][j].getColor().getGreen(),
                stackManager.getStackArea()[i][j].getColor().getBlue(), GameConstans.BONUS_COLOR_ALPHA));
        int x1 = GameConstans.STACK_X + (j * GameConstans.BLOCK_SIZE) + (GameConstans.BLOCK_SIZE / 4) + 4;
        if (upSideDown) {
            g2D.fillOval(x1,
                    GameConstans.STACK_Y + (GameConstans.ROWS + 1 - i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE +
                    (GameConstans.BLOCK_SIZE / 4) + 4, GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4 + 4) * 2,
                    GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4 + 4) * 2);
        } else {
            g2D.fillOval(x1,
                    GameConstans.STACK_Y + (i - GameConstans.ROW_OFFSET) * GameConstans.BLOCK_SIZE + (GameConstans.BLOCK_SIZE / 4) + 4,
                    GameConstans.BLOCK_SIZE - (GameConstans.BLOCK_SIZE / 4 + 4) * 2, GameConstans.BLOCK_SIZE -
                            (GameConstans.BLOCK_SIZE / 4 + 4) * 2);
        }
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