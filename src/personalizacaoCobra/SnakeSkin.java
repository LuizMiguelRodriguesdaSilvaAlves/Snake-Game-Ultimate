package personalizacaoCobra;
import java.awt.Color;
import java.awt.image.BufferedImage;

/** Stores snake head/body/outline colors and an optional texture. */
public class SnakeSkin {
    private final SkinType type;
    private final Color primaryColor;
    private final Color secondaryColor;
    private final BufferedImage texture;
    private final Color outlineColor;
    /** Full constructor with optional image texture and explicit outline color. */
    public SnakeSkin(SkinType type, Color primaryColor, Color secondaryColor, BufferedImage texture, Color outlineColor) {
        this.type = type; this.primaryColor = primaryColor; this.secondaryColor = secondaryColor;
        this.texture = texture; this.outlineColor = outlineColor;
    }
    /** Backward-compatible full constructor. */
    public SnakeSkin(SkinType type, Color primaryColor, Color secondaryColor, BufferedImage texture) {
        this(type, primaryColor, secondaryColor, texture, null);
    }
    /** Backward-compatible color-only constructor. */
    public SnakeSkin(SkinType type, Color primaryColor, Color secondaryColor) {
        this(type, primaryColor, secondaryColor, null, null);
    }
    public SkinType getType() { return type; }
    public Color getPrimaryColor() { return primaryColor; }
    public Color getSecondaryColor() { return secondaryColor; }
    public BufferedImage getTexture() { return texture; }
    public Color getOutlineColor() { return outlineColor; }
}
