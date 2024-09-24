package hu.nye.vpe;

import java.util.Stack;

/**
 * Shape pool class.
 */
public class ShapePool {
    private static final ShapePool instance = new ShapePool();
    private final Stack<Shape> availableShapes;

    private ShapePool() {
        availableShapes = new Stack<>();
    }

    public static ShapePool getInstance() {
        return instance;
    }

    /**
     * GetShape method.
     *
     * @return Shape
     */
    public Shape getShape() {
        if (!availableShapes.isEmpty()) {
            return availableShapes.pop();
        }
        return null; // Ha nincs elérhető Shape a pool-ban
    }

    public void releaseShape(Shape shape) {
        availableShapes.push(shape);
    }
}
