package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import exceptions.BossRushException;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Secret action mode built around four consecutive boss encounters. The player
 * shoots with Space, manages limited energy and survives telegraphed attacks.
 */
public final class ModoBossRush implements GameMode {
    private static final int MAX_LIVES = 3;
    private static final int MAX_ENERGY = 3;
    private static final int STARTING_ENERGY = 2;
    private static final int PLAYER_INVULNERABILITY = 18;
    private static final int INTERMISSION_TICKS = 18;

    private enum BossType {
        CRIMSON_HUNTER("Crimson Hunter", new Color(220, 55, 65), 3),
        VOLT_SENTINEL("Volt Sentinel", new Color(65, 190, 255), 4),
        RIFT_BOMBER("Rift Bomber", new Color(190, 90, 245), 4),
        VOID_SOVEREIGN("Void Sovereign", new Color(235, 220, 255), 5);

        private final String displayName;
        private final Color color;
        private final int baseHealth;

        BossType(String displayName, Color color, int baseHealth) {
            this.displayName = displayName;
            this.color = color;
            this.baseHealth = baseHealth;
        }
    }

    private static final class Boss {
        private final BossType type;
        private Point position;
        private final int maxHealth;
        private int health;
        private int actionCooldown;
        private int moveCooldown;
        private int warningTicks;
        private int attackTicks;
        private int hitFlashTicks;
        private int dashDx;
        private int dashDy;
        private int attackCoordinate;
        private boolean horizontalAttack;
        private int attackVariant;
        private int attackPattern;
        private Point teleportTarget;

        private Boss(BossType type, Point position, int cycle) {
            this.type = type;
            this.position = new Point(position);
            this.maxHealth = type.baseHealth + Math.min(4, Math.max(0, cycle - 1));
            this.health = maxHealth;
            this.actionCooldown = 22;
            this.moveCooldown = 4;
        }
    }

    private static final class Projectile {
        private Point position;
        private final int dx;
        private final int dy;

