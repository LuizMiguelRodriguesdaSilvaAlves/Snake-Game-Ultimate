package multiplayer;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import sistemaDeMovimentacaoGeral.Direction;

/**
 * Host-authoritative LAN server. It validates client input, resolves lobby
 * votes and simulates every player's snake before broadcasting snapshots.
 */
public final class LanServer implements AutoCloseable {
    public static final int MAX_PLAYERS = 4;
    public static final int DEFAULT_PORT = 47821;
    private static final int COLUMNS = 30;
    private static final int ROWS = 20;

    private static final class PlayerState {
        final int id;
        final MultiplayerProfile profile;
        final Peer peer;
        final List<Point> body = new ArrayList<>();
        Direction direction = Direction.RIGHT;
        int score;
        int combo;
        int comboTicks;
        boolean alive = true;
        PlayerState(int id, MultiplayerProfile profile, Peer peer, Point head) {
            this.id=id; this.profile=profile; this.peer=peer;
            for (int i=0;i<4;i++) body.add(new Point(head.x-i,head.y));
        }
    }
    private final class Peer implements Runnable {
        final Socket socket;
        final BufferedReader input;
        final PrintWriter output;
        volatile PlayerState player;
        Peer(Socket socket) throws IOException {
            this.socket=socket;
            input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            output=new PrintWriter(socket.getOutputStream(),true,StandardCharsets.UTF_8);
        }
        void send(String line) { synchronized(output) { output.println(line); } }
        @Override public void run() {
            try {
                String line;
                while ((line=input.readLine())!=null && !closed.get()) handle(this,line);
            } catch (IOException ignored) {
                // A dropped peer is marked inactive by the cleanup path below.
            } finally { disconnect(this); }
        }
        void close() { try { socket.close(); } catch(IOException ignored) { } }
    }

    private final int port;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final List<Peer> peers = new ArrayList<>();
    private final Map<Integer,PlayerState> players = new LinkedHashMap<>();
    private final Map<Integer,MultiplayerRuleset> votes = new LinkedHashMap<>();
    private final Random random = new Random();
    private volatile ServerSocket serverSocket;
    private volatile boolean running;
    private volatile MultiplayerRuleset selectedRuleset = MultiplayerRuleset.CLASSIC;
    private Point food = new Point(15,10);
    private int tick;
    private Thread acceptThread;
    private Thread gameThread;

