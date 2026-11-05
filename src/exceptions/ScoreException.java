package exceptions;

/**
 * Thrown when score values fail validation.
 * For example, attempting to add a negative number of points.
 */
public class ScoreException extends RuntimeException {
    public ScoreException(String message) {
        super(message);
    }

    public ScoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
