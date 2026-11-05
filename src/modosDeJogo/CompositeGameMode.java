package modosDeJogo;

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Composes several standard modes into one round. Each child keeps its own
 * mechanics, while this class coordinates lifecycle calls and shared rules.
 */
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
    /** Initializes every child with the same snake and score for this composite round. */
    @Override public void init(Snake snake, ScoreManager score) { for (GameMode mode : modes) mode.init(snake, score); }
    /** Updates every child in deterministic composition order. */
    @Override public void update(GameModeContext context) { for (GameMode mode : modes) mode.update(context); }
    /** Draws all child overlays in composition order. */
    @Override public void draw(Graphics graphics, int cellSize) { for (GameMode mode : modes) mode.draw(graphics, cellSize); }
    /** Broadcasts food placement to every child. */
    @Override public void onFoodPlaced(Point food) { for (GameMode mode : modes) mode.onFoodPlaced(food); }
    /** Broadcasts food collection to every child. */
    @Override public void onFoodCollected(GameModeContext context) { for (GameMode mode : modes) mode.onFoodCollected(context); }
    /** Broadcasts key input to all children. */
    @Override public void onKeyPressed(int keyCode) { for (GameMode mode : modes) mode.onKeyPressed(keyCode); }
    /** Applies directional transformations from left to right. */
    @Override public Direction adjustDirection(Direction direction) {
        Direction result = direction;
        for (GameMode mode : modes) result = mode.adjustDirection(result);
        return result;
    }
    /** The first selected Normal/Reverse mode decides the food/player representation conflict. */
    @Override public boolean usesFood() {
        int normal = indexOf("normal"), reverse = indexOf("reverse");
        if (normal >= 0 && reverse >= 0) return normal < reverse;
        if (reverse >= 0) return false;
        for (GameMode mode : modes) if (!mode.usesFood()) return false;
        return true;
    }
    /** The first selected Normal/Reverse mode decides the visual player representation conflict. */
    @Override public boolean rendersPlayerAsFood() {
        int normal = indexOf("normal"), reverse = indexOf("reverse");
        if (normal >= 0 && reverse >= 0) return reverse < normal;
        if (reverse >= 0) return true;
        for (GameMode mode : modes) if (mode.rendersPlayerAsFood()) return true;
        return false;
    }
    /** Wrap-around is enabled when any selected child requests it. */
    @Override public boolean wrapsAtEdges() { return modes.stream().anyMatch(GameMode::wrapsAtEdges); }
    /** Any child can mark a cell as blocked. */
    @Override public boolean isPositionBlocked(Point position) { return modes.stream().anyMatch(m -> m.isPositionBlocked(position)); }
    /** The fastest selected child determines the composite tick delay. */
    @Override public int getTickDelay(int score) { return modes.stream().mapToInt(m -> m.getTickDelay(score)).min().orElse(100); }
    /** The round ends when any child reports a terminal state. */
    @Override public boolean isGameOver() { return modes.stream().anyMatch(GameMode::isGameOver); }
    private int indexOf(String modeId) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).getId().equals(modeId)) return i;
        return -1;
    }
}
