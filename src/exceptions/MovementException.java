package exceptions;

/** Represents invalid snake movement or length operations. */
public class MovementException extends RuntimeException {
    public MovementException(String message) {
        super(message);
    }
}
