package exceptions;

/** Represents invalid configuration or state in No Walls mode. */
public class NoWallsException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public NoWallsException(String message) {
        super(message);
    }

    public NoWallsException(String message, Throwable cause) {
        super(message, cause);
    }
}
