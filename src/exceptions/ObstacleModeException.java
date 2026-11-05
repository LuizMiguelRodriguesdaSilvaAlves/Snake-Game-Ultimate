package exceptions;

/** Represents invalid configuration or state in Obstacle mode. */
public class ObstacleModeException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public ObstacleModeException(String message) {
        super(message);
    }

    public ObstacleModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
