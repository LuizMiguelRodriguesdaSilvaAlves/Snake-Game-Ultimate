package exceptions;

/** Represents a request for a theme that is not registered. */
public class TemaNaoEncontradoException extends RuntimeException {

    public TemaNaoEncontradoException(String nome) {
        super("Tema visual não encontrado: " + nome);
    }
}
