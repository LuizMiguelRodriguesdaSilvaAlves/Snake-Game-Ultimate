package exceptions;

/** Represents invalid configuration or state in Progressive Speed mode. */
public class SpeedModeException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public SpeedModeException(String message) {
        super(message);
    }

    public SpeedModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
