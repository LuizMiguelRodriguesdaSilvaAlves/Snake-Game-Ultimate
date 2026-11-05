package modosDeJogo;

import pontuacao.ScoreManager;
import exceptions.NoWallsException;
import sistemaDeMovimentacaoGeral.Snake;

/** Standard mode where leaving one edge makes the snake re-enter on the opposite edge. */
public final class ModoSemParedes implements GameMode {
    @Override public String getId() { return "wraparound"; }
    @Override public String getName() { return "Sem Paredes"; }
    /** No additional state is required, but the player is part of the mode lifecycle contract. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new NoWallsException("No Walls cannot initialize without a player snake.");
    }
    /** Enables wrap-around movement in the common game loop. */
    @Override public boolean wrapsAtEdges() { return true; }
}
