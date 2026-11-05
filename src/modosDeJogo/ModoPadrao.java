package modosDeJogo;

import sistemaDeMovimentacaoGeral.Snake;
import pontuacao.ScoreManager;
import java.awt.Graphics;
import java.awt.Point;

/**
 * Classic Snake rules with no additional mechanics.
 */
public class ModoPadrao implements GameMode {

    /** Stable identifier used to keep this mode score independent. */
    @Override
    public String getId() { return "normal"; }

    @Override
    public String getName() {
        return "Modo Normal";
    }

    @Override
    public void init(Snake snake, ScoreManager score) {
        // No mode-specific initialization is required.
    }

    @Override
    public void update(Snake snake) {
        // The common game loop already implements the standard rules.
    }

    @Override
    public void draw(Graphics g, int cellSize) {
        // This mode has no additional visual elements.
    }

    @Override
    public void onFoodPlaced(Point food) {
        // No action is required when regular food is placed.
    }
}
