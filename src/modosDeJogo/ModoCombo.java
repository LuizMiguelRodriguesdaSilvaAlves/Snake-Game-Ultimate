package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import pontuacao.ScoreManager;
import exceptions.ComboModeException;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode that rewards consecutive fruit collections with combo bonuses. */
public final class ModoCombo implements GameMode {
    private int combo;
    private int ticksSinceFood;
    @Override public String getId() { return "combo"; }
    @Override public String getName() { return "Combo"; }
    /** Resets the combo state for a new round. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new ComboModeException("Combo mode cannot initialize without a player snake.");
        combo = 0; ticksSinceFood = 0;
    }
    /** Breaks the combo after 25 ticks without collecting food. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null) {
            throw new ComboModeException("Combo mode requires a valid game context and score manager.");
        }
        if (++ticksSinceFood > 25) combo = 0;
    }
    /** Adds an increasing bonus for each consecutive collection. */
    @Override public void onFoodCollected(GameModeContext context) {
        if (context == null || context.getScore() == null) {
            throw new ComboModeException("Combo mode cannot award a food bonus without a score manager.");
        }
        ticksSinceFood = 0;
        combo++;
        if (combo >= 2) context.getScore().addPoints(Math.min(5, combo - 1));
    }
    /** Displays the current combo multiplier. */
    @Override public void draw(Graphics g, int cellSize) {
        g.setColor(new Color(255, 220, 90));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("COMBO x" + Math.max(1, combo), 430, 20);
    }
}
