package modosDeJogo;

import pontuacao.ScoreManager;
import exceptions.SpeedModeException;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode where every score milestone increases the game speed. */
public final class ModoVelocidade implements GameMode {
    @Override public String getId() { return "speed"; }
    @Override public String getName() { return "Velocidade Progressiva"; }
    /** This mode has no additional state to initialize. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new SpeedModeException("Progressive Speed cannot initialize without a player snake.");
    }
    /** Reduces the tick delay as the player's score grows, with a safe lower bound. */
    @Override public int getTickDelay(int score) {
        if (score < 0) throw new SpeedModeException("Progressive Speed requires a non-negative score.");
        return Math.max(42, 100 - (score / 5) * 4);
    }
}
