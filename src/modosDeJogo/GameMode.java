package modosDeJogo;

import java.awt.Graphics;
import java.awt.Point;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;
import pontuacao.ScoreManager;

/**
 * Defines the lifecycle and optional capabilities shared by every game mode.
 * Implementations override only the hooks required by their own mechanics.
 */
public interface GameMode {
    /** Returns the stable identifier used for independent score persistence. */
    String getId();

    /** Returns the localized name displayed in the user interface. */
    String getName();

    /** Initializes mode-specific state before the first game tick. */
    void init(Snake snake, ScoreManager score);

    /** Updates the mode using the current game state. */
    default void update(GameModeContext context) { update(context.getSnake()); }

    /** Legacy update hook retained for simple and existing modes. */
    default void update(Snake snake) { }

    /** Draws mode-specific entities above the board. */
    default void draw(Graphics graphics, int cellSize) { }

    /** Notifies the mode whenever the regular food is placed or replaced. */
    default void onFoodPlaced(Point food) { }

    /** Notifies the mode after the player collects regular food. */
    default void onFoodCollected(GameModeContext context) { }

    /** Receives keyboard keys that are not handled by the common movement layer. */
    default void onKeyPressed(int keyCode) { }

    /** Allows a mode to transform a requested movement direction. */
    default Direction adjustDirection(Direction requested) { return requested; }

    /** Whether the mode uses the regular collectible fruit. */
    default boolean usesFood() { return true; }

    /** Whether the player is rendered as a fruit instead of a snake. */
    default boolean rendersPlayerAsFood() { return false; }

    /** Whether crossing a board edge wraps the player to the opposite edge. */
    default boolean wrapsAtEdges() { return false; }

    /** Returns true when the supplied cell is occupied by a mode-specific obstacle. */
    default boolean isPositionBlocked(Point position) { return false; }

    /** Returns the desired Swing timer delay for the current score. */
    default int getTickDelay(int currentScore) { return 100; }

    /** Allows modes with enemies or timers to terminate the current round. */
    default boolean isGameOver() { return false; }
}
