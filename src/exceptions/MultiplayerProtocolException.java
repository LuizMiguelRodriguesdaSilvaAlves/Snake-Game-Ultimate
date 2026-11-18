package exceptions;

/** Reports malformed, unsupported or invalid LAN protocol messages. */
public final class MultiplayerProtocolException extends multiplayer.MultiplayerException {
    public MultiplayerProtocolException(String message) { super(message); }
    public MultiplayerProtocolException(String message, Throwable cause) { super(message, cause); }
}