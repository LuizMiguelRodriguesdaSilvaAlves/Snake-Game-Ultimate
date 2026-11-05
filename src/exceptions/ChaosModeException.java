package exceptions;

/** Represents invalid configuration or state in Chaos mode. */
public class ChaosModeException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public ChaosModeException(String message) {
        super(message);
    }

    public ChaosModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
