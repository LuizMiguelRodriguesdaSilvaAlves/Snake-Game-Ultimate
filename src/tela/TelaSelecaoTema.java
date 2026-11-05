package tela;

import configuracoesGerais.TemaVisual;
import configuracoesGerais.Temas;
import customizacao.UserSettings;

import javax.swing.*;
import java.awt.*;

/** Displays the visual themes available to the player. */
public class TelaSelecaoTema extends JPanel {

    public interface TemaListener {
        void voltar();
    }

    private final TemaListener listener;

    public TelaSelecaoTema(TemaListener listener) {
        this.listener = listener;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(600, 400));

        JLabel titulo = new JLabel("Selecionar Tema", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new GridLayout(0, 1, 10, 10));

        addBotaoTema(centro, "Clássico", Temas.CLASSICO);
        addBotaoTema(centro, "Neon", Temas.NEON);

        add(centro, BorderLayout.CENTER);

        JButton voltar = new JButton("Voltar");
        voltar.addActionListener(e -> listener.voltar());

        JPanel rodape = new JPanel();
        rodape.add(voltar);

        add(rodape, BorderLayout.SOUTH);

        aplicarTema();
    }

    // ===========================
    // THEME BUTTON
    // ===========================

    private void addBotaoTema(JPanel painel, String nome, TemaVisual tema) {

        JButton btn = new JButton(nome);

        btn.addActionListener(e -> {
            UserSettings.setTema(tema);
            aplicarTema();
        });

        painel.add(btn);
    }

    // ===========================
    // APPLY THE SELECTED THEME TO THIS SCREEN
    // ===========================

    private void aplicarTema() {

        TemaVisual tema = UserSettings.getTema();

        setBackground(tema.getFundo());

        for (Component c : getComponents()) {
            aplicarRecursivo(c, tema);
        }

        repaint();
    }

    private void aplicarRecursivo(Component c, TemaVisual tema) {

        c.setBackground(tema.getFundo());
        c.setForeground(tema.getTexto());
        c.setFont(tema.getFonte());

        if (c instanceof Container cont) {
            for (Component filho : cont.getComponents()) {
                aplicarRecursivo(filho, tema);
            }
        }
    }
}
