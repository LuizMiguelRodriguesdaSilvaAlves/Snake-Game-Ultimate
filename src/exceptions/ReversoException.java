package exceptions;

/** Represents invalid state or update failures in Reverse mode. */
public class ReversoException extends RuntimeException {

    public ReversoException(String msg) {
        super(msg);
    }

    public ReversoException(String msg, Throwable cause) {
        super(msg, cause);
    }
}