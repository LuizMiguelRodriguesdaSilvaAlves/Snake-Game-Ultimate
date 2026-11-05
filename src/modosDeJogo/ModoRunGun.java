package modosDeJogo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.RunAndGunException;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Secret arcade shooter mode. The player fires toward hostile fruit, while the
 * fruit AI fires back. Bullets from opposing sides cancel when they collide.
 */
public final class ModoRunGun implements GameMode {
    private static final class Projectile {
        int x, y, dx, dy, previousX, previousY;
        final boolean playerOwned;
        Projectile(int x, int y, int dx, int dy, boolean playerOwned) {
            this.x=x; this.y=y; this.dx=dx; this.dy=dy; this.playerOwned=playerOwned;
            this.previousX=x; this.previousY=y;
        }
    }
    private static final class FruitEnemy {
        Point position; int fireCooldown; int moveCooldown;
        FruitEnemy(Point position, int cooldown) { this.position=position; this.fireCooldown=cooldown; this.moveCooldown=8; }
    }
    private final Random random = new Random();
    private final List<Projectile> bullets = new ArrayList<>();
    private final List<FruitEnemy> enemies = new ArrayList<>();
    private Snake player;
    private int tick;

    @Override public String getId() { return "run-and-gun"; }
    @Override public String getName() { return "Run & Gun"; }
    /** Creates three fruit opponents and clears all projectiles for a new round. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new RunAndGunException("Run & Gun cannot initialize without a player snake.");
        player = snake; tick = 0; bullets.clear(); enemies.clear();
        for (int i=0; i<3; i++) enemies.add(new FruitEnemy(randomFreeCell(30,20), 12 + random.nextInt(20)));
    }
    /** This mode replaces collectible fruit with hostile fruit opponents. */
    @Override public boolean usesFood() { return false; }
    /** Fires one player projectile when Space is pressed. */
    @Override public void onKeyPressed(int keyCode) {
        if (keyCode != KeyEvent.VK_SPACE) return;
        if (player == null) throw new RunAndGunException("Run & Gun received fire input before initialization.");
        Point head = player.getHead(); Direction d = player.getDirection();
        if (head == null || d == null) throw new RunAndGunException("The player position and direction must be initialized before firing.");
        int dx=0, dy=0;
        switch (d) { case UP -> dy=-1; case DOWN -> dy=1; case LEFT -> dx=-1; case RIGHT -> dx=1; }
        bullets.add(new Projectile(head.x+dx, head.y+dy, dx, dy, true));
    }
    /** Updates fruit firing AI, projectile movement, projectile cancellation and hits. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null
                || context.getColumns() <= 0 || context.getRows() <= 0) {
            throw new RunAndGunException("Run & Gun requires a valid game context, player, score and positive board dimensions.");
        }
        tick++; Point playerHead = context.getSnake().getHead();
        for (FruitEnemy enemy : enemies) {
            if (--enemy.moveCooldown <= 0) {
                evadePlayer(enemy, playerHead, context.getColumns(), context.getRows());
                enemy.moveCooldown = 7 + random.nextInt(8);
            }
            if (--enemy.fireCooldown <= 0) {
                int[] aim = aimCardinal(enemy.position, playerHead);
                bullets.add(new Projectile(enemy.position.x+aim[0], enemy.position.y+aim[1], aim[0], aim[1], false));
                enemy.fireCooldown = 24 + random.nextInt(18);
            }
        }
        if (tick % 2 != 0) return;
        for (Projectile bullet : bullets) {
            bullet.previousX = bullet.x; bullet.previousY = bullet.y;
            bullet.x += bullet.dx; bullet.y += bullet.dy;
        }
        boolean[] removed = new boolean[bullets.size()];
        for (int i=0; i<bullets.size(); i++) {
            if (removed[i]) continue;
            Projectile a=bullets.get(i);
            for (int j=i+1; j<bullets.size(); j++) {
                if (removed[j]) continue; Projectile b=bullets.get(j);
                boolean sameCell = a.x == b.x && a.y == b.y;
                boolean crossedPaths = a.x == b.previousX && a.y == b.previousY
                        && b.x == a.previousX && b.y == a.previousY;
                if (sameCell || crossedPaths) {
                    removed[i] = true;
                    removed[j] = true;
                    break;
                }
            }
        }
        for (int i=0; i<bullets.size(); i++) {
            if (removed[i]) continue; Projectile bullet=bullets.get(i);
            if (bullet.x<0 || bullet.x>=context.getColumns() || bullet.y<0 || bullet.y>=context.getRows()) { removed[i]=true; continue; }
            if (bullet.playerOwned) {
                for (FruitEnemy enemy : enemies) if (enemy.position.x==bullet.x && enemy.position.y==bullet.y) {
                    enemy.position = randomFreeCell(context.getColumns(), context.getRows());
                    enemy.fireCooldown = 18 + random.nextInt(20);
                    context.getScore().addPoints(10); removed[i]=true; break;
                }
            } else if (context.getSnake().getBody().contains(new Point(bullet.x, bullet.y))) {
                context.requestGameOver(); removed[i]=true;
            }
        }
        for (int i=removed.length-1; i>=0; i--) if (removed[i]) bullets.remove(i);
    }
    /** Draws enemy fruit and both projectile teams with different colors. */
    @Override public void draw(Graphics g, int cellSize) {
        for (FruitEnemy enemy : enemies) {
            int x=enemy.position.x*cellSize, y=enemy.position.y*cellSize;
            g.setColor(new Color(240,90,55)); g.fillOval(x+1,y+1,cellSize-2,cellSize-2);
            g.setColor(Color.WHITE); g.drawOval(x+1,y+1,cellSize-2,cellSize-2);
        }
        for (Projectile bullet : bullets) {
            g.setColor(bullet.playerOwned ? Color.CYAN : Color.MAGENTA);
            int x=bullet.x*cellSize+cellSize/4, y=bullet.y*cellSize+cellSize/4;
            g.fillOval(x,y,cellSize/2,cellSize/2);
        }
        g.setColor(Color.WHITE); g.drawString("ESPAÇO: ATIRAR | Alvos: " + enemies.size(), 10, 80);
    }
    /** Selects a random unoccupied cell for a fruit opponent. */
    private Point randomFreeCell(int cols, int rows) {
        for (int attempt=0; attempt<500; attempt++) {
            Point p=new Point(random.nextInt(cols), random.nextInt(rows));
            boolean playerOccupied = player != null && player.getBody().contains(p);
            boolean enemyOccupied = enemies.stream().anyMatch(enemy -> enemy.position.equals(p));
            if (!playerOccupied && !enemyOccupied) return p;
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Point candidate = new Point(x, y);
                boolean playerOccupied = player != null && player.getBody().contains(candidate);
                boolean enemyOccupied = enemies.stream().anyMatch(enemy -> enemy.position.equals(candidate));
                if (!playerOccupied && !enemyOccupied) return candidate;
            }
        }
        throw new RunAndGunException("No unoccupied board cell is available for a fruit opponent.");
    }
    /** Moves a fruit opponent away from the player when a safe evasive cell exists. */
    private void evadePlayer(FruitEnemy enemy, Point target, int columns, int rows) {
        int[][] steps = {{1,0},{-1,0},{0,1},{0,-1}};
        int bestDistance = (int) enemy.position.distance(target);
        Point best = enemy.position;
        for (int[] step : steps) {
            Point candidate = new Point(enemy.position.x + step[0], enemy.position.y + step[1]);
            if (candidate.x < 1 || candidate.x >= columns - 1 || candidate.y < 1 || candidate.y >= rows - 1) continue;
            if (player.getBody().contains(candidate)) continue;
            boolean occupied = enemies.stream().anyMatch(other -> other != enemy && other.position.equals(candidate));
            if (occupied) continue;
            int distance = (int) candidate.distance(target);
            if (distance > bestDistance) { bestDistance = distance; best = candidate; }
        }
        enemy.position = new Point(best);
    }

    /** Chooses the dominant cardinal axis to aim an enemy shot at the player. */
    private int[] aimCardinal(Point from, Point target) {
        int dx=target.x-from.x, dy=target.y-from.y;
        if (Math.abs(dx)>Math.abs(dy)) return new int[]{Integer.signum(dx),0};
        return new int[]{0,Integer.signum(dy)};
    }
}