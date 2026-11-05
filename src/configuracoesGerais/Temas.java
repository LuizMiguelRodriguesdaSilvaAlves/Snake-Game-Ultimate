package configuracoesGerais;

import java.awt.Color;
import java.awt.Font;

/**
 * Catalog of predefined visual themes.
 */
public class Temas {

    public static final TemaVisual CLASSICO = new TemaVisual(
        Color.BLACK,              // Background
        Color.DARK_GRAY,           // Grid
        Color.RED,                 // Food
        Color.GREEN,               // Snake outline
        Color.WHITE,               // Text
        new Font("Arial", Font.BOLD, 18)
    );

    public static final TemaVisual NEON = new TemaVisual(
        new Color(10, 10, 20),      // Background
        new Color(0, 255, 200),     // Grid
        new Color(255, 50, 50),     // Food
        Color.CYAN,                // Snake outline
        Color.CYAN,                // Text
        new Font("Consolas", Font.BOLD, 18)
    );
}
