package hu.nye.vpe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import hu.nye.vpe.gaming.GameColorPalette;

/**
 * Shape factory class.
 */
public class ShapeFactory {
    private static final int MAX_SHAPE_WIDTH = 4;
    private static final int MAX_SHAPE_HEIGHT = 4;
    private static final ShapeFactory shapeFactory = new ShapeFactory();
    private static final int shapeNum = 7;
    private final int[][] pixelsEmpty = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Shape shapeEmpty = new Shape(0, 0, Color.WHITE, 1, pixelsEmpty);
    private final int[][] pixelsErased = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Shape shapeErased = new Shape(99, 40, Color.BLACK, 1, pixelsErased);
    private final int[][] pixelsLoaded = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1} };
    private final Shape shapeLoaded = new Shape(90, 120, new Color(60, 60, 60), 1, pixelsLoaded);
    private final ArrayList<Shape> shapeArray = new ArrayList<Shape>();
    private static final GameColorPalette palette = GameColorPalette.getInstance();
    private static final Random rnd = new Random();
    private final ShapePool shapePool = ShapePool.getInstance();

    private ShapeFactory() {
        init();
    }

    public static ShapeFactory getInstance() {
        return shapeFactory;
    }

    private void init() {
        shapeArray.add(new Shape(1, 10, Color.WHITE, 1, new int[][]{{1}, {1}, {1}, {1}})); // Shape I
        shapeArray.add(new Shape(2, 20, Color.WHITE, 2, new int[][]{{0, 1}, {0, 1}, {1, 1}})); // Shape J
        shapeArray.add(new Shape(3, 20, Color.WHITE, 2, new int[][]{{1, 0}, {1, 0}, {1, 1}})); // Shape L
        shapeArray.add(new Shape(4, 10, Color.WHITE, 2, new int[][]{{1, 1}, {1, 1}})); // Shape O
        shapeArray.add(new Shape(5, 30, Color.WHITE, 3, new int[][]{{1, 0}, {1, 1}, {0, 1}})); // Shape S
        shapeArray.add(new Shape(6, 20, Color.WHITE, 2, new int[][]{{1, 0}, {1, 1}, {1, 0}})); // Shape T
        shapeArray.add(new Shape(7, 30, Color.WHITE, 2, new int[][]{{0, 1}, {1, 1}, {1, 0}})); // Shape Z
    }

    /**
     * Get random shape.
     *
     * @return Shape
     */
    public Shape getRandomShape(int shapeId) {
        Shape poolShape = shapePool.getShape();  // Megpróbálunk egy Shape-et kérni a poolból
        if (poolShape != null) {
            return poolShape;  // Ha van elérhető shape, visszaadjuk
        }
        Shape randomShape;
        // Ha nincs elérhető shape a poolban, létrehozunk egy újat
        if (shapeId == -1) {
            int rsn = rnd.nextInt(shapeArray.size());
            randomShape = shapeArray.get(rsn);
        } else {
            randomShape = shapeArray.get(shapeId);
        }
        int[][] pixels = new int[randomShape.getPixels().length][randomShape.getPixels()[0].length];
        for (int a = 0; a < randomShape.getPixels().length; a++) {
            for (int b = 0; b < randomShape.getPixels()[a].length; b++) {
                pixels[a][b] = randomShape.getPixels()[a][b];
            }
        }
        Shape newShape = new Shape(
                randomShape.getId(),
                randomShape.getScore(),
                palette.getCurrentPalette()[randomShape.getId()],
                randomShape.getWeight(),
                pixels
        );
        int rn = rnd.nextInt(10);
        if (rn == 1) {
            putBonusToTile(newShape);
        }
        return newShape;
    }

    public void releaseShape(Shape shape) {
        shapePool.releaseShape(shape);  // Shape visszaadása a poolba
    }

    /**
     * Get empty shape.
     *
     * @return EmptyShape
     *
     */
    public Shape getEmptyShape() {
        return new Shape(
                shapeEmpty.getId(),
                shapeEmpty.getScore(),
                shapeEmpty.getColor(),
                shapeEmpty.getWeight(),
                shapeEmpty.getPixels()
        );
    }

    /**
     * Get erased shape.
     *
     * @return ErasedShape
     *
     */
    public Shape getErasedShape() {
        return new Shape(
                shapeErased.getId(),
                shapeErased.getScore(),
                shapeErased.getColor(),
                shapeErased.getWeight(),
                shapeErased.getPixels()
        );
    }

    /**
     * Get loaded shape.
     *
     * @return LoadedShape
     *
     */
    public Shape getLoadedShape() {
        return new Shape(
                shapeLoaded.getId(),
                shapeLoaded.getScore(),
                shapeLoaded.getColor(),
                shapeLoaded.getWeight(),
                shapeLoaded.getPixels()
        );
    }

    private void putBonusToTile(Shape shape) {
        Random randomPixel = new Random();
        int bonusPixel = randomPixel.nextInt(shape.getPixelsNumber());
        int pixelCounter = 0;
        for (int i = 0; i < shape.getPixels().length; i++) {
            for (int j = 0; j < shape.getPixels()[i].length; j++) {
                if (shape.getPixels()[i][j] == 1) {
                    pixelCounter++;
                    if (pixelCounter == bonusPixel) {
                        shape.setPixelBonus(i, j);
                    }
                }
            }
        }
    }

    /**
     * Convert shape to double array, where 1.0 pixel on, 0,0 pixel off.
     *
     * @param shape shape which convert to.
     *
     * @return double selected shape array
     */
    public double[] shapeToArray(Shape shape) {
        double[] output = new double[MAX_SHAPE_WIDTH * MAX_SHAPE_HEIGHT];
        int[][] pixels = shape.getPixels();

        for (int i = 0; i < MAX_SHAPE_HEIGHT; i++) {
            for (int j = 0; j < MAX_SHAPE_WIDTH; j++) {
                if (i < pixels.length && j < pixels[i].length) {
                    output[i * MAX_SHAPE_WIDTH + j] = pixels[i][j] == 1 ? 1.0 : 0.0;
                } else {
                    output[i * MAX_SHAPE_WIDTH + j] = 0.0;
                }
            }
        }

        return output;
    }
}
