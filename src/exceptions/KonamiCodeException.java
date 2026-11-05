package exceptions;

/** Represents invalid Konami secret-screen navigation state. */
public class KonamiCodeException extends RuntimeException {

    public KonamiCodeException(String msg) {
        super(msg);
    }

    public KonamiCodeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}