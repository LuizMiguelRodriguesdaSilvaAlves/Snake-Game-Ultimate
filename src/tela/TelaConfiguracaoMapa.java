package tela;

import configuracoesGerais.TemaVisual;
import customizacao.UserSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Visual map configuration screen.
 */
public class TelaConfiguracaoMapa extends JPanel {

    private Color fundo;
    private Color grade;
    private Color comida;
    private Color borda;

    private PreviewMapa preview;

    public TelaConfiguracaoMapa() {

        TemaVisual tema = UserSettings.getTema();

        fundo = tema.getFundo();
        grade = tema.getGrade();
        comida = tema.getComida();
        borda = tema.getBordaCobra();

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JLabel titulo = new JLabel("Configuração do Mapa", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setForeground(Color.WHITE);

        preview = new PreviewMapa();

        JPanel controles = new JPanel(new GridLayout(5, 1, 10, 10));
        controles.setBackground(Color.BLACK);

        controles.add(botaoCor("Fundo", c -> fundo = c));
        controles.add(botaoCor("Grade", c -> grade = c));
        controles.add(botaoCor("Comida", c -> comida = c));
        controles.add(botaoCor("Borda da Cobra", c -> borda = c));

        JButton salvar = new JButton("Salvar Tema");
        salvar.addActionListener(e -> salvarTema());

        JButton voltar = new JButton("Voltar");
        voltar.addActionListener(e -> Main.mostrarMenu());

        controles.add(salvar);

        add(titulo, BorderLayout.NORTH);
        add(preview, BorderLayout.CENTER);
        add(controles, BorderLayout.EAST);
        add(voltar, BorderLayout.SOUTH);
    }

    private JButton botaoCor(String texto, java.util.function.Consumer<Color> setter) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, texto, Color.WHITE);
            if (c != null) {
                setter.accept(c);
                preview.repaint();
            }
        });
        return btn;
    }

    private void salvarTema() {
        TemaVisual novo = new TemaVisual(
                fundo,
                grade,
                comida,
                borda,
                Color.WHITE,
                new Font("Arial", Font.BOLD, 16)
        );

        UserSettings.setTema(novo);
        JOptionPane.showMessageDialog(this, "Tema salvo com sucesso!");
    }

    // =====================
    // LIVE PREVIEW
    // =====================

    private class PreviewMapa extends JPanel {

        public PreviewMapa() {
            setPreferredSize(new Dimension(400, 300));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            setBackground(fundo);

            int size = 20;
            int w = getWidth();
            int h = getHeight();

            g.setColor(grade);
            for (int x = 0; x < w; x += size)
                g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += size)
                g.drawLine(0, y, w, y);

            g.setColor(comida);
            g.fillOval(5 * size, 4 * size, size, size);

            g.setColor(borda);
            g.drawRect(3 * size, 4 * size, size, size);
        }
    }
}
