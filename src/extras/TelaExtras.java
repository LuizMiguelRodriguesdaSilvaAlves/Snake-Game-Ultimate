package extras;

import modosDeJogo.GameMode;
import modosDeJogo.GameModeRegistry;
import modosDeJogo.ModoCaos;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** Displays the five secret modes unlocked through the Konami Code. */
public class TelaExtras extends JPanel {
    private final Runnable voltarMenu;
    private final Consumer<GameMode> iniciarModo;

    /** Creates the secret mode selector and receives navigation/game-launch callbacks. */
    public TelaExtras(Runnable voltarMenu, Consumer<GameMode> iniciarModo) {
        if (voltarMenu == null || iniciarModo == null) throw new IllegalArgumentException("Secret screen callbacks cannot be null.");
        this.voltarMenu = voltarMenu; this.iniciarModo = iniciarModo;
        setPreferredSize(new Dimension(700, 520)); setLayout(new BorderLayout(10, 10)); setBackground(Color.BLACK);
        createContent();
    }

    /** Builds a five-entry list of exclusive mode launchers. */
    private void createContent() {
        JLabel title = new JLabel("MODOS SECRETOS DESBLOQUEADOS", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30)); title.setForeground(new Color(255, 215, 0));
        title.setBorder(BorderFactory.createEmptyBorder(22, 12, 10, 12));
        JPanel list = new JPanel(new GridLayout(0, 1, 10, 10)); list.setBackground(Color.BLACK);
        list.setBorder(BorderFactory.createEmptyBorder(12, 45, 12, 45));
        for (GameMode mode : GameModeRegistry.createExclusiveModes()) list.add(createModeButton(mode));
        JButton back = new JButton("Voltar ao Menu Principal"); back.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        back.addActionListener(event -> voltarMenu.run());
        JPanel footer = new JPanel(); footer.setBackground(Color.BLACK); footer.add(back);
        add(title, BorderLayout.NORTH); add(list, BorderLayout.CENTER); add(footer, BorderLayout.SOUTH);
    }

    /** Creates a secret mode button and asks for Intensidade do Caos when necessary. */
    private JButton createModeButton(GameMode mode) {
        String label = switch (mode.getId()) {
            case "chaos" -> "CAOS — combine de 0 a 10 modos padrão";
            case "run-and-gun" -> "RUN & GUN — atire nas frutas e desvie dos disparos";
            case "boss-rush" -> "BOSS RUSH — sobreviva aos chefes perseguidores";
            case "time-attack" -> "CONTRA O RELÓGIO — faça pontos em 90 segundos";
            case "glitch" -> "GLITCH — os controles giram aleatoriamente";
            default -> mode.getName();
        };
        JButton button = new JButton(label); button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setForeground(new Color(45, 35, 65));
        button.addActionListener(event -> {
            if ("chaos".equals(mode.getId())) {
                Integer[] amounts = new Integer[11]; for (int i=0; i<=10; i++) amounts[i]=i;
                Integer selected = (Integer) JOptionPane.showInputDialog(this,
                        "Quantos modos padrão devem funcionar simultaneamente?", "Intensidade do Caos",
                        JOptionPane.QUESTION_MESSAGE, null, amounts, 5);
                if (selected == null) return;
                iniciarModo.accept(new ModoCaos(selected));
            } else iniciarModo.accept(mode);
        });
        return button;
    }
}
