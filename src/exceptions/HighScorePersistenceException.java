package exceptions;

/**
 * Thrown when a high score cannot be loaded or saved due to I/O, permission or format errors.
 */
public class HighScorePersistenceException extends RuntimeException {
    public HighScorePersistenceException(String message) {
        super(message);
    }

    public HighScorePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
