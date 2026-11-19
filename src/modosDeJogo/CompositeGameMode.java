package modosDeJogo;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/** Coordinates child modes and resolves player-identity conflicts before activating Reverse. */
public class CompositeGameMode implements GameMode {
    protected final List<GameMode> modes;
    private final String id;
    private final String name;
    /** Creates a composite with a stable score identifier and ordered children. */
    public CompositeGameMode(String id, String name, List<GameMode> modes) {
        this.id = id; this.name = name; this.modes = new ArrayList<>(modes);
    }
    /** Returns an immutable view of the child modes. */
    public List<GameMode> getModes() { return Collections.unmodifiableList(modes); }
    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    /** Initializes only children whose gameplay identity is active. */
    @Override public void init(Snake snake, ScoreManager score) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.init(snake, score);
    }
    /** Updates active children in deterministic composition order. */
    @Override public void update(GameModeContext context) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.update(context);
    }
    /** Draws active entities inside the board. */
    @Override public void draw(Graphics graphics, int cellSize) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.draw(graphics, cellSize);
    }
    /** Draws mode status in the session HUD. */
    @Override public void drawHud(Graphics graphics, int width, int height) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.drawHud(graphics, width, height);
    }
    /** Broadcasts legacy food placement notifications to active children. */
    @Override public void onFoodPlaced(Point food) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.onFoodPlaced(food);
    }
    /** Broadcasts the full context to active children. */
    @Override public void onFoodPlaced(GameModeContext context) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.onFoodPlaced(context);
    }
    /** Broadcasts food collection to active children. */
    @Override public void onFoodCollected(GameModeContext context) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.onFoodCollected(context);
    }
    /** Broadcasts target-defeat events to active children. */
    @Override public void onEnemyDefeated(GameModeContext context) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.onEnemyDefeated(context);
    }
    /** Broadcasts keyboard input to active children. */
    @Override public void onKeyPressed(int keyCode) {
        for (GameMode mode : modes) if (isModeActive(mode)) mode.onKeyPressed(keyCode);
    }
    /** Applies direction transformations from active children in composition order. */
    @Override public Direction adjustDirection(Direction direction) {
        Direction result = direction;
        for (GameMode mode : modes) if (isModeActive(mode)) result = mode.adjustDirection(result);
        return result;
    }
    /** Normal/Reverse order determines whether the player is a snake or hunted fruit. */
    @Override public boolean usesFood() {
        // Golden Fruit suppresses Reverse, while Run & Gun still controls regular-food availability.
        if (indexOf("golden-fruit") >= 0) {
            for (GameMode mode : modes) {
                if (isModeActive(mode) && !mode.usesFood()) return false;
            }
            return true;
        }
        int normal = indexOf("normal"), reverse = indexOf("reverse");
        if (normal >= 0 && reverse >= 0) return normal < reverse;
        if (reverse >= 0) return false;
        for (GameMode mode : modes) if (isModeActive(mode) && !mode.usesFood()) return false;
        return true;
    }
    /** Renders the player as fruit only when Reverse wins the identity conflict. */
    @Override public boolean rendersPlayerAsFood() {
        // A Golden Fruit composition always keeps the player represented as a snake.
        if (indexOf("golden-fruit") >= 0) return false;
        int normal = indexOf("normal"), reverse = indexOf("reverse");
        if (normal >= 0 && reverse >= 0) return reverse < normal;
        if (reverse >= 0) return true;
        for (GameMode mode : modes) if (isModeActive(mode) && mode.rendersPlayerAsFood()) return true;
        return false;
    }
    /** Enables wrap-around when an active child requests it. */
    @Override public boolean wrapsAtEdges() {
        return modes.stream().anyMatch(mode -> isModeActive(mode) && mode.wrapsAtEdges());
    }
    /** Any active child can mark a cell as blocked. */
    @Override public boolean isPositionBlocked(Point position) {
        return modes.stream().anyMatch(mode -> isModeActive(mode) && mode.isPositionBlocked(position));
    }
    /** The fastest active child determines the timer delay. */
    @Override public int getTickDelay(int score) {
        return modes.stream().filter(this::isModeActive).mapToInt(mode -> mode.getTickDelay(score)).min().orElse(100);
    }
    /** The round ends when an active child reports a terminal state. */
    @Override public boolean isGameOver() {
        return modes.stream().anyMatch(mode -> isModeActive(mode) && mode.isGameOver());
    }
    /** Prevents Reverse AI from running when Normal has priority over it. */
    public boolean isModeActive(GameMode mode) {
        if (mode == null || !"reverse".equals(mode.getId())) return true;
        if (indexOf("golden-fruit") >= 0) return false;
        int normalIndex = indexOf("normal"), reverseIndex = indexOf("reverse");
        return normalIndex < 0 || reverseIndex < normalIndex;
    }
    private int indexOf(String modeId) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).getId().equals(modeId)) return i;
        return -1;
    }
}
