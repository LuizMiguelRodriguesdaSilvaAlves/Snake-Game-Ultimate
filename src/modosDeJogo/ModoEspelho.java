package modosDeJogo;

import pontuacao.ScoreManager;
import exceptions.MirroredControlsException;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode that mirrors all directional input on both axes. */
public final class ModoEspelho implements GameMode {
    @Override public String getId() { return "mirror-controls"; }
    @Override public String getName() { return "Controles Espelhados"; }
    /** No mode-specific state is needed. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new MirroredControlsException("Mirrored Controls cannot initialize without a player snake.");
    }
    /** Converts each requested direction to its opposite direction. */
    @Override public Direction adjustDirection(Direction direction) {
        if (direction == null) throw new MirroredControlsException("Mirrored Controls cannot transform a null direction.");
        return switch (direction) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }
}
