package exceptions;

/** Represents invalid configuration or state in Time Attack mode. */
public class TimeAttackException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public TimeAttackException(String message) {
        super(message);
    }

    public TimeAttackException(String message, Throwable cause) {
        super(message, cause);
    }
}