        private Projectile(Point position, int dx, int dy) {
            this.position = new Point(position);
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static final class Mine {
        private final Point position;
        private int warningTicks;

        private Mine(Point position, int warningTicks) {
            this.position = new Point(position);
            this.warningTicks = warningTicks;
        }
    }

    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Mine> mines = new ArrayList<>();
    private Snake player;
    private ScoreManager score;
    private Boss boss;
    private int bossIndex;
    private int cycle;
    private int bossesDefeated;
    private int lives;
    private int energy;
    private int invulnerabilityTicks;
    private int fireCooldown;
    private int intermissionTicks;
    private int tick;
    private int columns;
    private int rows;
    private boolean gameOver;

    @Override public String getId() { return "boss-rush"; }
    @Override public String getName() { return "Boss Rush"; }

    /** Resets every encounter resource and spawns the first guardian. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || score == null || snake.getHead() == null) {
            throw new BossRushException("Boss Rush requires an initialized player snake and score manager.");
        }
        this.player = snake;
        this.score = score;
        this.bossIndex = 0;
        this.cycle = 1;
        this.bossesDefeated = 0;
        this.lives = MAX_LIVES;
        this.energy = STARTING_ENERGY;
        this.invulnerabilityTicks = 0;
        this.fireCooldown = 0;
        this.intermissionTicks = 0;
        this.tick = 0;
        this.columns = 30;
        this.rows = 20;
        this.gameOver = false;
        this.projectiles.clear();
        this.mines.clear();
        this.boss = createBoss(BossType.CRIMSON_HUNTER, snake.getHead(), columns, rows);
    }

    /** Keeps regular fruit enabled; collecting it replenishes one shot charge. */
    @Override public boolean usesFood() { return true; }

    /** Awards one energy charge for each regular fruit, up to the resource cap. */
    @Override public void onFoodCollected(GameModeContext context) {
        validateContext(context);
        if (energy < MAX_ENERGY) energy++;
    }

    /** Space fires a limited-energy projectile in the snake's current direction. */
    @Override public void onKeyPressed(int keyCode) {
        if (keyCode != KeyEvent.VK_SPACE) return;
        if (player == null || player.getHead() == null || player.getDirection() == null) {
            throw new BossRushException("Boss Rush received a firing request before player initialization.");
        }
        if (boss == null || intermissionTicks > 0 || energy <= 0 || fireCooldown > 0 || gameOver) return;

        int dx = 0;
        int dy = 0;
        switch (player.getDirection()) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        projectiles.add(new Projectile(player.getHead(), dx, dy));
        energy--;
        fireCooldown = 3;
    }

    /** Advances projectiles, boss AI, warning windows, hazards and encounter progression. */
    @Override public void update(GameModeContext context) {
        validateContext(context);
        columns = context.getColumns();
        rows = context.getRows();
        tick++;
        if (invulnerabilityTicks > 0) invulnerabilityTicks--;
        if (fireCooldown > 0) fireCooldown--;
        if (boss != null && boss.hitFlashTicks > 0) boss.hitFlashTicks--;

        updateProjectiles(context);
        if (gameOver) {
            context.requestGameOver();
            return;
        }

        if (boss == null) {
            if (intermissionTicks > 0) intermissionTicks--;
            if (intermissionTicks == 0) spawnNextBoss(context);
            return;
        }

        updateMines(context);
        if (gameOver) {
            context.requestGameOver();
            return;
        }

        if (touchingPlayer(boss.position)) {
            damagePlayer(context);
            repositionBossAwayFromPlayer(context);
        }

        switch (boss.type) {
            case CRIMSON_HUNTER -> updateCrimsonHunter(context);
            case VOLT_SENTINEL -> updateVoltSentinel(context);
            case RIFT_BOMBER -> updateRiftBomber(context);
            case VOID_SOVEREIGN -> updateVoidSovereign(context);
        }

        if (boss != null && touchingPlayer(boss.position)) {
            damagePlayer(context);
            repositionBossAwayFromPlayer(context);
        }
        if (gameOver) context.requestGameOver();
    }

    /** Reports the terminal state to the shared game loop. */
    @Override public boolean isGameOver() { return gameOver; }

    /** Makes later cycles slightly faster without exceeding a fair minimum delay. */
    @Override public int getTickDelay(int currentScore) {
        if (currentScore < 0) throw new BossRushException("Boss Rush cannot calculate speed from a negative score.");
        return Math.max(72, 100 - (cycle - 1) * 4);
    }

    /** Draws combat entities inside the board, keeping status out of the playfield. */
    @Override public void draw(Graphics graphics, int cellSize) {
        if (graphics == null || cellSize <= 0) throw new BossRushException("Boss Rush requires valid drawing graphics and cell size.");
        drawWarnings(graphics, cellSize);
        drawMines(graphics, cellSize);
        drawProjectiles(graphics, cellSize);
        drawBoss(graphics, cellSize);
    }

    /** Read-only status accessors used by the UI smoke tests and diagnostics. */
    public int getLives() { return lives; }
    public int getEnergyCharges() { return energy; }
    public int getCycle() { return cycle; }
    public int getBossesDefeated() { return bossesDefeated; }
    public String getCurrentBossName() { return boss == null ? "Intermission" : boss.type.displayName; }
    public int getCurrentBossHealth() { return boss == null ? 0 : boss.health; }
    public int getCurrentBossMaxHealth() { return boss == null ? 0 : boss.maxHealth; }

    private void validateContext(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null
                || player == null || score == null || context.getColumns() < 30 || context.getRows() < 20) {
            throw new BossRushException("Boss Rush requires an initialized round and a board at least 30 columns by 20 rows.");
        }
        if (context.getSnake() != player || context.getScore() != score) {
            throw new BossRushException("Boss Rush received a context belonging to a different game round.");
        }
    }

    private Boss createBoss(BossType type, Point playerHead, int boardColumns, int boardRows) {
        Point[] corners = {
            new Point(3, 3), new Point(boardColumns - 4, 3),
            new Point(3, boardRows - 4), new Point(boardColumns - 4, boardRows - 4)
        };
        Point selected = corners[0];
        double bestDistance = -1;
        for (Point corner : corners) {
            double distance = corner.distance(playerHead);
            if (distance > bestDistance) {
                selected = corner;
                bestDistance = distance;
            }
        }
        return new Boss(type, selected, cycle);
    }

