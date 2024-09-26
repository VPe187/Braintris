package hu.nye.vpe;

import java.awt.Graphics2D;

import hu.nye.vpe.gaming.GameAudio;
import hu.nye.vpe.gaming.GameColorPalette;
import hu.nye.vpe.gaming.GameInput;
import hu.nye.vpe.gaming.GameStarfield;
import hu.nye.vpe.gaming.GameTimeTicker;
import hu.nye.vpe.nn.NeuralNetworkQLearning;

/**
 * Tetris class.
 */
public class Tetris {
    private static final int ROWS = 24;
    private static final int COLS = 12;
    //private static final int FEED_DATA_SIZE = ROWS * COLS + 32 + 37;
    private static final int FEED_DATA_SIZE = 29;
    private static final int HIDDEN_NODES1 = 256;
    private static final int HIDDEN_NODES2 = 128;
    private static final int OUTPUT_NODES = 4;
    private static final int SHAPE_SIZE = 4 * 4;
    private static final long DROP_SPEED = 10L;
    private final long speed = 1000L;
    private final long learningSpeed = 10L;
    private Shape nextShape = null;
    private static final ShapeFactory sf = ShapeFactory.getInstance();
    private final Stack stack;
    private GameTimeTicker tickDown;
    private final GameTimeTicker tickBackground;
    private final GameTimeTicker tickControl;
    private final GameTimeTicker tickAnim;
    private final GameStarfield starField;
    private static final GameAudio gameAudio = new GameAudio();
    private final GameInput gameInput;
    private boolean musicOn = true;
    private final boolean learning;
    private NeuralNetworkQLearning brain;
    private double[] lastState;
    private int lastAction;

    public Tetris(int width, int height, GameInput gameInput, boolean learning) {
        stack = new Stack(learning);
        tickBackground = new GameTimeTicker(80);
        tickControl = new GameTimeTicker(10);
        tickAnim = new GameTimeTicker(20);
        starField = new GameStarfield(width, height);
        this.gameInput = gameInput;
        this.learning = learning;
        if (this.learning) {
            try {
                brain = NeuralNetworkQLearning.loadFromFile();
            } catch (Exception e) {
                System.out.println("Creating new Q-Learning Neural Network");
                brain = new NeuralNetworkQLearning(FEED_DATA_SIZE, HIDDEN_NODES1, HIDDEN_NODES2, OUTPUT_NODES);
            }
        }
    }

    /**
     * Tetris start.
     */
    public void start() {
        stack.start();
        stack.nextIteration();
        if (learning) {
            stack.setCurrentSpeed(learningSpeed);
        } else {
            stack.setCurrentSpeed(speed);
        }
        GameColorPalette.getInstance().setRandomPalette();
        starField.setColorPalette(GameColorPalette.getInstance().getCurrentPalette());
        tickDown = new GameTimeTicker(stack.getCurrentSpeed());
        nextShape = createNextShape();
        if (!learning) {
            gameAudio.musicBackgroundPlay();
        }
        musicOn = true;
        nextShape();
        lastAction = -1;
    }

    private void nextShape() {
        Shape currentShape = nextShape;
        nextShape = createNextShape();
        stack.setShapes(currentShape, nextShape);
    }

    private Shape createNextShape() {
        Shape nb = sf.getRandomShape();
        if (nb != null) {
            nb.setColPosition((COLS / 2) - 2);
            nb.setRowPosition(0);
            nb.rotateRight();
        }
        return nb;
    }

    /**
     * Update.
     */
    public void update() {
        if (tickBackground.tick()) {
            starField.update();
        }
        if (tickControl.tick()) {
            handleInput();
        }
        if (tickDown.tick()) {
            if (stack.getState() != State.DELETINGROWS) {
                if (stack.getLevel() == 0) {
                    stack.nextLevel();
                }
                if (stack.getState() != State.GAMEOVER && stack.getState() != State.PAUSED && stack.getState() != State.CHANGINGLEVEL) {
                    if (stack.getCurrentShape() == null) {
                        nextShape();
                    } else {
                        if (stack.moveShapeDown()) {
                            if (learning) {
                                double[] currentState = getFeedData();
                                double reward = calculateReward();
                                System.out.println("Reward: " + reward);
                                brain.learn(lastState, lastAction, reward, currentState, false);
                                lastState = currentState;
                            }
                        }
                    }
                }
            }
        }
        if (stack.getState() == State.GAMEOVER) {
            if (learning) {
                System.out.printf("Holes: %.0f, S.holes: %.0f, Height: %d, Avg.height: %.2f, Bumpiness: %.2f, " +
                                "Stable rows: %.0f, Blocked rows: %.0f, Nearly full rows: %.0f, Dropped: %.0f%n",
                        stack.getNumberofHoles(), stack.getSurroundingHoles(), stack.getMaxHeight(),
                        stack.getAvgColumnHeights(), stack.getBumpiness(), stack.getStableRows(),
                        stack.getBlockedRows(), stack.getNearlyFullRows(), stack.getDroppedElements());
                double[] currentState = getFeedData();
                double reward = calculateFinishReward();
                brain.learn(lastState, lastAction, reward, currentState, true);
                lastState = currentState;
                try {
                    brain.saveToFile();
                } catch (Exception e) {
                    System.out.println("Error saving Q-Learning Neural Network: " + e.getMessage());
                }
                start();
            }
        }
    }

