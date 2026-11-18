package exceptions;

/** Reports invalid lobby state, voting or match-start requests. */
public final class MultiplayerLobbyException extends MultiplayerException {
    public MultiplayerLobbyException(String message) { super(message); }
}