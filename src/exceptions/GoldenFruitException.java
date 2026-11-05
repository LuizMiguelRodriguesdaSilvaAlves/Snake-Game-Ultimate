package exceptions;

/** Represents invalid configuration or state in Golden Fruit mode. */
public class GoldenFruitException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public GoldenFruitException(String message) {
        super(message);
    }

    public GoldenFruitException(String message, Throwable cause) {
        super(message, cause);
    }
}
