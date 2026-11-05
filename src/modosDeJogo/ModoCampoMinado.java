package modosDeJogo;

import sistemaDeMovimentacaoGeral.Snake;
import pontuacao.ScoreManager;
import exceptions.MinaExplodiuException;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Minefield mode:
 * - Mines are generated after each regular fruit is collected;
 * - Mines cannot overlap the active fruit or any snake segment;
 * - Stepping on a mine starts a short warning animation followed by a 4x4 blast;
 * - If the head is inside the blast area, a MinaExplodiuException ends the round;
 * - Each body segment hit by the blast removes one segment and one score point.
 */
public class ModoCampoMinado implements GameMode {

    private final Set<Point> minas = new HashSet<>();
    private final Random rnd = new Random();
    private ScoreManager scoreManager;
    private Snake snake;
    private int cols = 30;
    private int rows = 20;

    // Explosion animation state.
    private Point explodingMine = null;
    private int explosionTick = 0; // A positive value means the warning animation is active.
    private final int EXPLOSION_TICKS = 6;

    // Number of new mines added whenever a regular fruit is placed.
    private final int MINAS_POR_FRUTA = 1;

    public ModoCampoMinado() {}

    public ModoCampoMinado(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
    }

    /** Stable identifier used to keep this mode score independent. */
    @Override
    public String getId() { return "minefield"; }

    @Override
    public String getName() {
        return "Campo Minado";
    }

    @Override
    public void init(Snake snake, ScoreManager score) {
        minas.clear();
        this.scoreManager = score;
        this.snake = snake;
        this.explodingMine = null;
        this.explosionTick = 0;
    }

    @Override
    public void update(Snake snake) {
        // Advance the active explosion warning animation.
        if (explosionTick > 0) {
            explosionTick--;
            if (explosionTick == 0) {
                // Apply the explosion effect when the warning animation finishes.
                applyExplosionEffect(snake, explodingMine);
                explodingMine = null;
            }
            return; // Do not run mine detection while the animation is active.
        }

        // Detect whether the head stepped on a mine; detonation is delayed for a warning animation.
        Point head = snake.getHead();
        Iterator<Point> it = minas.iterator();

        while (it.hasNext()) {
            Point m = it.next();

            if (m.equals(head)) {
                it.remove();
                explodingMine = new Point(m);
                explosionTick = EXPLOSION_TICKS;
                return;
            }
        }
    }

    @Override
    public void draw(Graphics g, int cellSize) {
        // Draw each mine as a small dark tile.
        g.setColor(Color.GRAY.darker());
        for (Point m : minas) {
            g.fillRect(m.x * cellSize + 4, m.y * cellSize + 4, cellSize - 8, cellSize - 8);
        }

        // Draw the warning pulse while an explosion is active.
        if (explosionTick > 0 && explodingMine != null) {
            // The circle grows and becomes more opaque as the countdown advances.
            float phase = (float) explosionTick / EXPLOSION_TICKS; // 1..0
            int expand = (int) ((1.0 - phase) * (cellSize * 3)); // Expansion amount for the current animation frame.
            int cx = explodingMine.x * cellSize + cellSize/2;
            int cy = explodingMine.y * cellSize + cellSize/2;

            // Draw the translucent warning circle.
            Graphics2D g2 = (Graphics2D) g;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f + 0.6f * (1f - phase)));
            g2.setColor(Color.ORANGE);
            g2.fillOval(cx - expand/2, cy - expand/2, expand, expand);
            g2.setComposite(old);

            // Outline the 4x4 blast area.
            // The blast extends one cell left/up and two cells right/down.
            g2.setColor(Color.RED);
            int sx = (explodingMine.x - 1) * cellSize;
            int sy = (explodingMine.y - 1) * cellSize;
            int w = 4 * cellSize;
            int h = 4 * cellSize;
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(sx, sy, w, h);
        }
    }

    @Override
    public void onFoodPlaced(Point newFood) {
        // Add new mines whenever the regular fruit is placed after a collection.
        // Never place a mine on the currently active fruit.
        addRandomMinesAvoiding(newFood);
    }

    // ----------------------------
    // Helper methods
    // ----------------------------
    private void addRandomMinesAvoiding(Point avoid) {
        // Adds new mines while avoiding the supplied cell, the snake and existing mines.
        int attempts = 0;
        int added = 0;
        while (added < MINAS_POR_FRUTA && attempts < 500) {
            int x = rnd.nextInt(cols);
            int y = rnd.nextInt(rows);
            Point p = new Point(x, y);
            if (p.equals(avoid)) { attempts++; continue; }
            if (minas.contains(p)) { attempts++; continue; }
            if (snake != null && snake.getBody().contains(p)) { attempts++; continue; }
            minas.add(p);
            added++;
            attempts++;
        }
    }

    private void applyExplosionEffect(Snake snake, Point center) {
        if (center == null) return;
        // Calculate the 4x4 blast rectangle around the mine.
        int x0 = center.x - 1;
        int y0 = center.y - 1;
        int x1 = center.x + 2;
        int y1 = center.y + 2;

        // The head inside the blast area causes an immediate game over.
        Point head = snake.getHead();
        if (head.x >= x0 && head.x <= x1 && head.y >= y0 && head.y <= y1) {
            throw new MinaExplodiuException();
        }

        // Count body segments inside the blast area, excluding the head.
        int hitSegments = 0;
        int index = 0;
        for (Point segment : snake.getBody()) {
            if (index > 0 && segment.x >= x0 && segment.x <= x1
                    && segment.y >= y0 && segment.y <= y1) {
                hitSegments++;
            }
            index++;
        }

        if (hitSegments > 0) {
            // Each damaged segment removes one score point and one tail segment.
            snake.shrink(hitSegments);
            if (scoreManager != null) scoreManager.removePoints(hitSegments);
        }
    }
}
