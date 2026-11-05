package exceptions;

/** Represents invalid game mode state or behavior. */
public class GameModeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GameModeException(String msg) {
        super(msg);
    }

    public GameModeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