    private void spawnNextBoss(GameModeContext context) {
        BossType[] sequence = BossType.values();
        boss = createBoss(sequence[bossIndex], context.getSnake().getHead(), columns, rows);
        projectiles.clear();
        mines.clear();
    }

    private void updateProjectiles(GameModeContext context) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.position.translate(projectile.dx, projectile.dy);
            Point position = projectile.position;
            if (position.x < 0 || position.x >= columns || position.y < 0 || position.y >= rows
                    || context.isPositionBlocked(position)) {
                iterator.remove();
                continue;
            }
            if (boss != null && position.equals(boss.position)) {
                iterator.remove();
                boss.health--;
                boss.hitFlashTicks = 3;
                if (boss.health <= 0) defeatBoss();
                if (boss == null) break;
            }
        }
    }

    private void defeatBoss() {
        score.addPoints(5);
        bossesDefeated++;
        boss = null;
        intermissionTicks = INTERMISSION_TICKS;
        projectiles.clear();
        mines.clear();
        if (lives < MAX_LIVES) lives++;
        if (energy < MAX_ENERGY) energy++;
        bossIndex++;
        if (bossIndex >= BossType.values().length) {
            bossIndex = 0;
            cycle++;
            score.addPoints(10);
        }
    }

    private void updateCrimsonHunter(GameModeContext context) {
        Boss current = boss;
        if (current.warningTicks > 0) {
            current.warningTicks--;
            if (current.warningTicks == 0) {
                performDash(context, current, 4);
                current.actionCooldown = Math.max(8, 24 - cycle * 2 - (current.health <= current.maxHealth / 2 ? 5 : 0));
            }
            return;
        }
        if (--current.actionCooldown <= 0) {
            Point target = predictPlayerPosition(context, 2);
            int dx = target.x - current.position.x;
            int dy = target.y - current.position.y;
            if (Math.abs(dx) >= Math.abs(dy)) {
                current.dashDx = Integer.signum(dx);
                current.dashDy = 0;
            } else {
                current.dashDx = 0;
                current.dashDy = Integer.signum(dy);
            }
            if (current.dashDx == 0 && current.dashDy == 0) current.dashDx = 1;
            current.warningTicks = 9;
            return;
        }
        if (--current.moveCooldown <= 0) {
            moveBossTowardPlayer(context, current);
            current.moveCooldown = Math.max(2, 5 - cycle / 2);
        }
    }

    private void updateVoltSentinel(GameModeContext context) {
        Boss current = boss;
        if (current.attackTicks > 0) {
            if (playerOnBeam(context, current)) damagePlayer(context);
            current.attackTicks--;
            return;
        }
        if (current.warningTicks > 0) {
            current.warningTicks--;
            if (current.warningTicks == 0) current.attackTicks = 2;
            return;
        }
        if (--current.actionCooldown <= 0) {
            Point target = context.getSnake().getHead();
            current.horizontalAttack = Math.abs(target.x - current.position.x) >= Math.abs(target.y - current.position.y);
            current.attackCoordinate = current.horizontalAttack ? target.y : target.x;
            current.warningTicks = 11;
            current.actionCooldown = Math.max(12, 30 - cycle * 2 - (current.health <= current.maxHealth / 2 ? 5 : 0));
        }
    }

    private void updateRiftBomber(GameModeContext context) {
        Boss current = boss;
        if (!mines.isEmpty()) return;
        if (--current.actionCooldown <= 0) {
            Point target = context.getSnake().getHead();
            mines.add(new Mine(target, 17));
            if (current.health <= current.maxHealth / 2) {
                mines.add(new Mine(predictPlayerPosition(context, 2), 22));
            }
            current.actionCooldown = Math.max(15, 34 - cycle * 2 - (current.health <= current.maxHealth / 2 ? 6 : 0));
        }
        if (--current.moveCooldown <= 0) {
            moveBossTowardPlayer(context, current);
            current.moveCooldown = 9;
        }
    }

    private void updateVoidSovereign(GameModeContext context) {
        Boss current = boss;
        if (current.attackTicks > 0) {
            if (current.attackVariant == 0 && playerOnBeam(context, current)) damagePlayer(context);
            current.attackTicks--;
            return;
        }
        if (current.warningTicks > 0) {
            current.warningTicks--;
            if (current.warningTicks == 0) {
                if (current.attackVariant == 0) current.attackTicks = 2;
                else {
                    if (current.teleportTarget != null) current.position = new Point(current.teleportTarget);
                    aimDashAtPlayer(current, context.getSnake().getHead());
                    performDash(context, current, 5);
                }
            }
            return;
        }
        if (--current.actionCooldown <= 0) {
            current.attackVariant = current.attackPattern++ % 2;
            Point target = context.getSnake().getHead();
            if (current.attackVariant == 0) {
                current.horizontalAttack = true;
                current.attackCoordinate = target.y;
                current.teleportTarget = null;
            } else {
                current.teleportTarget = findTeleportDestination(context, current.position, target);
            }
            current.warningTicks = 13;
            current.actionCooldown = Math.max(13, 31 - cycle * 2 - (current.health <= current.maxHealth / 2 ? 6 : 0));
        }
    }

    private void updateMines(GameModeContext context) {
        Iterator<Mine> iterator = mines.iterator();
        while (iterator.hasNext()) {
            Mine mine = iterator.next();
            mine.warningTicks--;
            if (mine.warningTicks <= 0) {
                if (playerInsideBlast(context, mine.position, 1)) damagePlayer(context);
                iterator.remove();
            }
        }
    }

    private boolean playerInsideBlast(GameModeContext context, Point center, int radius) {
        for (Point segment : context.getSnake().getBody()) {
            if (Math.abs(segment.x - center.x) <= radius && Math.abs(segment.y - center.y) <= radius) return true;
        }
        return false;
    }

    private boolean playerOnBeam(GameModeContext context, Boss current) {
        for (Point segment : context.getSnake().getBody()) {
            if (current.horizontalAttack && segment.y == current.attackCoordinate) return true;
            if (!current.horizontalAttack && segment.x == current.attackCoordinate) return true;
        }
        return false;
    }

    /** Uses breadth-first pathfinding so pursuing bosses route around walls and maze obstacles. */
    private void moveBossTowardPlayer(GameModeContext context, Boss current) {
        Point start = new Point(current.position);
        Point target = context.getSnake().getHead();
        ArrayDeque<Point> frontier = new ArrayDeque<>();
        Map<Point, Point> previous = new HashMap<>();
        Set<Point> visited = new HashSet<>();
        frontier.add(start);
        visited.add(start);
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!frontier.isEmpty()) {
            Point position = frontier.removeFirst();
            if (position.equals(target)) break;
            for (int[] direction : directions) {
                Point next = new Point(position.x + direction[0], position.y + direction[1]);
                if (next.x <= 0 || next.x >= columns - 1 || next.y <= 0 || next.y >= rows - 1
                        || visited.contains(next) || context.isPositionBlocked(next)) continue;
                if (!next.equals(target) && context.getSnake().getBody().contains(next)) continue;
                visited.add(next);
                previous.put(next, position);
                frontier.addLast(next);
            }
        }

        if (!visited.contains(target)) return;
        Point step = target;
        while (previous.containsKey(step) && !previous.get(step).equals(start)) step = previous.get(step);
        moveBossTo(context, current, step);
    }

    /** Predicts a short, bounded movement lead for bosses with aimed attacks. */
    private Point predictPlayerPosition(GameModeContext context, int leadTicks) {
        Point head = context.getSnake().getHead();
        int dx = 0, dy = 0;
        switch (context.getSnake().getDirection()) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        int x = Math.max(1, Math.min(columns - 2, head.x + dx * leadTicks));
        int y = Math.max(1, Math.min(rows - 2, head.y + dy * leadTicks));
        return new Point(x, y);
    }

    private void moveBossTo(GameModeContext context, Boss current, Point next) {
        if (next.x <= 0 || next.x >= columns - 1 || next.y <= 0 || next.y >= rows - 1
                || context.isPositionBlocked(next)) return;
        if (context.getSnake().getBody().contains(next)) {
            damagePlayer(context);
            return;
        }
        current.position = new Point(next);
    }

    private void aimDashAtPlayer(Boss current, Point target) {
        int dx = target.x - current.position.x;
        int dy = target.y - current.position.y;
        if (Math.abs(dx) >= Math.abs(dy)) {
            current.dashDx = Integer.signum(dx);
            current.dashDy = 0;
        } else {
            current.dashDx = 0;
            current.dashDy = Integer.signum(dy);
        }
        if (current.dashDx == 0 && current.dashDy == 0) current.dashDx = 1;
    }

    private void performDash(GameModeContext context, Boss current, int distance) {
        for (int step = 0; step < distance; step++) {
            Point next = new Point(current.position.x + current.dashDx, current.position.y + current.dashDy);
            if (next.x <= 0 || next.x >= columns - 1 || next.y <= 0 || next.y >= rows - 1
                    || context.isPositionBlocked(next)) break;
            if (context.getSnake().getBody().contains(next)) {
                damagePlayer(context);
                break;
            }
            current.position = next;
        }
    }

    private Point findTeleportDestination(GameModeContext context, Point current, Point playerHead) {
        Point best = null;
        double bestDistance = -1;
        for (int y = 2; y < rows - 2; y++) {
            for (int x = 2; x < columns - 2; x++) {
                Point candidate = new Point(x, y);
                if (candidate.distance(current) < 8 || candidate.distance(playerHead) < 6
                        || context.getSnake().getBody().contains(candidate) || context.isPositionBlocked(candidate)) continue;
                double distance = candidate.distance(playerHead);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best == null ? new Point(current) : best;
    }

    private boolean touchingPlayer(Point position) {
        return player != null && player.getBody().contains(position);
    }

    private void damagePlayer(GameModeContext context) {
        if (invulnerabilityTicks > 0 || gameOver) return;
        lives--;
        invulnerabilityTicks = PLAYER_INVULNERABILITY;
        if (lives <= 0) {
            lives = 0;
            gameOver = true;
            context.requestGameOver();
        }
    }

    private void repositionBossAwayFromPlayer(GameModeContext context) {
        if (boss == null) return;
        Point head = context.getSnake().getHead();
        Point farthest = boss.position;
        double distance = -1;
        for (int y = 2; y < rows - 2; y++) {
            for (int x = 2; x < columns - 2; x++) {
                Point candidate = new Point(x, y);
                if (context.getSnake().getBody().contains(candidate) || context.isPositionBlocked(candidate)) continue;
                double candidateDistance = candidate.distance(head);
                if (candidateDistance > distance) {
                    distance = candidateDistance;
                    farthest = candidate;
                }
            }
        }
        boss.position = new Point(farthest);
        boss.actionCooldown = Math.max(boss.actionCooldown, 12);
    }

    private void drawBoss(Graphics graphics, int cellSize) {
        if (boss == null) return;
        int x = boss.position.x * cellSize;
        int y = boss.position.y * cellSize;
        int inset = Math.max(2, cellSize / 8);
        graphics.setColor(boss.hitFlashTicks > 0 ? Color.WHITE : boss.type.color);
        graphics.fillOval(x + inset, y + inset, cellSize - inset * 2, cellSize - inset * 2);
        graphics.setColor(Color.WHITE);
        graphics.drawOval(x + 1, y + 1, cellSize - 2, cellSize - 2);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, cellSize / 2)));
        String mark = switch (boss.type) {
            case CRIMSON_HUNTER -> "C";
            case VOLT_SENTINEL -> "V";
            case RIFT_BOMBER -> "R";
            case VOID_SOVEREIGN -> "Ω";
        };
        graphics.drawString(mark, x + cellSize / 4, y + (cellSize * 3) / 4);
    }

    private void drawProjectiles(Graphics graphics, int cellSize) {
        graphics.setColor(new Color(90, 245, 255));
        for (Projectile projectile : projectiles) {
            int x = projectile.position.x * cellSize + cellSize / 4;
            int y = projectile.position.y * cellSize + cellSize / 4;
            graphics.fillOval(x, y, cellSize / 2, cellSize / 2);
            graphics.setColor(Color.WHITE);
            graphics.drawOval(x, y, cellSize / 2, cellSize / 2);
            graphics.setColor(new Color(90, 245, 255));
        }
    }

    private void drawMines(Graphics graphics, int cellSize) {
        for (Mine mine : mines) {
            int x = mine.position.x * cellSize;
            int y = mine.position.y * cellSize;
            graphics.setColor(new Color(235, 70, 230));
            graphics.drawOval(x - cellSize / 2, y - cellSize / 2, cellSize * 2, cellSize * 2);
            graphics.setColor(new Color(255, 125, 240));
            graphics.fillOval(x + cellSize / 4, y + cellSize / 4, cellSize / 2, cellSize / 2);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            graphics.drawString(Integer.toString(Math.max(1, mine.warningTicks / 10 + 1)), x + cellSize / 3, y + cellSize / 2);
        }
    }

    private void drawWarnings(Graphics graphics, int cellSize) {
        if (boss == null || boss.warningTicks <= 0) return;
        if (boss.type == BossType.VOLT_SENTINEL || (boss.type == BossType.VOID_SOVEREIGN && boss.attackVariant == 0)) {
            graphics.setColor(new Color(80, 190, 255, 150));
            if (boss.horizontalAttack) {
                int y = boss.attackCoordinate * cellSize + cellSize / 2;
                graphics.drawLine(0, y, columns * cellSize, y);
                graphics.drawLine(0, y + 2, columns * cellSize, y + 2);
            } else {
                int x = boss.attackCoordinate * cellSize + cellSize / 2;
                graphics.drawLine(x, 0, x, rows * cellSize);
                graphics.drawLine(x + 2, 0, x + 2, rows * cellSize);
            }
        } else if (boss.type == BossType.CRIMSON_HUNTER) {
            graphics.setColor(new Color(255, 70, 80, 170));
            int x = boss.position.x * cellSize + cellSize / 2;
            int y = boss.position.y * cellSize + cellSize / 2;
            graphics.drawLine(x, y, x + boss.dashDx * cellSize * 4, y + boss.dashDy * cellSize * 4);
        } else if (boss.type == BossType.VOID_SOVEREIGN && boss.teleportTarget != null) {
            graphics.setColor(new Color(200, 130, 255, 190));
            int x = boss.teleportTarget.x * cellSize;
            int y = boss.teleportTarget.y * cellSize;
            graphics.drawOval(x - 3, y - 3, cellSize + 6, cellSize + 6);
            graphics.drawOval(x + 2, y + 2, cellSize - 4, cellSize - 4);
        }
    }

    /** Draws encounter status in the dedicated HUD strip above the board. */
    @Override public void drawHud(Graphics graphics, int width, int height) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.setColor(Color.WHITE);
        String bossLabel = boss == null ? "NEXT GUARDIAN" : boss.type.displayName.toUpperCase();
        graphics.drawString("BOSS RUSH  |  CYCLE " + cycle + "  |  DEFEATED " + (bossesDefeated % 4) + "/4", 12, 15);
        graphics.drawString(bossLabel, 12, 38);
        if (boss != null) {
            int barX = Math.max(185, width - 180);
            int barWidth = Math.max(100, width - barX - 12);
            graphics.setColor(new Color(55, 55, 65)); graphics.fillRoundRect(barX, 27, barWidth, 12, 7, 7);
            graphics.setColor(boss.type.color);
            int filledWidth = (int) (barWidth * (boss.health / (double) boss.maxHealth));
            graphics.fillRoundRect(barX, 27, Math.max(0, filledWidth), 12, 7, 7);
            graphics.setColor(Color.WHITE); graphics.drawRoundRect(barX, 27, barWidth, 12, 7, 7);
        }
        graphics.setColor(Color.WHITE);
        graphics.drawString("LIVES: " + "♥ ".repeat(lives) + "   ENERGY: " + "● ".repeat(energy) + "○ ".repeat(MAX_ENERGY - energy), 12, 59);
        if (invulnerabilityTicks > 0 && (tick / 3) % 2 == 0) {
            graphics.setColor(new Color(255, 235, 130)); graphics.drawString("DAMAGE SHIELD", Math.max(12, width - 130), 59);
        }
        if (boss == null && intermissionTicks > 0) {
            graphics.setColor(new Color(255, 220, 110)); graphics.drawString("GUARDIAN DEFEATED - NEXT FIGHT", Math.max(12, width - 235), 15);
        }
    }
}