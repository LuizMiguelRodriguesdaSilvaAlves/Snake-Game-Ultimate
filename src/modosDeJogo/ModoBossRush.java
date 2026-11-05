package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.BossRushException;
import sistemaDeMovimentacaoGeral.Snake;

/** Secret survival challenge featuring boss cores that slowly hunt the player. */
public final class ModoBossRush implements GameMode {
    private final Random random = new Random();
    private final List<Point> bosses = new ArrayList<>();
    private int movementCounter;
    private ScoreManager score;
    @Override public String getId() { return "boss-rush"; }
    @Override public String getName() { return "Boss Rush"; }
    /** Starts the first boss away from the player's spawn position. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || score == null) {
            throw new BossRushException("Boss Rush requires an initialized player snake and score manager.");
        }
        this.score=score; bosses.clear(); bosses.add(randomCorner()); movementCounter=0;
    }
    /** Moves bosses toward the player and ends the round if a boss touches any body segment. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || score == null
                || context.getColumns() < 30 || context.getRows() < 20) {
            throw new BossRushException("Boss Rush requires an initialized round and a board at least 30 columns by 20 rows.");
        }
        movementCounter++; if (movementCounter % 5 != 0) return;
        Point target=context.getSnake().getHead();
        for (Point boss : bosses) {
            if (boss.equals(target) || context.getSnake().getBody().contains(boss)) { context.requestGameOver(); return; }
            int dx=Integer.compare(target.x,boss.x), dy=Integer.compare(target.y,boss.y);
            if (Math.abs(target.x-boss.x) >= Math.abs(target.y-boss.y)) boss.translate(dx,0); else boss.translate(0,dy);
            if (boss.equals(target) || context.getSnake().getBody().contains(boss)) { context.requestGameOver(); return; }
        }
        if (score.getCurrentScore() >= bosses.size()*20 && bosses.size()<4) bosses.add(randomCorner());
    }
    /** Draws each boss as a large crimson core. */
    @Override public void draw(Graphics g, int cellSize) {
        for (Point boss : bosses) {
            int x=boss.x*cellSize, y=boss.y*cellSize;
            g.setColor(new Color(140,20,35)); g.fillOval(x-2,y-2,cellSize+4,cellSize+4);
            g.setColor(Color.WHITE); g.drawOval(x-2,y-2,cellSize+4,cellSize+4);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12)); g.drawString("B",x+6,y+14);
        }
        g.setColor(Color.WHITE); g.drawString("BOSSES: " + bosses.size(), 10, 80);
    }
    /** Chooses one of the four inner board corners. */
    private Point randomCorner() {
        int[][] corners={{2,2},{27,2},{2,17},{27,17}}; int[] c=corners[random.nextInt(corners.length)];
        return new Point(c[0],c[1]);
    }
}
