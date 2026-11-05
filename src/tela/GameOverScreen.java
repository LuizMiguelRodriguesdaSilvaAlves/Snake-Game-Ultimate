package tela;

import modosDeJogo.GameMode;
import pontuacao.ScoreManager;
import javax.swing.*;
import java.awt.*;

/** Displays the final score and the independent best score for the completed mode. */
public class GameOverScreen extends JPanel {
    /** Builds the result screen with restart and return-to-menu actions. */
    public GameOverScreen(JFrame frame, ScoreManager score, GameMode gameMode) {
        setLayout(new BorderLayout(10, 10)); setBackground(Color.BLACK);
        JLabel title = new JLabel("FIM DE JOGO — " + gameMode.getName(), SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30)); title.setForeground(Color.WHITE);
        JPanel center = new JPanel(new GridLayout(0, 1, 8, 8)); center.setBackground(Color.BLACK);
        JLabel current = new JLabel("Pontuação: " + score.getCurrentScore(), SwingConstants.CENTER);
        current.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22)); current.setForeground(Color.WHITE);
        JLabel best = new JLabel("Recorde do modo: " + score.getHighScore(), SwingConstants.CENTER);
        best.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19)); best.setForeground(new Color(255, 215, 90));
        center.add(current); center.add(best);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12)); buttons.setBackground(Color.BLACK);
        JButton restart = new JButton("Jogar Novamente"); JButton menu = new JButton("Menu Principal");
        restart.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16)); menu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        restart.addActionListener(event -> {
            GamePanel next = new GamePanel(gameMode); frame.setContentPane(next); frame.pack();
            frame.setLocationRelativeTo(null); frame.revalidate(); frame.repaint(); next.requestFocusInWindow();
        });
        menu.addActionListener(event -> Main.mostrarMenu());
        buttons.add(restart); buttons.add(menu);
        add(title, BorderLayout.NORTH); add(center, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
    }
}
