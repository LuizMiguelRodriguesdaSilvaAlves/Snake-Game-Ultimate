package exceptions;

/** Represents an invalid application or gameplay state. */
public class StateValidationException extends RuntimeException {
    public StateValidationException(String message) {
        super(message);
    }
}
