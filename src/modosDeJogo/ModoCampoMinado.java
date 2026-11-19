package modosDeJogo;

import sistemaDeMovimentacaoGeral.Snake;
import pontuacao.ScoreManager;
import exceptions.CampoMinadoException;
import exceptions.MinaExplodiuException;

import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Minefield mode supports two player identities:
 * - A snake player receives one mine whenever regular food is replaced.
 * - A hunted-fruit player receives one to five random mines at every ten points.
 * - A snake paired with Run & Gun receives a random mine wave after each hostile-fruit defeat.
 * - Mines never occupy the outer wall, the snake, active food or a combined-mode obstacle.
 * - Touching a mine starts a warning; fruit identity makes the explosion fatal even after escape.
 * - Snake identity keeps the original 4x4 blast damage behavior.
 */
public class ModoCampoMinado implements GameMode {
    private static final int MINES_PER_FOOD = 1;
    private static final int FRUIT_SCORE_INTERVAL = 10;
    private static final int MAX_FRUIT_IDENTITY_MINES = 5;
    private static final int EXPLOSION_TICKS = 6;

    private final Set<Point> minas = new HashSet<>();
    private final Random rnd = new Random();
    private ScoreManager scoreManager;
    private Snake snake;
    private int cols = 30;
    private int rows = 20;
    private int nextFruitIdentityMilestone = FRUIT_SCORE_INTERVAL;
    private boolean playerAsFood;

    // A positive countdown means the player has triggered a mine.
    private Point explodingMine;
    private int explosionTick;

    public ModoCampoMinado() { }

    public ModoCampoMinado(int cols, int rows) {
        if (cols < 3 || rows < 3) throw new CampoMinadoException("Minefield board dimensions must be at least 3x3.");
        this.cols = cols;
        this.rows = rows;
    }

    /** Stable identifier used to keep this mode score independent. */
    @Override public String getId() { return "minefield"; }

    @Override public String getName() { return "Campo Minado"; }

