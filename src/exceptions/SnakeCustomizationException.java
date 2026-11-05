package exceptions;

/** Represents failures while applying a snake customization. */
public class SnakeCustomizationException extends RuntimeException {

    public SnakeCustomizationException(String message) {
        super(message);
    }

    public SnakeCustomizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
