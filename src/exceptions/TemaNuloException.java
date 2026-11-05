package exceptions;

/** Represents a missing theme where a valid theme is required. */
public class TemaNuloException extends RuntimeException {

    public TemaNuloException() {
        super("Tentativa de aplicar um tema visual nulo.");
    }
}