    /** Resets mines, explosion state and score milestones for a new round. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || score == null) {
            throw new CampoMinadoException("Minefield requires an initialized snake and score manager.");
        }
        minas.clear();
        this.scoreManager = score;
        this.snake = snake;
        this.explodingMine = null;
        this.explosionTick = 0;
        this.nextFruitIdentityMilestone = FRUIT_SCORE_INTERVAL;
        this.playerAsFood = false;
    }

    /**
     * Reads the authoritative player identity and combined obstacle map.
     * Fruit identity creates random mine waves at every ten-point milestone.
     */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null) {
            throw new CampoMinadoException("Minefield requires a valid game-mode update context.");
        }
        cols = context.getColumns();
        rows = context.getRows();
        snake = context.getSnake();
        scoreManager = context.getScore();
        playerAsFood = context.isPlayerAsFood();

        if (playerAsFood) {
            int currentScore = scoreManager.getCurrentScore();
            while (currentScore >= nextFruitIdentityMilestone) {
                int randomMineCount = 1 + rnd.nextInt(MAX_FRUIT_IDENTITY_MINES);
                addRandomMinesAvoiding(context.getFood(), randomMineCount, context);
                nextFruitIdentityMilestone += FRUIT_SCORE_INTERVAL;
            }
        }
        update(snake);
    }

    /** Advances mine-trigger detection and the warning animation. */
    @Override public void update(Snake snake) {
        if (snake == null || snake.getHead() == null) {
            throw new CampoMinadoException("Minefield cannot update without an initialized snake.");
        }
        if (explosionTick > 0) {
            explosionTick--;
            if (explosionTick == 0) {
                applyExplosionEffect(snake, explodingMine);
                explodingMine = null;
            }
            return;
        }

        Point head = snake.getHead();
        Iterator<Point> iterator = minas.iterator();
        while (iterator.hasNext()) {
            Point mine = iterator.next();
            if (mine.equals(head)) {
                iterator.remove();
                explodingMine = new Point(mine);
                explosionTick = EXPLOSION_TICKS;
                return;
            }
        }
    }

    /** Draws mines and the warning pulse for the active detonation. */
    @Override public void draw(Graphics graphics, int cellSize) {
        graphics.setColor(Color.GRAY.darker());
        for (Point mine : minas) {
            graphics.fillRect(mine.x * cellSize + 4, mine.y * cellSize + 4,
                    cellSize - 8, cellSize - 8);
        }

        if (explosionTick > 0 && explodingMine != null) {
            float phase = (float) explosionTick / EXPLOSION_TICKS;
            int expand = (int) ((1.0 - phase) * (cellSize * 3));
            int centerX = explodingMine.x * cellSize + cellSize / 2;
            int centerY = explodingMine.y * cellSize + cellSize / 2;
            Graphics2D graphics2D = (Graphics2D) graphics;
            Composite oldComposite = graphics2D.getComposite();
            graphics2D.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.4f + 0.6f * (1f - phase)));
            graphics2D.setColor(Color.ORANGE);
            graphics2D.fillOval(centerX - expand / 2, centerY - expand / 2, expand, expand);
            graphics2D.setComposite(oldComposite);

            graphics2D.setColor(Color.RED);
            int startX = (explodingMine.x - 1) * cellSize;
            int startY = (explodingMine.y - 1) * cellSize;
            graphics2D.setStroke(new BasicStroke(2));
            graphics2D.drawRect(startX, startY, 4 * cellSize, 4 * cellSize);
        }
    }

    /** Preserves the legacy one-mine-per-fruit callback for simple callers. */
    @Override public void onFoodPlaced(Point newFood) {
        addRandomMinesAvoiding(newFood, MINES_PER_FOOD, null);
    }

    /**
     * Uses the full placement context so mines avoid Maze obstacles and the board border.
     * Fruit identity does not receive food-placement mines; it uses score milestones instead.
     */
    @Override public void onFoodPlaced(GameModeContext context) {
        if (context == null || context.getSnake() == null) {
            throw new CampoMinadoException("Minefield requires a valid food-placement context.");
        }
        cols = context.getColumns();
        rows = context.getRows();
        snake = context.getSnake();
        playerAsFood = context.isPlayerAsFood();
        if (playerAsFood) return;
        addRandomMinesAvoiding(context.getFood(), MINES_PER_FOOD, context);
    }

    /**
     * Adds a mine wave after Run & Gun defeats a hostile fruit.
     * Reverse identity is already handled by the ten-point score milestone in update().
     */
    @Override public void onEnemyDefeated(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null) {
            throw new CampoMinadoException("Minefield requires a valid target-defeat context.");
        }
        cols = context.getColumns();
        rows = context.getRows();
        snake = context.getSnake();
        scoreManager = context.getScore();
        playerAsFood = context.isPlayerAsFood();
        if (playerAsFood) return;

        int randomMineCount = 1 + rnd.nextInt(MAX_FRUIT_IDENTITY_MINES);
        addRandomMinesAvoiding(context.getFood(), randomMineCount, context);
    }

    /** Exposes the current mine count for deterministic diagnostics and smoke tests. */
    public int getMineCount() { return minas.size(); }

    /**
     * Adds up to the requested number of mines in free interior cells.
     * The supplied context contributes every obstacle from the active composition.
     */
    private void addRandomMinesAvoiding(Point avoid, int requestedCount, GameModeContext context) {
        if (cols < 3 || rows < 3 || requestedCount <= 0) return;
        int attempts = 0;
        int added = 0;
        int maximumAttempts = Math.max(500, cols * rows * 20);
        while (added < requestedCount && attempts < maximumAttempts) {
            attempts++;
            Point candidate = new Point(1 + rnd.nextInt(cols - 2), 1 + rnd.nextInt(rows - 2));
            if (candidate.equals(avoid) || minas.contains(candidate)) continue;
            if (snake != null && snake.getBody().contains(candidate)) continue;
            if (context != null && context.isPositionBlocked(candidate)) continue;
            minas.add(candidate);
            added++;
        }
    }

    /** Applies the existing blast rule, with fruit identity making any triggered mine fatal. */
    private void applyExplosionEffect(Snake snake, Point center) {
        if (center == null) return;
        Point head = snake.getHead();
        if (playerAsFood || isInsideBlast(head, center)) {
            throw new MinaExplodiuException();
        }

        int hitSegments = 0;
        int index = 0;
        for (Point segment : snake.getBody()) {
            if (index > 0 && isInsideBlast(segment, center)) hitSegments++;
            index++;
        }
        if (hitSegments > 0) {
            snake.shrink(hitSegments);
            if (scoreManager != null) scoreManager.removePoints(hitSegments);
        }
    }

    /** Returns whether a cell belongs to the mine's existing 4x4 blast rectangle. */
    private boolean isInsideBlast(Point point, Point center) {
        int minX = center.x - 1;
        int minY = center.y - 1;
        int maxX = center.x + 2;
        int maxY = center.y + 2;
        return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY;
    }
}