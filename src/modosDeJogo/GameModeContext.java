package modosDeJogo;

import java.awt.Point;
import java.util.function.Predicate;
import java.util.function.Consumer;
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
    private final boolean playerAsFood;
    private final Consumer<GameModeContext> enemyDefeatedHandler;
    private Point food;
    private boolean endGameRequested;

    /** Creates a context snapshot without additional mode-specific obstacles. */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows, int cellSize) {
        this(snake, score, food, columns, rows, cellSize, position -> false, false, null);
    }

    /** Creates a context snapshot with a query for the combined obstacle map. */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows,
            int cellSize, Predicate<Point> blockedPosition) {
        this(snake, score, food, columns, rows, cellSize, blockedPosition, false, null);
    }

    /** Creates a context snapshot with obstacle queries and the current player identity. */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows,
            int cellSize, Predicate<Point> blockedPosition, boolean playerAsFood) {
        this(snake, score, food, columns, rows, cellSize, blockedPosition, playerAsFood, null);
    }

    /**
     * Creates a full context snapshot, including the callback used to notify the active
     * composition when a mode-specific enemy or target is defeated.
     */
    public GameModeContext(Snake snake, ScoreManager score, Point food, int columns, int rows,
            int cellSize, Predicate<Point> blockedPosition, boolean playerAsFood,
            Consumer<GameModeContext> enemyDefeatedHandler) {
        this.snake = snake;
        this.score = score;
        this.food = food == null ? null : new Point(food);
        this.columns = columns;
        this.rows = rows;
        this.cellSize = cellSize;
        this.blockedPosition = blockedPosition == null ? position -> false : blockedPosition;
        this.playerAsFood = playerAsFood;
        this.enemyDefeatedHandler = enemyDefeatedHandler;
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
    /** Returns whether the player is currently represented as the hunted fruit. */
    public boolean isPlayerAsFood() { return playerAsFood; }
    /** Returns whether a cell is occupied by an obstacle in any selected mode. */
    public boolean isPositionBlocked(Point position) {
        return position != null && blockedPosition.test(new Point(position));
    }

    /** Notifies the active mode composition that a mode-specific target was defeated. */
    public void notifyEnemyDefeated() {
        if (enemyDefeatedHandler != null) enemyDefeatedHandler.accept(this);
    }

    /** Requests a game-over transition after the current update finishes. */
    public void requestGameOver() { endGameRequested = true; }
    /** Indicates whether the current update requested game over. */
    public boolean isGameOverRequested() { return endGameRequested; }
}