    private void handleInput() {
        if (gameInput.letterP()) {
            if (stack.getState() == State.RUNNING) {
                stack.setState(State.PAUSED);
            } else if (stack.getState() == State.PAUSED) {
                stack.setState(State.RUNNING);
            }
            return;
        }
        if (gameInput.letterR()) {
            start();
            return;
        }
        if (stack.getState() == State.RUNNING && stack.getCurrentShape() != null) {
            if (learning) {
                double[] currentState = getFeedData();
                int action = brain.selectAction(currentState);
                //System.out.println(action);
                lastState = currentState;
                lastAction = action;
                switch (action) {
                    case 0:
                        stack.moveShapeLeft();
                        break;
                    case 1:
                        stack.moveShapeRight();
                        break;
                    case 2:
                        stack.rotateShapeRight();
                        break;
                    case 3:
                        tickDown.setPeriodMilliSecond(DROP_SPEED);
                        break;
                    default:
                        tickDown.setPeriodMilliSecond(stack.getCurrentSpeed());
                        break;
                }
            } else {
                if (gameInput.left()) {
                    stack.moveShapeLeft();
                }
                if (gameInput.right()) {
                    stack.moveShapeRight();
                }
                if (gameInput.space()) {
                    stack.rotateShapeRight();
                }
                if (gameInput.downRepeat()) {
                    tickDown.setPeriodMilliSecond(DROP_SPEED);
                } else {
                    tickDown.setPeriodMilliSecond(stack.getCurrentSpeed());
                }
                if (gameInput.letterM()) {
                    if (musicOn) {
                        gameAudio.musicBackgroundStop();
                        musicOn = false;
                    } else {
                        gameAudio.musicBackgroundPlay();
                        musicOn = true;
                    }
                }
            }
        } else {
            gameInput.clearBuffer();
            if (learning) {
                tickDown.setPeriodMilliSecond(learningSpeed);
            } else {
                tickDown.setPeriodMilliSecond(speed);
            }
        }
    }

    /**
     * Render.
     *
     * @param g2d Graphics2D
     */
    public void render(Graphics2D g2d) {
        starField.render(g2d);
        stack.setTickAnim(tickAnim.tick());
        stack.render(g2d);
    }

