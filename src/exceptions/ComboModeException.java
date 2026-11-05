package exceptions;

/** Represents invalid configuration or state in Combo mode. */
public class ComboModeException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public ComboModeException(String message) {
        super(message);
    }

    public ComboModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
