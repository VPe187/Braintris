package hu.nye.vpe.tetris;

/**
 * Tetromino type enum.
 */
public enum TetrominoType {
        COMMON(100), EMPTY(0), ERASED(99), LOADED(90), HIDDEN(80);
        private final int typeId;
        TetrominoType(int typeId) {
            this.typeId = typeId;
        }
        public int getTetrominoTypeId() {
            return typeId;
        }
}
