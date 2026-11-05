package tela;

import modosDeJogo.GameMode;
import modosDeJogo.GameModeRegistry;
import javax.swing.*;
import java.awt.*;

/** Presents the ten standard modes registered by the game. */
public class TelaSelecaoModoJogo extends JPanel {
    /** Creates a scrollable two-column selector for the standard game modes. */
    public TelaSelecaoModoJogo() {
        setBackground(Color.BLACK); setLayout(new BorderLayout(12, 12));
        JLabel title = new JLabel("MODOS DE JOGO PADRÃO", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27)); title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(18, 10, 8, 10));
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12)); grid.setBackground(Color.BLACK);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        for (GameMode mode : GameModeRegistry.createStandardModes()) grid.add(createModeCard(mode));
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(Color.BLACK);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        JButton back = new JButton("Voltar ao Menu Principal"); back.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        back.addActionListener(event -> Main.mostrarMenu());
        JPanel footer = new JPanel(); footer.setBackground(Color.BLACK); footer.add(back);
        add(title, BorderLayout.NORTH); add(scroll, BorderLayout.CENTER); add(footer, BorderLayout.SOUTH);
    }

    /** Builds one mode card and connects it to the central game launcher. */
    private JPanel createModeCard(GameMode mode) {
        JPanel card = new JPanel(new BorderLayout(4, 5)); card.setBackground(new Color(28, 28, 34));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(85, 85, 100)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel description = new JLabel("<html><div style='width:190px;color:#cccccc'>" + descriptionFor(mode.getId()) + "</div></html>");
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JButton play = new JButton(mode.getName()); play.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        play.addActionListener(event -> Main.iniciarModo(mode));
        card.add(play, BorderLayout.NORTH); card.add(description, BorderLayout.CENTER);
        return card;
    }

    /** Returns concise descriptions for the standard mode catalog. */
    private String descriptionFor(String id) {
        return switch (id) {
            case "normal" -> "A experiência clássica de Snake: colete frutas, cresça e evite colisões.";
            case "minefield" -> "Minas surgem durante a partida. Fuja da explosão ou perca segmentos.";
            case "portal" -> "Coletar uma fruta teletransporta a cobra para uma nova posição segura.";
            case "reverse" -> "Torne-se a fruta caçada e sobreviva às cobras inimigas controladas por IA.";
            case "speed" -> "O jogo fica mais rápido conforme sua pontuação aumenta.";
            case "mirror-controls" -> "Os controles são espelhados: cada direção solicitada é invertida.";
            case "obstacles" -> "Atravesse um mapa repleto de blocos de pedra.";
            case "wraparound" -> "Ao atravessar uma borda, a cobra retorna pelo lado oposto do mapa.";
            case "combo" -> "Colete frutas rapidamente para aumentar seu combo e ganhar pontos extras.";
            case "golden-fruit" -> "Uma fruta dourada aparece periodicamente e concede pontos extras.";
            default -> "Um modo de jogo exclusivo do Snake Ultimate.";
        };
    }
}
