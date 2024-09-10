package hu.nye.vpe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

/**
 * Shape factory class.
 */
public class ShapeFactory {
    private static final ShapeFactory shapeFactory = new ShapeFactory();
    private final int shapeNum = 7;
    private final int[][] pixelsI = new int[][]{{1}, {1}, {1}, {1}};
    private final Shape shapeI = new Shape(1, 10, Color.WHITE, 1, pixelsI);
    private final int[][] pixelsJ = new int[][]{{0, 1}, {0, 1}, {1, 1}};
    private final Shape shapeJ = new Shape(2, 20, Color.WHITE, 2, pixelsJ);
    private final int[][] pixelsL = new int[][]{{1, 0}, {1, 0}, {1, 1}};
    private final Shape shapeL = new Shape(3, 20, Color.WHITE, 2, pixelsL);
    private final int[][] pixelsO = new int[][]{{1, 1}, {1, 1}};
    private final Shape shapeO = new Shape(4, 10, Color.WHITE, 2, pixelsO);
    private final int[][] pixelsS = new int[][]{{1, 0}, {1, 1}, {0, 1}};
    private final Shape shapeS = new Shape(5, 30, Color.WHITE, 3, pixelsS);
    private final int[][] pixelsT = new int[][]{{1, 0}, {1, 1}, {1, 0}};
    private final Shape shapeT = new Shape(6, 20, Color.WHITE, 2, pixelsT);
    private final int[][] pixelsZ = new int[][]{{0, 1}, {1, 1}, {1, 0}};
    private final Shape shapeZ = new Shape(7, 30, Color.WHITE, 2, pixelsZ);
    private final int[][] pixelsEmpty = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Shape shapeEmpty = new Shape(0, 0, Color.WHITE, 1, pixelsEmpty);
    private final int[][] pixelsErased = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
    private final Shape shapeErased = new Shape(99, 40, Color.BLACK, 1, pixelsErased);
    private final int[][] pixelsLoaded = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1} };
    private final Shape shapeLoaded = new Shape(90, 120, new Color(60, 60, 60), 1, pixelsLoaded);
    private final ArrayList<Shape> shapeArray = new ArrayList<Shape>();
    private final ColorPalette palette = ColorPalette.getInstance();
    Random rnd = new Random();

    private ShapeFactory() {
        init();
    }

    public static ShapeFactory getInstance() {
        return shapeFactory;
    }

    private void init() {
        shapeArray.add(shapeI);
        shapeArray.add(shapeJ);
        shapeArray.add(shapeL);
        shapeArray.add(shapeO);
        shapeArray.add(shapeS);
        shapeArray.add(shapeT);
        shapeArray.add(shapeZ);
    }

    /**
     * Get random shape.
     *
     * @return Shape
     *
     */
    public Shape getRandomShape() {
        int rsn = rnd.nextInt(shapeArray.size());
        Shape randomShape = shapeArray.get(rsn);
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

    /**
     * Get empty shape.
     *
     * @return EmptyShape
     *
     */
    public Shape getEmptyShape() {
        Shape emptyShape = new Shape(
                shapeEmpty.getId(),
                shapeEmpty.getScore(),
                shapeEmpty.getColor(),
                shapeEmpty.getWeight(),
                shapeEmpty.getPixels()
        );
        return emptyShape;
    }

    /**
     * Get erased shape.
     *
     * @return ErasedShape
     *
     */
    public Shape getErasedShape() {
        Shape erasedShape = new Shape(
                shapeErased.getId(),
                shapeErased.getScore(),
                shapeErased.getColor(),
                shapeErased.getWeight(),
                shapeErased.getPixels()
        );
        return erasedShape;
    }

    /**
     * Get loaded shape.
     *
     * @return LoadedShape
     *
     */
    public Shape getLoadedShape() {
        Shape loadedShape = new Shape(
                shapeLoaded.getId(),
                shapeLoaded.getScore(),
                shapeLoaded.getColor(),
                shapeLoaded.getWeight(),
                shapeLoaded.getPixels()
        );
        return loadedShape;
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

}
