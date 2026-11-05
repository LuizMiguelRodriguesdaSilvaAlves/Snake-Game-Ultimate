package tela;

import javax.swing.*;
import java.awt.*;

import extras.TelaExtras;
import modosDeJogo.GameMode;
import exceptions.MenuNavigationException;

/**
 * Main application entry point.
 * Responsibilities:
 * - Create the main application window
 * - Coordinate navigation between screens
 * - Centralize screen transitions
 */
public class Main {

    /** Main application window. */
    private static JFrame frame;

    public static void main(String[] args) {

        // Create and update Swing components on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {

            frame = new JFrame("Snake Ultimate");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            mostrarMenu();

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // =========================
    // SCREEN NAVIGATION
    // =========================

    /** Displays the main menu. */
    public static void mostrarMenu() {
        frame.setContentPane(new MenuInicial(menuListener()));
        atualizarFrame();
    }

    /** Displays the standard mode selector. */
    public static void mostrarSelecaoModo() {
        frame.setContentPane(new TelaSelecaoModoJogo());
        atualizarFrame();
    }

    /** Displays the map configuration screen. */
    public static void mostrarConfiguracaoMapa() {
        frame.setContentPane(new TelaConfiguracaoMapa());
        atualizarFrame();
    }
    

    /** Starts the supplied game mode inside the main application window. */
    public static void iniciarModo(GameMode mode) {
        if (frame == null) throw new MenuNavigationException("Main window has not been initialized.");
        frame.setContentPane(new GamePanel(mode));
        atualizarFrame();
        frame.getContentPane().requestFocusInWindow();
    }

    /** Displays the secret mode selector. */
    public static void mostrarExtras() {
        try {
            frame.setContentPane(new TelaExtras(() -> mostrarMenu(), Main::iniciarModo));
            atualizarFrame();
        } catch (Exception ex) {
            throw new MenuNavigationException("Falha ao abrir tela de extras: " + ex.getMessage());
        }
    }

    /** Displays the snake customization screen. */
    public static void mostrarPersonalizacaoCobra() {

        TelaPersonalizacaoCobra tela =
                new TelaPersonalizacaoCobra(new TelaPersonalizacaoCobra.PersonalizacaoListener() {

                    @Override
                    public void salvarPersonalizacao(Color cabeca, Color corpo, Color borda) {
                        // SnakeSkinManager already persists the selected skin.
                    }

                    @Override
                    public void voltar() {
                        mostrarMenu();
                    }
                });

        frame.setContentPane(tela);
        atualizarFrame();
    }

    // =========================
    // MAIN MENU LISTENER
    // =========================

    /**
     * Central listener for main menu actions.
     * Individual screens do not own application navigation decisions.
     */
    public static MenuInicial.MenuListener menuListener() {
        return new MenuInicial.MenuListener() {

            @Override
            public void iniciarJogo() {
                mostrarSelecaoModo();
            }

            @Override
            public void abrirPersonalizacaoCobra() {
                mostrarPersonalizacaoCobra();
            }

            @Override
            public void abrirConfiguracaoMapa() {
                mostrarConfiguracaoMapa();
            }
            
            @Override
            public void abrirExtras() {
                mostrarExtras();
            }
        };
    }

    // =========================
    // UTILITIES
    // =========================

    /** Refreshes the window after a screen transition. */
    private static void atualizarFrame() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.revalidate();
        frame.repaint();
    }
}