    private double[] getFeedData() {
        //Cell[][] stackArea = stack.getStackArea();
        double[] feedData = new double[FEED_DATA_SIZE];
        int k = 0;
        /*
        for (Cell[] cells : stackArea) {
            for (Cell cell : cells) {
                feedData[k++] = cell.getShapeId() != Shape.ShapeType.EMPTY.getShapeTypeId() ? 1.0 : 0.0;
            }
        }
         */

        // Aktuális elem alakja
        /*
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getStackRow();
        } else {
            feedData[k++] = 0.0;
        }
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getStackCol();
        } else {
            feedData[k++] = 0.0;
        }

        if (stack.getCurrentShape() != null) {
            double[] currentShapeData = ShapeFactory.getInstance().shapeToArray(stack.getCurrentShape());
            double[] nextShapeData = ShapeFactory.getInstance().shapeToArray(nextShape);
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = currentShapeData[i];
            }
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = nextShapeData[i];
            }
        } else {
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = 0.0;
            }
            for (int i = 0; i < SHAPE_SIZE; i++) {
                feedData[k++] = 0.0;
            }
        }
         */

        // Oszlopmagasságok
        double[] columnHeights = stack.getColumnHeights();
        for (int i = 0; i < columnHeights.length; i++) {
            feedData[k++] = columnHeights[i];
        }

        // Aktuális elem
        if (stack.getCurrentShape() != null) {
            feedData[k++] = stack.getCurrentShape().getQid();
        } else {
            feedData[k++] = -1;
        }

        // Aktuális elem x és y poziciója
        if (stack.getCurrentShape() != null) {
            feedData[k++] = (double) stack.getCurrentShape().getStackCol() / COLS;
            feedData[k++] = (double) stack.getCurrentShape().getStackRow() / ROWS;
            feedData[k++] = stack.getShapeRotation();
        } else {
            feedData[k++] = -1;
            feedData[k++] = -1;
            feedData[k++] = -1;
        }

        // Következő elem
        feedData[k++] = nextShape.getQid();

        // Lyukak száma
        feedData[k++] = stack.getNumberofHoles() / ROWS;

        // Majdnem teli sorok
        feedData[k++] = stack.getNearlyFullRows();

        // Blokkolt sorok
        feedData[k++] = stack.getBlockedRows();

        // Magasság különbségek
        feedData[k++] = stack.getAvgColumnHeights();

        // Körbevett lukak
        feedData[k++] = stack.calculateHolesSurroundings();

        // Stabil sorok
        feedData[k++] = stack.countStableRows();

        // Maximum magasság
        feedData[k++] = stack.calculateMaxHeight();

        // Egyenetlenség
        feedData[k++] = stack.calculateBumpiness();

        // Játék állapot
        feedData[k++] = stack.getGameScore();
        feedData[k++] = stack.getGameLevel();
        feedData[k++] = stack.getAllFullRows();
        feedData[k++] = stack.getDroppedElements();

        /*
        InputNormalizer normalizer = new InputNormalizer(FEED_DATA_SIZE);
        double[] normalizedData = normalizer.normalizeAutomatically (feedData);
        for (int i = 0; i < normalizedData.length; i++) {
            if (Double.isNaN(normalizedData[i])) {
                System.out.println("NaN detektálva normalizálás után! Index: " + k);
            }
        }
        System.out.println();
        for (int i = 0; i < normalizedData.length; i++) {
            System.out.print(normalizedData[i] + " ");

        }
        System.out.println();
         */
        //return normalizedData;
        return feedData;
    }

    private double calculateReward() {
        double reward = 0;

        // Kisebb jutalom a teljes sorokért
        reward += stack.getAllFullRows() * 2;

        // Stabil sorokért és majdnem teljes sorokért kisebb jutalom
        reward += stack.getStableRows() * 1;
        reward += stack.getNearlyFullRows() * 0.5;

        // Büntetés a lyukakért és a magasságért
        reward -= stack.getNumberofHoles() * 3; // Több büntetés a lyukakért
        reward -= stack.getMaxHeight() * 2; // Magasabb büntetés a magasságért
        reward -= stack.getAvgColumnHeights() * 0.5; // Kisebb büntetés az átlagos magasságokért
        reward -= stack.getBumpiness() * 0.5; // Kisebb büntetés az egyenetlenségért
        reward -= stack.getSurroundingHoles() * 2; // Magasabb büntetés a körbevett lyukakért
        reward -= stack.getBlockedRows() * 0.5;

        // Túlélési idő jutalmazása
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        reward += Math.log(elapsedTime + 1) * 0.1;

        // Külön jutalom, ha nincs lyuk
        if (stack.getNumberofHoles() == 0) {
            reward += 2;
        }

        // Külön jutalom alacsony magasságért
        if (stack.getMaxHeight() <= 5) {
            reward += 1;
        }

        // Jutalom a játék folytatásáért
        reward += 0.1;

        return reward / 10;
    }


    private double calculateFinishReward() {
        double reward = 20;

        // Nagy jutalom a teljes sorokért
        reward += stack.getAllFullRows() * 40;

        // Büntetés a lyukakért a magasságért a körbevett lyukakért és blokkolt sorokért
        reward -= stack.getNumberofHoles() * 4;
        reward -= stack.getAvgColumnHeights() * 1.5;
        reward -= stack.getBumpiness() * 1.5;
        reward -= stack.getSurroundingHoles() * 2;
        reward -= stack.getBlockedRows() * 1.5;

        // Jutalom a stabil sorokért és majdnem tele sorokért
        reward += stack.getStableRows() * 10;
        reward += stack.getNearlyFullRows() * 5;

        // Jelentős jutalom a teljes játékpontszámért és a szintért
        reward += Math.log(stack.getGameScore() + 1) * 0.5;
        reward += stack.getGameLevel() * 3;

        // Túlélési időért jutalom
        double elapsedTime = stack.getElapsedTimeLong() / 1000.0;
        reward += Math.log(elapsedTime + 1) * 0.5;

        // Külön jutalom, ha nincs lyuk
        if (stack.getNumberofHoles() == 0) {
            reward += 10;
        }

        reward += stack.getDroppedElements() * 2;

        return reward / 10;
    }

}
