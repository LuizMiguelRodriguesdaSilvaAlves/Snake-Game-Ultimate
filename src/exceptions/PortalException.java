package exceptions;

/** Represents invalid state or relocation failures in Portal mode. */
public class PortalException extends RuntimeException {

    public PortalException(String msg) {
        super(msg);
    }

    public PortalException(String msg, Throwable cause) {
        super(msg, cause);
    }
}