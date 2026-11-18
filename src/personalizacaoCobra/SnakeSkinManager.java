package personalizacaoCobra;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Loads and persists the current computer's snake color preferences. */
public final class SnakeSkinManager {
    private static final Path SKIN_FILE = Paths.get(System.getProperty("user.home"), ".snakeultimate", "snake-skin.properties");
    private static SnakeSkin currentSkin = loadSkin();
    private SnakeSkinManager() { }
    /** Returns the saved local skin. */
    public static synchronized SnakeSkin getSkin() { return currentSkin; }
    /** Applies and persists a skin for future local and multiplayer sessions. */
    public static synchronized void setSkin(SnakeSkin skin) {
        if (skin == null) return;
        currentSkin = skin;
        saveSkin(skin);
    }
    private static SnakeSkin loadSkin() {
        SnakeSkin fallback = defaultSkin();
        if (!Files.exists(SKIN_FILE)) return fallback;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(SKIN_FILE)) {
            properties.load(input);
            return new SnakeSkin(SkinType.PADRAO,
                    parseColor(properties.getProperty("head"), fallback.getPrimaryColor()),
                    parseColor(properties.getProperty("body"), fallback.getSecondaryColor()),
                    null, parseColor(properties.getProperty("outline"), Color.BLACK));
        } catch (IOException | IllegalArgumentException exception) { return fallback; }
    }
    private static void saveSkin(SnakeSkin skin) {
        Properties properties = new Properties();
        properties.setProperty("head", Integer.toString(colorOrDefault(skin.getPrimaryColor(), Color.GREEN.brighter()).getRGB()));
        properties.setProperty("body", Integer.toString(colorOrDefault(skin.getSecondaryColor(), Color.GREEN.darker()).getRGB()));
        properties.setProperty("outline", Integer.toString(colorOrDefault(skin.getOutlineColor(), Color.BLACK).getRGB()));
        try {
            Path parent = SKIN_FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream output = Files.newOutputStream(SKIN_FILE)) {
                properties.store(output, "Snake Ultimate local skin colors");
            }
        } catch (IOException exception) {
            System.err.println("Could not persist the selected snake skin: " + exception.getMessage());
        }
    }
    private static Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank()) return fallback;
        return new Color(Integer.parseInt(value), true);
    }
    private static Color colorOrDefault(Color value, Color fallback) { return value == null ? fallback : value; }
    private static SnakeSkin defaultSkin() {
        return new SnakeSkin(SkinType.PADRAO, Color.GREEN.brighter(), Color.GREEN.darker(), null, Color.BLACK);
    }
}
