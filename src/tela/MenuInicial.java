package tela;

import exceptions.KonamiCodeException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Builds the main menu and listens for the Konami Code sequence. */
public class MenuInicial extends JPanel {

    private final MenuListener listener;

    private final int[] KONAMI_CODE = {
            KeyEvent.VK_UP,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            KeyEvent.VK_B,
            KeyEvent.VK_A
    };

    private int konamiIndex = 0;

    public interface MenuListener {
        void iniciarJogo();
        void abrirPersonalizacaoCobra();
        void abrirConfiguracaoMapa();
        void abrirExtras();
    }

    public MenuInicial(MenuListener listener) {

        if (listener == null) {
            throw new KonamiCodeException("MenuListener não pode ser null.");
        }

        this.listener = listener;

        setPreferredSize(new Dimension(600, 400));
        setLayout(new GridLayout(4, 1, 10, 10));
        setBackground(Color.BLACK);
        setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));

        JButton btnIniciarJogo = new JButton("Iniciar Jogo");
        JButton btnPersonalizar = new JButton("Personalizar Cobra");
        JButton btnMapa = new JButton("Configurações do Mapa");
        JButton btnSair = new JButton("Sair");

        estilizar(btnIniciarJogo);
        estilizar(btnPersonalizar);
        estilizar(btnMapa);
        estilizar(btnSair);

        btnIniciarJogo.addActionListener(e -> listener.iniciarJogo());
        btnPersonalizar.addActionListener(e -> listener.abrirPersonalizacaoCobra());
        btnMapa.addActionListener(e -> listener.abrirConfiguracaoMapa());
        btnSair.addActionListener(e -> System.exit(0));

        add(btnIniciarJogo);
        add(btnPersonalizar);
        add(btnMapa);
        add(btnSair);

        configurarKonamiCode();
    }

    private void estilizar(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 22));
        btn.setFocusable(false);
    }

    private void configurarKonamiCode() {

        registrarTecla(KeyEvent.VK_UP);
        registrarTecla(KeyEvent.VK_DOWN);
        registrarTecla(KeyEvent.VK_LEFT);
        registrarTecla(KeyEvent.VK_RIGHT);
        registrarTecla(KeyEvent.VK_B);
        registrarTecla(KeyEvent.VK_A);
    }

    private void registrarTecla(int keyCode) {

        String actionName = "KONAMI_" + keyCode;

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), actionName);

        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processarKonamiCode(keyCode);
            }
        });
    }

    private void processarKonamiCode(int teclaPressionada) {

        if (teclaPressionada == KONAMI_CODE[konamiIndex]) {
            konamiIndex++;

            if (konamiIndex == KONAMI_CODE.length) {
                konamiIndex = 0;
                listener.abrirExtras();
            }

            return;
        }

        if (teclaPressionada == KONAMI_CODE[0]) {
            konamiIndex = 1;
        } else {
            konamiIndex = 0;
        }
    }
}