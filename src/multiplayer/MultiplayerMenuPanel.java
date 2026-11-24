package multiplayer;

import tela.Main;

import javax.swing.*;
import java.awt.*;
import exceptions.MenuNavigationException;
import personalizacaoCobra.SnakeSkinManager;
import customizacao.UserSettings;

/** Entry screen for hosting or joining a four-player local-area-network match. */
public final class MultiplayerMenuPanel extends JPanel {
    private final JTextField nameField=new JTextField("Player",14);
    private final JTextField addressField=new JTextField(LanServer.findLocalAddress(),14);
    private final JTextField portField=new JTextField(Integer.toString(LanServer.DEFAULT_PORT),6);

    public MultiplayerMenuPanel() {
        setLayout(new BorderLayout(12,12)); setBackground(Color.BLACK);
        JLabel title=new JLabel("MULTIPLAYER LAN",SwingConstants.CENTER);
        title.setForeground(Color.WHITE); title.setFont(new Font(Font.SANS_SERIF,Font.BOLD,26));
        JPanel form=new JPanel(new GridLayout(0,2,10,12)); form.setBackground(Color.BLACK);
        form.setBorder(BorderFactory.createEmptyBorder(25,50,25,50));
        addField(form,"Player name",nameField); addField(form,"Host IP",addressField); addField(form,"Port",portField);
        JButton host=new JButton("Host a match"); JButton join=new JButton("Join a match");
        host.addActionListener(event -> launch(true)); join.addActionListener(event -> launch(false));
        JPanel actions=new JPanel(); actions.setBackground(Color.BLACK); actions.add(host); actions.add(join);
        JButton back=new JButton("Back to menu"); back.addActionListener(event -> Main.mostrarMenu());
        JPanel footer=new JPanel(new BorderLayout()); footer.setBackground(Color.BLACK); footer.add(actions,BorderLayout.CENTER); footer.add(back,BorderLayout.SOUTH);
        add(title,BorderLayout.NORTH); add(form,BorderLayout.CENTER); add(footer,BorderLayout.SOUTH);
    }
    private void addField(JPanel panel,String label,JTextField field) {
        JLabel caption=new JLabel(label); caption.setForeground(Color.WHITE); caption.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));
        panel.add(caption); field.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16)); panel.add(field);
    }
    private void launch(boolean host) {
        LanServer server=null;
        try {
            int port=Integer.parseInt(portField.getText().trim());
            MultiplayerProfile profile=MultiplayerProfile.fromLocalSkin(nameField.getText(),SnakeSkinManager.getSkin());
            String address=addressField.getText().trim();
            if(host) { server=new LanServer(port, UserSettings.getTema()); server.start(); address="127.0.0.1"; }
            Main.mostrarLobby(server,address,port,profile,host);
        } catch(NumberFormatException exception) {
            JOptionPane.showMessageDialog(this,"Port must be a number between 1024 and 65535.","Invalid port",JOptionPane.ERROR_MESSAGE);
            if(server!=null) server.close();
        } catch(RuntimeException exception) {
            JOptionPane.showMessageDialog(this,exception.getMessage(),"LAN connection error",JOptionPane.ERROR_MESSAGE);
            if(server!=null) server.close();
        }
    }
}