package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import pontuacao.ScoreManager;
import exceptions.TimeAttackException;
import sistemaDeMovimentacaoGeral.Snake;

/** Secret 90-second challenge: collect as many regular fruits as possible before time expires. */
public final class ModoContrarrelogio implements GameMode {
    private static final int ROUND_TICKS = 900;
    private int remainingTicks;
    @Override public String getId() { return "time-attack"; }
    @Override public String getName() { return "Contra o Relógio"; }
    /** Resets the 90-second countdown. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new TimeAttackException("Time Attack cannot initialize without a player snake.");
        remainingTicks=ROUND_TICKS;
    }
    /** Advances the countdown and ends the round at zero. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || remainingTicks < 0 || remainingTicks > ROUND_TICKS) {
            throw new TimeAttackException("Time Attack received an invalid round context or countdown state.");
        }
        if (remainingTicks > 0) remainingTicks--;
        if (remainingTicks <= 0) context.requestGameOver();
    }
    /** Displays the remaining time in seconds. */
    @Override public void draw(Graphics g, int cellSize) {
        g.setColor(new Color(255,220,100)); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));
        g.drawString(String.format("TEMPO: %02d:%02d", remainingTicks/600, (remainingTicks%600)/10), 430, 20);
    }
}
