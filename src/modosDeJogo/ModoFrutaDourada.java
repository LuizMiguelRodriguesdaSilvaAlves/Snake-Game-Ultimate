package modosDeJogo;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.GoldenFruitException;
import sistemaDeMovimentacaoGeral.Snake;

/** Creates a time-limited bonus fruit that grants score and growth. */
public final class ModoFrutaDourada implements GameMode {
    private static final int MIN_SPAWN_DELAY = 65;
    private static final int MAX_SPAWN_DELAY = 95;
    private static final int GOLDEN_LIFETIME = 70;
    private final Random random = new Random();
    private Point goldenFruit;
    private int spawnTimer;
    private int lifetime;
    private int animationTick;
    @Override public String getId() { return "golden-fruit"; }
    @Override public String getName() { return "Fruta Dourada"; }
    /** Resets the bonus-fruit timer and animation. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || score == null) throw new GoldenFruitException("Golden Fruit mode requires a player and score manager.");
        goldenFruit = null; spawnTimer = nextSpawnDelay(); lifetime = 0; animationTick = 0;
    }
    /** Spawns, ages and collects the bonus fruit without replacing regular food. */
    @Override public void update(GameModeContext context) {
        validateContext(context); animationTick++;
        if (goldenFruit == null) {
            if (--spawnTimer <= 0) { goldenFruit = findFreeCell(context); lifetime = GOLDEN_LIFETIME; }
            return;
        }
        if (goldenFruit.equals(context.getSnake().getHead())) {
            context.getScore().addPoints(5); context.getSnake().grow();
            goldenFruit = null; lifetime = 0; spawnTimer = nextSpawnDelay(); return;
        }
        if (--lifetime <= 0) { goldenFruit = null; lifetime = 0; spawnTimer = 35 + random.nextInt(25); }
    }
    /** Renders the bonus collectible; its timer is shown in the HUD. */
    @Override public void draw(Graphics graphics, int cellSize) {
        if (goldenFruit == null) return;
        int inset = (animationTick / 4) % 2 == 0 ? 2 : 4;
        int x = goldenFruit.x * cellSize, y = goldenFruit.y * cellSize;
        graphics.setColor(new Color(255, 205, 25));
        graphics.fillOval(x + inset, y + inset, cellSize - inset * 2, cellSize - inset * 2);
        graphics.setColor(Color.WHITE); graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.drawString("★", x + 5, y + 15);
    }
    /** Displays the spawn countdown or remaining lifetime outside the board. */
    @Override public void drawHud(Graphics graphics, int width, int height) {
        graphics.setColor(new Color(255, 215, 65)); graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        String status = goldenFruit == null ? "GOLDEN FRUIT IN: " + spawnTimer + " TICKS"
                : "GOLDEN FRUIT: +5 POINTS  |  EXPIRES IN: " + lifetime;
        graphics.drawString(status, 12, 18);
    }
    /** Returns the bonus fruit position as a defensive copy. */
    public Point getGoldenFruitPosition() { return goldenFruit == null ? null : new Point(goldenFruit); }
    public int getLifetime() { return lifetime; }
    private void validateContext(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null || context.getColumns() < 3 || context.getRows() < 3) {
            throw new GoldenFruitException("Golden Fruit mode requires a valid game context and positive board dimensions.");
        }
    }
    private int nextSpawnDelay() { return MIN_SPAWN_DELAY + random.nextInt(MAX_SPAWN_DELAY - MIN_SPAWN_DELAY + 1); }
    /** Finds an interior cell free from the player, regular fruit and obstacles. */
    private Point findFreeCell(GameModeContext context) {
        for (int attempt = 0; attempt < 500; attempt++) {
            Point candidate = new Point(1 + random.nextInt(context.getColumns() - 2), 1 + random.nextInt(context.getRows() - 2));
            if (isFreeInteriorCell(candidate, context)) return candidate;
        }
        for (int y = 1; y < context.getRows() - 1; y++) for (int x = 1; x < context.getColumns() - 1; x++) {
            Point candidate = new Point(x, y);
            if (isFreeInteriorCell(candidate, context)) return candidate;
        }
        throw new GoldenFruitException("No free interior cell is available for the golden fruit.");
    }
    private boolean isFreeInteriorCell(Point candidate, GameModeContext context) {
        return candidate.x > 0 && candidate.x < context.getColumns() - 1 && candidate.y > 0 && candidate.y < context.getRows() - 1
                && !context.getSnake().getBody().contains(candidate) && !candidate.equals(context.getFood()) && !context.isPositionBlocked(candidate);
    }
}
