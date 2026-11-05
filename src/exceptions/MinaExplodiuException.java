package exceptions;

/** Signals that a Minefield explosion hit the snake head. */
public class MinaExplodiuException extends CampoMinadoException {
    public MinaExplodiuException() {
        super("Você atingiu uma mina!");
    }
}
