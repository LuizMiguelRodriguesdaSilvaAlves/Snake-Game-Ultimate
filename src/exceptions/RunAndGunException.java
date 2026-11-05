package exceptions;

/** Represents invalid configuration or state in Run & Gun mode. */
public class RunAndGunException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public RunAndGunException(String message) {
        super(message);
    }

    public RunAndGunException(String message, Throwable cause) {
        super(message, cause);
    }
}
