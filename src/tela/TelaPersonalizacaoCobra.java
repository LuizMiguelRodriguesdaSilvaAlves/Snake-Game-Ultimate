package tela;

import exceptions.CobraPersonalizacaoException;
import exceptions.CorInvalidaException;

import personalizacaoCobra.SnakeSkin;
import personalizacaoCobra.SnakeSkinManager;
import personalizacaoCobra.SkinType;

import javax.swing.*;
import java.awt.*;

/**
 * Screen responsible for snake appearance customization.
 * Allows the player to change the head, body and outline colors,
 * while displaying a real-time preview.
 */
public class TelaPersonalizacaoCobra extends JPanel {

    // =========================
    // CUSTOMIZATION STATE
    // =========================

    private Color corCabeca = Color.GREEN;
    private Color corCorpo  = new Color(0, 180, 0);
    private Color corBorda  = Color.BLACK;

    // =========================
    // USER INTERFACE COMPONENTS
    // =========================

    private JButton btnCorCabeca;
    private JButton btnCorCorpo;
    private JButton btnCorBorda;
    private JButton btnSalvar;
    private JButton btnVoltar;

    private PreviewCobra preview;

    // Required navigation callback.
    private final PersonalizacaoListener listener;

    // =========================
    // CALLBACKS
    // =========================

    public interface PersonalizacaoListener {
        void salvarPersonalizacao(Color cabeca, Color corpo, Color borda)
                throws CobraPersonalizacaoException;
        void voltar();
    }

    // =========================
    // CONSTRUCTOR
    // =========================

    /**
     * A listener is always required by this screen.
     * This guarantees that the Back button can return to the previous screen.
     */
    public TelaPersonalizacaoCobra(PersonalizacaoListener listener) {
        this.listener = listener;
        inicializarTela();
    }

    // =========================
    // USER INTERFACE
    // =========================

    private void inicializarTela() {

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        preview = new PreviewCobra();

        JPanel botoes = new JPanel(new GridLayout(5, 1, 10, 10));
        botoes.setBackground(Color.BLACK);

        btnCorCabeca = new JButton("Cor da Cabeça");
        btnCorCorpo  = new JButton("Cor do Corpo");
        btnCorBorda  = new JButton("Cor da Borda");

        estilizar(btnCorCabeca);
        estilizar(btnCorCorpo);
        estilizar(btnCorBorda);

        btnCorCabeca.addActionListener(e ->
                selecionarCor(c -> { corCabeca = c; preview.repaint(); })
        );

        btnCorCorpo.addActionListener(e ->
                selecionarCor(c -> { corCorpo = c; preview.repaint(); })
        );

        btnCorBorda.addActionListener(e ->
                selecionarCor(c -> { corBorda = c; preview.repaint(); })
        );

        btnSalvar = new JButton("Salvar Customização");
        btnVoltar = new JButton("Voltar");

        estilizar(btnSalvar);
        estilizar(btnVoltar);

        btnSalvar.addActionListener(e -> salvarSkin());
        btnVoltar.addActionListener(e -> listener.voltar());

        botoes.add(btnCorCabeca);
        botoes.add(btnCorCorpo);
        botoes.add(btnCorBorda);
        botoes.add(btnSalvar);
        botoes.add(btnVoltar);

        add(preview, BorderLayout.CENTER);
        add(botoes, BorderLayout.EAST);
    }

    // =========================
    // CUSTOMIZATION LOGIC
    // =========================

    private void salvarSkin() {
        try {
            validar();

            SnakeSkin novaSkin = new SnakeSkin(
                    SkinType.PADRAO,
                    corCabeca,
                    corCorpo,
                    null
            );

            SnakeSkinManager.setSkin(novaSkin);

            JOptionPane.showMessageDialog(this, "Skin salva com sucesso!");

            listener.salvarPersonalizacao(corCabeca, corCorpo, corBorda);

        } catch (CobraPersonalizacaoException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void validar() throws CobraPersonalizacaoException {
        if (corCabeca == null || corCorpo == null || corBorda == null)
            throw new CorInvalidaException("Nenhuma cor pode ser nula.");
    }

    // =========================
    // UTILITIES
    // =========================

    private interface CorSelecionadaCallback {
        void onColorSelected(Color color);
    }

    private void selecionarCor(CorSelecionadaCallback callback) {
        Color novaCor = JColorChooser.showDialog(
                this, "Selecione a Cor", Color.WHITE
        );
        if (novaCor != null) callback.onColorSelected(novaCor);
    }

    private void estilizar(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setFocusable(false);
    }

    // =========================
    // LIVE PREVIEW
    // =========================

    private class PreviewCobra extends JPanel {

        public PreviewCobra() {
            setPreferredSize(new Dimension(400, 400));
            setBackground(Color.DARK_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int x = getWidth() / 2 - 75;
            int y = getHeight() / 2 - 25;

            g2.setColor(corCorpo);
            g2.fillRoundRect(x + 50, y, 150, 50, 20, 20);

            g2.setColor(corBorda);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(x + 50, y, 150, 50, 20, 20);

            g2.setColor(corCabeca);
            g2.fillOval(x, y, 50, 50);

            g2.setColor(corBorda);
            g2.drawOval(x, y, 50, 50);
        }
    }
}
