package modosDeJogo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import pontuacao.ScoreManager;
import exceptions.ObstacleModeException;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode that places static blocks around the board. */
public final class ModoObstaculos implements GameMode {
    private final Set<Point> obstacles = new HashSet<>();
    private final Random random = new Random();
    @Override public String getId() { return "obstacles"; }
    @Override public String getName() { return "Labirinto de Obstáculos"; }
    /** Creates a reproducible-sized obstacle field while keeping the spawn area clear. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || snake.getHead() == null) {
            throw new ObstacleModeException("Obstacle mode requires an initialized player snake.");
        }
        obstacles.clear();
        Point head = snake.getHead();
        for (int i = 0, attempts = 0; i < 18 && attempts < 500; attempts++) {
            Point candidate = new Point(1 + random.nextInt(28), 1 + random.nextInt(18));
            if (candidate.distance(head) < 5 || snake.getBody().contains(candidate)) continue;
            if (obstacles.add(candidate)) i++;
        }
    }
    /** Blocks collisions with obstacle cells. */
    @Override public boolean isPositionBlocked(Point position) { return obstacles.contains(position); }
    /** Renders every obstacle as a dark stone tile with a light outline. */
    @Override public void draw(Graphics g, int cellSize) {
        g.setColor(new Color(75, 78, 88));
        for (Point p : obstacles) {
            int x = p.x * cellSize, y = p.y * cellSize;
            g.fillRoundRect(x + 1, y + 1, cellSize - 2, cellSize - 2, 5, 5);
            g.setColor(new Color(150, 155, 170));
            g.drawRoundRect(x + 1, y + 1, cellSize - 2, cellSize - 2, 5, 5);
            g.setColor(new Color(75, 78, 88));
        }
    }
}