    public LanServer(int port) {
        if (port < 1024 || port > 65535) throw new MultiplayerException("LAN port must be between 1024 and 65535.");
        this.port=port;
    }
    public int getPort() { return port; }
    /** Opens the TCP listener and starts accepting up to three remote players. */
    public synchronized void start() {
        if (serverSocket!=null) throw new MultiplayerException("The LAN host is already running.");
        try { serverSocket=new ServerSocket(port); }
        catch(IOException exception) { throw new MultiplayerException("Could not open LAN port "+port+".",exception); }
        acceptThread=new Thread(this::acceptLoop,"snake-lan-accept");
        acceptThread.setDaemon(true); acceptThread.start();
    }
    private void acceptLoop() {
        while(!closed.get()) {
            try {
                Socket socket=serverSocket.accept();
                socket.setTcpNoDelay(true);
                Peer peer=new Peer(socket);
                synchronized(this) {
                    if(peers.size()>=MAX_PLAYERS) { peer.send("ERROR|ROOM_FULL"); peer.close(); continue; }
                    peers.add(peer);
                }
                Thread reader=new Thread(peer,"snake-lan-peer");
                reader.setDaemon(true); reader.start();
            } catch(IOException exception) {
                if(!closed.get()) close();
            }
        }
    }
    private void handle(Peer peer,String line) {
        String[] fields=line.split("\\|",-1);
        try {
            if(fields.length==5 && "HELLO".equals(fields[0]) && peer.player==null) {
                String name=new String(Base64.getUrlDecoder().decode(fields[1]),StandardCharsets.UTF_8);
                MultiplayerProfile profile=new MultiplayerProfile(name,Integer.parseInt(fields[2]),Integer.parseInt(fields[3]),Integer.parseInt(fields[4]));
                synchronized(this) {
                    if(players.size()>=MAX_PLAYERS) { peer.send("ERROR|ROOM_FULL"); peer.close(); return; }
                    int id=players.size()+1;
                    Point[] starts={new Point(5,5),new Point(24,14),new Point(5,14),new Point(24,5)};
                    PlayerState state=new PlayerState(id,profile,peer,starts[id-1]);
                    players.put(id,state); peer.player=state;
                    peer.send("WELCOME|"+id+"|"+(id==1));
                    broadcastLobby();
                    if(running) peer.send(snapshotLine());
                }
                return;
            }
            if(peer.player==null) { peer.send("ERROR|HANDSHAKE_REQUIRED"); return; }
            if(fields.length==2 && "VOTE".equals(fields[0])) {
                synchronized(this) {
                    if(running) return;
                    votes.put(peer.player.id,MultiplayerRuleset.fromWire(fields[1]));
                    broadcastLobby();
                }
            } else if(fields.length==2 && "DIR".equals(fields[0])) {
                synchronized(this) {
                    if(!peer.player.alive) return;
                    Direction requested=Direction.valueOf(fields[1]);
                    if(!requested.isOpposite(peer.player.direction)) peer.player.direction=requested;
                }
            } else if(fields.length==1 && "START".equals(fields[0])) {
                synchronized(this) {
                    if(peer.player.id!=1 || running) return;
                    startMatch();
                }
            }
        } catch(RuntimeException exception) {
            peer.send("ERROR|INVALID_MESSAGE");
        }
    }
    /** Starts the match after majority voting; the host's vote breaks a tie. */
    public synchronized void startMatch() {
        if(running) throw new MultiplayerException("The LAN match has already started.");
        if(players.size()<2) throw new MultiplayerException("At least two players must join before starting.");
        if(!votes.containsKey(1)) throw new MultiplayerException("The host must cast a vote before starting.");
        if(votes.size()!=players.size()) throw new MultiplayerException("Every connected player must vote before the host can start the match.");
        selectedRuleset=resolveVote();
        running=true; tick=0; food=spawnFood();
        gameThread=new Thread(this::gameLoop,"snake-lan-game");
        gameThread.setDaemon(true); gameThread.start();
        broadcast("STARTED|"+selectedRuleset.name());
        broadcast(snapshotLine());
    }
    private MultiplayerRuleset resolveVote() {
        Map<MultiplayerRuleset,Integer> counts=new LinkedHashMap<>();
        for(MultiplayerRuleset mode:votes.values()) counts.merge(mode,1,Integer::sum);
        int max=counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<MultiplayerRuleset> tied=new ArrayList<>();
        counts.forEach((mode,count)->{if(count==max)tied.add(mode);});
        MultiplayerRuleset hostVote=votes.get(1);
        return tied.contains(hostVote)?hostVote:tied.get(0);
    }
    private void gameLoop() {
        while(!closed.get() && running) {
            try { Thread.sleep(selectedRuleset==MultiplayerRuleset.SPEED?85:125); }
            catch(InterruptedException exception) { Thread.currentThread().interrupt(); return; }
            synchronized(this) {
                if(!running) return;
                tick++;
                for(PlayerState player:players.values()) if(player.alive) updatePlayer(player);
                if(players.values().stream().noneMatch(player->player.alive)) running=false;
                broadcast(snapshotLine());
                if(!running) broadcast("OVER");
            }
        }
    }
    private void updatePlayer(PlayerState player) {
        Point head=player.body.get(0);
        Point next=new Point(head);
        switch(player.direction) {
            case UP -> next.y--;
            case DOWN -> next.y++;
            case LEFT -> next.x--;
            case RIGHT -> next.x++;
        }
        if(selectedRuleset==MultiplayerRuleset.WRAPAROUND) {
            next.x=(next.x+COLUMNS)%COLUMNS; next.y=(next.y+ROWS)%ROWS;
        } else if(next.x<0||next.x>=COLUMNS||next.y<0||next.y>=ROWS) { player.alive=false; return; }
        if(player.body.size()>1 && player.body.subList(0,player.body.size()-1).contains(next)) { player.alive=false; return; }
        for(PlayerState other:players.values()) {
            if(other.body.contains(next)) { player.alive=false; return; }
        }
        player.body.add(0,next);
        if(next.equals(food)) {
            player.score++;
            if(selectedRuleset==MultiplayerRuleset.COMBO) {
                player.combo=player.comboTicks>0?player.combo+1:1;
                player.comboTicks=35;
                if(player.combo>1) player.score+=Math.min(10,(player.combo-1)*2);
            }
            food=spawnFood();
        } else player.body.remove(player.body.size()-1);
        if(player.comboTicks>0 && --player.comboTicks==0) player.combo=0;
    }
    private Point spawnFood() {
        for(int attempt=0;attempt<600;attempt++) {
            Point candidate=new Point(random.nextInt(COLUMNS),random.nextInt(ROWS));
            if(players.values().stream().noneMatch(p->p.body.contains(candidate))) return candidate;
        }
        throw new MultiplayerException("No free cell remains for multiplayer food.");
    }
    private String snapshotLine() {
        StringBuilder line=new StringBuilder("SNAP|").append(selectedRuleset.name()).append('|')
                .append(food.x).append('|').append(food.y).append('|').append(tick);
        for(PlayerState player:players.values()) {
            line.append('|').append(player.id).append('~')
                .append(Base64.getUrlEncoder().withoutPadding().encodeToString(player.profile.name().getBytes(StandardCharsets.UTF_8))).append('~')
                .append(player.profile.headRgb()).append('~').append(player.profile.bodyRgb()).append('~')
                .append(player.profile.outlineRgb()).append('~').append(player.score).append('~')
                .append(player.alive?1:0).append('~');
            for(int i=0;i<player.body.size();i++) {
                Point point=player.body.get(i); if(i>0) line.append(',');
                line.append(point.x).append(':').append(point.y);
            }
        }
        return line.toString();
    }
    private void broadcastLobby() {
        StringBuilder line=new StringBuilder("LOBBY|").append(players.size()).append('|');
        for(Map.Entry<Integer,MultiplayerRuleset> entry:votes.entrySet()) line.append(entry.getKey()).append(':').append(entry.getValue().name()).append(',');
        broadcast(line.toString());
    }
    private void broadcast(String line) {
        for(Peer peer:new ArrayList<>(peers)) if(peer.player!=null) peer.send(line);
    }
    private synchronized void disconnect(Peer peer) {
        peers.remove(peer);
        if(peer.player!=null) {
            players.remove(peer.player.id); votes.remove(peer.player.id);
            if(peer.player.id==1) running=false;
            broadcastLobby();
        }
        peer.close();
    }
    /** Returns a LAN address suitable for sharing with other computers. */
    public static String findLocalAddress() {
        try(var socket=new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("192.0.2.1"),80);
            return socket.getLocalAddress().getHostAddress();
        } catch(IOException exception) { return "127.0.0.1"; }
    }
    @Override public synchronized void close() {
        if(!closed.compareAndSet(false,true)) return;
        running=false;
        if(serverSocket!=null) try { serverSocket.close(); } catch(IOException ignored) { }
        for(Peer peer:new ArrayList<>(peers)) peer.close();
        peers.clear(); players.clear(); votes.clear();
    }
}