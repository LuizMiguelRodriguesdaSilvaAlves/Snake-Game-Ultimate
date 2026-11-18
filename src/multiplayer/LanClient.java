package multiplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import sistemaDeMovimentacaoGeral.Direction;
import exceptions.MultiplayerConnectionException;
import exceptions.MultiplayerProtocolException;

/** TCP client for LAN lobby voting, directional input and authoritative snapshots. */
public final class LanClient implements AutoCloseable {
    public interface Listener {
        default void onWelcome(int playerId, boolean host) { }
        default void onLobby(String lobbyState) { }
        default void onStarted(String ruleset) { }
        default void onSnapshot(String snapshot) { }
        default void onGameOver() { }
        default void onError(String message) { }
        default void onDisconnected() { }
    }

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Listener listener;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private volatile int playerId;
    private volatile boolean host;

    public LanClient(Listener listener) {
        if (listener == null) throw new MultiplayerException("A LAN client listener is required.");
        this.listener = listener;
    }

    /** Connects to the host and sends this computer's local snake colors. */
    public void connect(String address, int port, MultiplayerProfile profile) {
        if (socket != null) throw new MultiplayerException("This LAN client is already connected.");
        if (address == null || address.isBlank() || profile == null) {
            throw new MultiplayerException("A host address and player profile are required.");
        }
        try {
            socket = new Socket(address.trim(), port);
            socket.setTcpNoDelay(true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            String encodedName = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(profile.name().getBytes(StandardCharsets.UTF_8));
            send("HELLO|" + encodedName + "|" + profile.headRgb() + "|"
                    + profile.bodyRgb() + "|" + profile.outlineRgb());
            Thread reader = new Thread(this::readLoop, "snake-lan-client");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException exception) {
            close();
            throw new MultiplayerConnectionException("Could not connect to " + address + ":" + port
                    + ". Check the host IP and local network.", exception);
        }
    }

    /** Reads validated server messages and forwards them to the active Swing screen. */
    private void readLoop() {
        try {
            String line;
            while (!closed.get() && (line = input.readLine()) != null) {
                String[] fields = line.split("\\|", -1);
                switch (fields[0]) {
                    case "WELCOME" -> {
                        if (fields.length != 3) throw new MultiplayerProtocolException("Malformed welcome packet.");
                        playerId = Integer.parseInt(fields[1]);
                        host = Boolean.parseBoolean(fields[2]);
                        listener.onWelcome(playerId, host);
                    }
                    case "LOBBY" -> listener.onLobby(line);
                    case "STARTED" -> {
                        if (fields.length != 2) throw new MultiplayerProtocolException("Malformed match-start packet.");
                        listener.onStarted(fields[1]);
                    }
                    case "SNAP" -> listener.onSnapshot(line);
                    case "OVER" -> listener.onGameOver();
                    case "ERROR" -> listener.onError(fields.length > 1 ? fields[1] : "UNKNOWN");
                    default -> listener.onError("UNKNOWN_SERVER_MESSAGE");
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (!closed.get()) listener.onError("Connection lost: " + exception.getMessage());
        } finally {
            if (!closed.get()) listener.onDisconnected();
            close();
        }
    }

    public int getPlayerId() { return playerId; }
    public boolean isHost() { return host; }

    /** Submits one vote for a supported multiplayer ruleset. */
    public void vote(MultiplayerRuleset ruleset) {
        if (ruleset == null) throw new MultiplayerException("A multiplayer vote is required.");
        send("VOTE|" + ruleset.name());
    }

    /** Sends a requested direction; the server validates reverse turns. */
    public void setDirection(Direction direction) {
        if (direction == null) throw new MultiplayerException("A movement direction is required.");
        send("DIR|" + direction.name());
    }

    /** Requests match start; the server accepts this command from the host only. */
    public void startMatch() { send("START"); }

    private synchronized void send(String message) {
        if (closed.get() || output == null) throw new MultiplayerException("The LAN connection is not active.");
        output.println(message);
        if (output.checkError()) throw new MultiplayerConnectionException("Could not send data to the LAN host.");
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }
}