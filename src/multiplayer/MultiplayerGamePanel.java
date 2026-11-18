package multiplayer;

import configuracoes.GameConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import sistemaDeMovimentacaoGeral.Direction;

/** Client-side renderer and input surface for the host-authoritative LAN match. */
public final class MultiplayerGamePanel extends JPanel {
    private record PlayerView(int id,String name,int headRgb,int bodyRgb,int outlineRgb,int score,boolean alive,List<Point> body) { }
    private final LanServer server;
    private final LanClient client;
    private final boolean host;
    private volatile String snapshot;
    private volatile boolean gameOver;
    private final Timer repaintTimer;

    public MultiplayerGamePanel(LanServer server,LanClient client,boolean host) {
        this.server=server; this.client=client; this.host=host;
        setPreferredSize(new Dimension(GameConfig.COLS*GameConfig.CELL_SIZE,GameConfig.ROWS*GameConfig.CELL_SIZE+58));
        setBackground(Color.BLACK); setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                Direction direction=switch(event.getKeyCode()) {
                    case KeyEvent.VK_UP,KeyEvent.VK_W -> Direction.UP;
                    case KeyEvent.VK_DOWN,KeyEvent.VK_S -> Direction.DOWN;
                    case KeyEvent.VK_LEFT,KeyEvent.VK_A -> Direction.LEFT;
                    case KeyEvent.VK_RIGHT,KeyEvent.VK_D -> Direction.RIGHT;
                    default -> null;
                };
                if(direction!=null) client.setDirection(direction);
                if(event.getKeyCode()==KeyEvent.VK_ESCAPE) {
                    client.close(); if(MultiplayerGamePanel.this.server!=null) MultiplayerGamePanel.this.server.close();
                    Main.mostrarMenu();
                }
            }
        });
        repaintTimer=new Timer(100,event -> repaint()); repaintTimer.start();
    }
    /** Receives a complete authoritative state line from the LAN server. */
    public void acceptSnapshot(String line) { snapshot=line; }
    public void markGameOver() { gameOver=true; }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();
        int cell=GameConfig.CELL_SIZE, width=GameConfig.COLS*cell, height=GameConfig.ROWS*cell;
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));
        g.drawString("LAN MULTIPLAYER  |  ESC: LEAVE MATCH",10,19);
        g.setColor(new Color(55,55,55));
        for(int x=0;x<=width;x+=cell) g.drawLine(x,58,x,height+58);
        for(int y=0;y<=height;y+=cell) g.drawLine(0,y+58,width,y+58);
        String state=snapshot;
        if(state!=null) drawSnapshot(g,state,cell);
        if(gameOver) {
            g.setColor(Color.YELLOW); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,28));
            g.drawString("MATCH FINISHED",width/2-115,height/2+58);
        }
        g.dispose();
    }
    private void drawSnapshot(Graphics2D g,String line,int cell) {
        String[] fields=line.split("\\|",-1);
        if(fields.length<5||!"SNAP".equals(fields[0])) return;
        int foodX=Integer.parseInt(fields[2]), foodY=Integer.parseInt(fields[3]);
        g.setColor(new Color(245,55,55)); g.fillOval(foodX*cell,foodY*cell+58,cell,cell);
        int labelX=10;
        for(int index=5;index<fields.length;index++) {
            PlayerView player=parsePlayer(fields[index]);
            if(player==null) continue;
            g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));
            g.drawString("P"+player.id+" "+player.name+": "+player.score+(player.alive?"":" (OUT)"),labelX,42);
            labelX+=145;
            boolean head=true;
            for(Point point:player.body) {
                g.setColor(new Color(head?player.headRgb:player.bodyRgb,true));
                g.fillRect(point.x*cell,point.y*cell+58,cell,cell);
                g.setColor(new Color(player.outlineRgb,true));
                g.drawRect(point.x*cell,point.y*cell+58,cell,cell);
                head=false;
            }
        }
    }
    private PlayerView parsePlayer(String field) {
        String[] parts=field.split("~",-1);
        if(parts.length!=8) return null;
        try {
            String name=new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8);
            List<Point> body=new ArrayList<>();
            if(!parts[7].isBlank()) for(String coordinate:parts[7].split(",")) {
                String[] xy=coordinate.split(":");
                body.add(new Point(Integer.parseInt(xy[0]),Integer.parseInt(xy[1])));
            }
            return new PlayerView(Integer.parseInt(parts[0]),name,Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),Integer.parseInt(parts[4]),Integer.parseInt(parts[5]),
                    "1".equals(parts[6]),body);
        } catch(RuntimeException exception) { return null; }
    }
    public void shutdown() { repaintTimer.stop(); client.close(); if(server!=null) server.close(); }
}