package exceptions;

/** Represents failures during game round initialization. */
public class GameInitializationException extends RuntimeException {
    public GameInitializationException(String message) {
        super(message);
    }
}
