package modosDeJogo;

import java.awt.Point;
import java.util.function.Predicate;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Provides controlled access to the mutable state of the current round.
 * Modes use this object instead of depending directly on the Swing panel.
 */
public final class GameModeContext {
    private final Snake snake;
    private final ScoreManager score;
    private final int columns;
    private final int rows;
    private final int cellSize;
    private final Predicate<Point> blockedPosition;
    private Point food;
    private boolean endGameRequested;

    /** Creates a context snapshot without additional mode-specific obstacles. */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows, int cellSize) {
        this(snake, score, food, columns, rows, cellSize, position -> false);
    }

    /** Creates a context snapshot with a query for the combined obstacle map. */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows,
            int cellSize, Predicate<Point> blockedPosition) {
        this.snake = snake;
        this.score = score;
        this.food = food == null ? null : new Point(food);
        this.columns = columns;
        this.rows = rows;
        this.cellSize = cellSize;
        this.blockedPosition = blockedPosition == null ? position -> false : blockedPosition;
    }
    /** Returns the player snake. */
    public Snake getSnake() { return snake; }
    /** Returns the mode-specific score manager. */
    public ScoreManager getScore() { return score; }
    /** Returns a defensive copy of the regular fruit position. */
    public Point getFood() { return food == null ? null : new Point(food); }
    /** Replaces or removes the regular fruit. */
    public void setFood(Point food) { this.food = food == null ? null : new Point(food); }
    /** Returns the board width in cells. */
    public int getColumns() { return columns; }
    /** Returns the board height in cells. */
    public int getRows() { return rows; }
    /** Returns the rendered cell size in pixels. */
    public int getCellSize() { return cellSize; }
    /** Returns whether a cell is occupied by an obstacle in any selected mode. */
    public boolean isPositionBlocked(Point position) {
        return position != null && blockedPosition.test(new Point(position));
    }

    /** Requests a game-over transition after the current update finishes. */
    public void requestGameOver() { endGameRequested = true; }
    /** Indicates whether the current update requested game over. */
    public boolean isGameOverRequested() { return endGameRequested; }
}
