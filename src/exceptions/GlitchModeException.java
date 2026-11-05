package exceptions;

/** Represents invalid configuration or state in Glitch mode. */
public class GlitchModeException extends GameModeException {
    private static final long serialVersionUID = 1L;

    public GlitchModeException(String message) {
        super(message);
    }

    public GlitchModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
