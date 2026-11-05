package exceptions;

/** Represents invalid configuration or state in Mirrored Controls mode. */
public class MirroredControlsException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public MirroredControlsException(String message) {
        super(message);
    }

    public MirroredControlsException(String message, Throwable cause) {
        super(message, cause);
    }
}
