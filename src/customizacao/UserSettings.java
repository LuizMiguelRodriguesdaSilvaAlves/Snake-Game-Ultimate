package customizacao;

import configuracoesGerais.TemaVisual;

import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * Manages persistent user preferences.
 */
public class UserSettings {

    private static final String FILE = "tema.properties";
    private static TemaVisual temaAtual;

    static {
        carregarTema();
    }

    public static TemaVisual getTema() {
        return temaAtual;
    }

    public static void setTema(TemaVisual tema) {
        temaAtual = tema;
        salvarTema();
    }

    // ---------------------------
    // PERSISTENCE
    // ---------------------------

    private static void salvarTema() {
        try {
            Properties p = new Properties();

            p.setProperty("fundo", String.valueOf(temaAtual.getFundo().getRGB()));
            p.setProperty("grade", String.valueOf(temaAtual.getGrade().getRGB()));
            p.setProperty("comida", String.valueOf(temaAtual.getComida().getRGB()));
            p.setProperty("borda", String.valueOf(temaAtual.getBordaCobra().getRGB()));

            FileOutputStream out = new FileOutputStream(FILE);
            p.store(out, "Tema do Snake");
            out.close();

        } catch (Exception ignored) {}
    }

    private static void carregarTema() {
        try {
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(FILE);
            p.load(in);
            in.close();

            temaAtual = new TemaVisual(
                    new Color(Integer.parseInt(p.getProperty("fundo"))),
                    new Color(Integer.parseInt(p.getProperty("grade"))),
                    new Color(Integer.parseInt(p.getProperty("comida"))),
                    new Color(Integer.parseInt(p.getProperty("borda"))),
                    Color.WHITE,
                    new Font("Arial", Font.BOLD, 16)
            );

        } catch (Exception e) {
            // Fall back to the default theme when no saved preference is available.
            temaAtual = new TemaVisual(
                    Color.BLACK,
                    Color.DARK_GRAY,
                    Color.RED,
                    Color.GREEN,
                    Color.WHITE,
                    new Font("Arial", Font.BOLD, 16)
            );
        }
    }
}
