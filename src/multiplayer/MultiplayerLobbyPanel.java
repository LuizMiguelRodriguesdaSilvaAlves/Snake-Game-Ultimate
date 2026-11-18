package multiplayer;

import exceptions.MultiplayerException;

import tela.Main;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Base64;

/** Lobby screen for LAN hosting, joining, majority voting and host-controlled start. */
public final class MultiplayerLobbyPanel extends JPanel {
    private final LanServer server;
    private final LanClient client;
    private final boolean host;
    private final JLabel status=new JLabel("Connecting to LAN host...",SwingConstants.CENTER);
    private final JLabel voteSummary=new JLabel("Waiting for players...",SwingConstants.CENTER);
    private final JLabel connectionInfo=new JLabel("",SwingConstants.CENTER);
    private final JComboBox<MultiplayerRuleset> voteBox=new JComboBox<>(MultiplayerRuleset.values());
    private final JButton voteButton=new JButton("Submit vote");
    private final JButton startButton=new JButton("Start match");
    private final String address;
    private final int port;
    private final MultiplayerProfile profile;
    private MultiplayerGamePanel gamePanel;

    public MultiplayerLobbyPanel(LanServer server,String address,int port,MultiplayerProfile profile,boolean host) {
        this.server=server; this.address=address; this.port=port; this.profile=profile; this.host=host;
        setLayout(new BorderLayout(12,12)); setBackground(Color.BLACK);
        JLabel title=new JLabel("MULTIPLAYER LAN LOBBY",SwingConstants.CENTER);
        title.setForeground(Color.WHITE); title.setFont(new Font(Font.SANS_SERIF,Font.BOLD,24));
        status.setForeground(Color.WHITE); voteSummary.setForeground(new Color(190,210,255));
        connectionInfo.setForeground(new Color(160,220,160));
        if(host) connectionInfo.setText("Share LAN address: "+LanServer.findLocalAddress()+":"+port);
        else connectionInfo.setText("Connected to host: "+address+":"+port);
        JPanel center=new JPanel(new GridLayout(0,1,10,10)); center.setBackground(Color.BLACK);
        center.setBorder(BorderFactory.createEmptyBorder(24,40,24,40));
        center.add(new JLabel("Choose the mode you want to play:",SwingConstants.CENTER));
        center.add(connectionInfo); center.add(voteBox); center.add(voteButton); center.add(voteSummary); center.add(status);
        voteBox.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));
        voteButton.addActionListener(event -> submitVote());
        startButton.setEnabled(host); startButton.addActionListener(event -> startMatch());
        JButton back=new JButton("Leave lobby");
        back.addActionListener(event -> leaveLobby());
        JPanel footer=new JPanel(); footer.setBackground(Color.BLACK); footer.add(back); footer.add(startButton);
        add(title,BorderLayout.NORTH); add(center,BorderLayout.CENTER); add(footer,BorderLayout.SOUTH);
        client=new LanClient(new LanClient.Listener() {
            @Override public void onWelcome(int id,boolean isHost) {
                SwingUtilities.invokeLater(() -> status.setText("Connected as Player "+id+(isHost?" (Host)":"")));
                if(isHost) SwingUtilities.invokeLater(() -> voteButton.doClick());
            }
            @Override public void onLobby(String state) { SwingUtilities.invokeLater(() -> updateLobby(state)); }
            @Override public void onStarted(String ruleset) {
                SwingUtilities.invokeLater(() -> {
                    gamePanel = new MultiplayerGamePanel(server, client, host);
                    Main.mostrarPartidaMultiplayer(gamePanel);
                });
            }
            @Override public void onSnapshot(String snapshot) {
                MultiplayerGamePanel current = gamePanel;
                if (current != null) current.acceptSnapshot(snapshot);
            }
            @Override public void onGameOver() {
                MultiplayerGamePanel current = gamePanel;
                if (current != null) SwingUtilities.invokeLater(current::markGameOver);
            }
            @Override public void onError(String message) { SwingUtilities.invokeLater(() -> status.setText("Network: "+message)); }
            @Override public void onDisconnected() { SwingUtilities.invokeLater(() -> status.setText("Disconnected from host.")); }
        });
        try { client.connect(address,port,profile); }
        catch(MultiplayerException exception) {
            if(server!=null) server.close();
            throw exception;
        }
    }
    private void submitVote() {
        try {
            client.vote((MultiplayerRuleset)voteBox.getSelectedItem());
            status.setText("Vote submitted. You can change it until the host starts.");
        } catch(RuntimeException exception) { status.setText(exception.getMessage()); }
    }
    private void startMatch() {
        try { client.startMatch(); }
        catch(RuntimeException exception) { status.setText(exception.getMessage()); }
    }
    private void updateLobby(String state) {
        String[] fields=state.split("\\|",-1);
        if(fields.length<3) return;
        int count=Integer.parseInt(fields[1]);
        int voteCount=fields[2].isBlank()?0:fields[2].split(",").length;
        voteSummary.setText("Players connected: "+count+"/4  |  Votes: "+voteCount+"/"+count
                +"  |  "+(fields[2].isBlank()?"No votes yet":fields[2].replace(',',' ')));
        startButton.setEnabled(host && count>=2 && voteCount==count);
    }
    private void leaveLobby() {
        client.close(); if(server!=null) server.close(); Main.mostrarMenu();
    }
}