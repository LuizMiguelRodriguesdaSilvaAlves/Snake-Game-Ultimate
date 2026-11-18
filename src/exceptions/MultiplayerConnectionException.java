package exceptions;

/** Reports LAN socket creation, connection and transport failures. */
public final class MultiplayerConnectionException extends multiplayer.MultiplayerException {
    public MultiplayerConnectionException(String message) { super(message); }
    public MultiplayerConnectionException(String message, Throwable cause) { super(message, cause); }
}