package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.GoldenFruitException;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode that periodically creates a high-value golden fruit. */
public final class ModoFrutaDourada implements GameMode {
    private final Random random = new Random();
    private Point goldenFruit;
    private int spawnTimer;
    @Override public String getId() { return "golden-fruit"; }
    @Override public String getName() { return "Fruta Dourada"; }
    /** Resets the bonus fruit cycle at the beginning of each round. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new GoldenFruitException("Golden Fruit mode cannot initialize without a player snake.");
        goldenFruit = null; spawnTimer = 70;
    }
    /** Spawns a bonus fruit and awards five points plus one growth segment when collected. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null
                || context.getColumns() < 3 || context.getRows() < 3) {
            throw new GoldenFruitException("Golden Fruit mode requires a valid game context and positive board dimensions.");
        }
        if (goldenFruit == null) {
            if (--spawnTimer <= 0) { goldenFruit = findFreeCell(context); spawnTimer = 100; }
            return;
        }
        if (goldenFruit.equals(context.getSnake().getHead())) {
            context.getScore().addPoints(5);
            context.getSnake().grow();
            goldenFruit = null;
            spawnTimer = 90;
        }
    }
    /** Draws the bonus fruit with a gold fill and a small star marker. */
    @Override public void draw(Graphics g, int cellSize) {
        if (goldenFruit == null) return;
        int x = goldenFruit.x * cellSize, y = goldenFruit.y * cellSize;
        g.setColor(new Color(255, 205, 25)); g.fillOval(x + 2, y + 2, cellSize - 4, cellSize - 4);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("★", x + 5, y + 15);
    }
    /** Finds an empty cell that is not occupied by the player or regular food. */
    private Point findFreeCell(GameModeContext context) {
        for (int attempt = 0; attempt < 500; attempt++) {
            Point candidate = new Point(1 + random.nextInt(context.getColumns() - 2),
                    1 + random.nextInt(context.getRows() - 2));
            if (isFreeInteriorCell(candidate, context)) return candidate;
        }
        for (int y = 1; y < context.getRows() - 1; y++) {
            for (int x = 1; x < context.getColumns() - 1; x++) {
                Point candidate = new Point(x, y);
                if (isFreeInteriorCell(candidate, context)) return candidate;
            }
        }
        throw new GoldenFruitException("No free interior cell is available for the golden fruit.");
    }

    /** Checks walls, snake body, regular food and obstacles contributed by other selected modes. */
    private boolean isFreeInteriorCell(Point candidate, GameModeContext context) {
        return candidate.x > 0 && candidate.x < context.getColumns() - 1
                && candidate.y > 0 && candidate.y < context.getRows() - 1
                && !context.getSnake().getBody().contains(candidate)
                && !candidate.equals(context.getFood())
                && !context.isPositionBlocked(candidate);
    }
}
