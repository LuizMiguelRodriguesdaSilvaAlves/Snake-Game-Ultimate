package personalizacaoCobra;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Stores the colors and optional texture used to draw the player snake. */
public class SnakeSkin {

    private final SkinType type;
    private final Color primaryColor;
    private final Color secondaryColor;
    private final BufferedImage texture;

    /**
     * Full constructor (colors and texture).
     */
    public SnakeSkin(
            SkinType type,
            Color primaryColor,
            Color secondaryColor,
            BufferedImage texture
    ) {
        this.type = type;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.texture = texture;
    }

    /**
     * Simplified constructor (colors only).
     * Used by the snake customization screen.
     */
    public SnakeSkin(
            SkinType type,
            Color primaryColor,
            Color secondaryColor
    ) {
        this(type, primaryColor, secondaryColor, null);
    }

    public SkinType getType() {
        return type;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public BufferedImage getTexture() {
        return texture;
    }
}
