package exceptions;

/** Represents errors while changing application screens. */
public class MenuNavigationException extends RuntimeException {
    public MenuNavigationException(String msg) {
        super(msg);
    }
}
