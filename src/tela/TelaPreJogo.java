package tela;

import javax.swing.*;

import exceptions.MenuNavigationException;

import java.awt.*;

/** Legacy pre-game panel kept for compatibility with earlier versions. */
public class TelaPreJogo extends JPanel {

    private JFrame frame;

    public TelaPreJogo(JFrame frame) {
        this.frame = frame;

        setLayout(null);
        setBackground(Color.BLACK);

        int w = 300;
        int h = 50;

        JButton btnStart = new JButton("Jogar Agora");
        btnStart.setBounds(100, 40, w, h);
        formatButton(btnStart);

        JButton btnSkin = new JButton("Customizar Cobra");
        btnSkin.setBounds(100, 120, w, h);
        formatButton(btnSkin);

        JButton btnModes = new JButton("Modos de Jogo");
        btnModes.setBounds(100, 200, w, h);
        formatButton(btnModes);

        JButton btnExit = new JButton("Sair");
        btnExit.setBounds(100, 280, w, h);
        formatButton(btnExit);

        add(btnStart);
        add(btnSkin);
        add(btnModes);
        add(btnExit);

        // ------------------------------
        // BUTTON ACTIONS
        // ------------------------------

        btnStart.addActionListener(e -> iniciarJogo());
        btnSkin.addActionListener(e -> abrirCustomizacao());
        btnModes.addActionListener(e -> abrirModos());
        btnExit.addActionListener(e -> System.exit(0));
    }

    private void formatButton(JButton b) {
        b.setFont(new Font("Arial", Font.BOLD, 20));
        b.setFocusPainted(false);
    }

    private void iniciarJogo() {
        try {
            GamePanel game = new GamePanel();
            trocarTela(game);
        } catch (Exception ex) {
            throw new MenuNavigationException("Falha ao iniciar jogo: " + ex.getMessage());
        }
    }

    private void abrirCustomizacao() {
        // Legacy placeholder: the current application uses the dedicated customization screen.
        JOptionPane.showMessageDialog(this, "Tela de personalização será implementada após criarmos o sistema de skins.");
    }

    private void abrirModos() {
        // Legacy placeholder: the current application uses the registered mode selector.
        JOptionPane.showMessageDialog(this, "Tela de modos de jogo será implementada posteriormente.");
    }

    /** Replaces the current window content with the supplied screen. */
    private void trocarTela(JPanel novaTela) {
        if (frame == null) {
            throw new MenuNavigationException("Referência para JFrame está nula.");
        }

        novaTela.setPreferredSize(getPreferredSize());

        frame.setContentPane(novaTela);
        frame.pack();
        frame.revalidate();
        frame.repaint();
    }
}
