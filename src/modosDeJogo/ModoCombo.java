package modosDeJogo;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import pontuacao.ScoreManager;
import exceptions.ComboModeException;
import sistemaDeMovimentacaoGeral.Snake;

/** Rewards quick consecutive fruit collections with an escalating score bonus. */
public final class ModoCombo implements GameMode {
    private static final int COMBO_WINDOW_TICKS = 35;
    private static final int MAX_BONUS = 10;
    private int combo;
    private int ticksRemaining;
    @Override public String getId() { return "combo"; }
    @Override public String getName() { return "Combo"; }
    /** Resets the combo chain for a new round. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null || score == null) throw new ComboModeException("Combo mode requires a player and score manager.");
        combo = 0; ticksRemaining = 0;
    }
    /** Counts down the combo window and clears an expired chain. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null || context.getScore() == null) {
            throw new ComboModeException("Combo mode requires a valid game context and score manager.");
        }
        if (ticksRemaining > 0 && --ticksRemaining == 0) combo = 0;
    }
    /** Awards a capped increasing bonus after the first consecutive collection. */
    @Override public void onFoodCollected(GameModeContext context) {
        if (context == null || context.getScore() == null) throw new ComboModeException("Combo mode cannot award a food bonus without a score manager.");
        combo++; ticksRemaining = COMBO_WINDOW_TICKS;
        if (combo >= 2) context.getScore().addPoints(Math.min(MAX_BONUS, (combo - 1) * 2));
    }
    /** Keeps the board clear; status appears in the dedicated HUD. */
    @Override public void draw(Graphics graphics, int cellSize) { }
    /** Displays the chain, remaining window and next bonus. */
    @Override public void drawHud(Graphics graphics, int width, int height) {
        graphics.setColor(new Color(255, 220, 90)); graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        int nextBonus = combo < 1 ? 0 : Math.min(MAX_BONUS, combo * 2);
        graphics.drawString("COMBO x" + Math.max(1, combo), 12, 18);
        graphics.drawString("NEXT BONUS: +" + nextBonus, 150, 18);
        graphics.drawString("CHAIN WINDOW: " + ticksRemaining + " TICKS", 310, 18);
    }
    /** Exposes chain state for regression tests. */
    public int getCombo() { return combo; }
    public int getTicksRemaining() { return ticksRemaining; }
}
