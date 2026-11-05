package exceptions;

/** Represents invalid configuration or state in Boss Rush mode. */
public class BossRushException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public BossRushException(String message) {
        super(message);
    }

    public BossRushException(String message, Throwable cause) {
        super(message, cause);
    }
}
