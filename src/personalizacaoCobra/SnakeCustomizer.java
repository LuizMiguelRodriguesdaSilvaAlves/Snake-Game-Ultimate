package personalizacaoCobra;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Factory helpers for default, preset and image-textured snake skins.
 */
public class SnakeCustomizer {

    // DEFAULT SKIN
    public static SnakeSkin loadDefaultSkin() {
        return new SnakeSkin(
                SkinType.PADRAO,
                Color.GREEN.brighter(),
                Color.GREEN.darker(),
                null
        );
    }

    // PREDEFINED SKINS
    public static SnakeSkin predefinedGreenNeon() {
        return new SnakeSkin(
                SkinType.VERDE_NEON,
                Color.GREEN.brighter(),
                Color.GREEN.darker(),
                null
        );
    }

    public static SnakeSkin predefinedFire() {
        return new SnakeSkin(
                SkinType.FOGO,
                Color.ORANGE,
                Color.RED,
                null
        );
    }

    // IMAGE TEXTURE
    public static SnakeSkin loadTextureSkin(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));

            if (img == null) {
                return loadDefaultSkin();
            }

            return new SnakeSkin(
                    SkinType.CUSTOM_IMAGE,
                    null,
                    null,
                    img
            );

        } catch (Exception e) {
            return loadDefaultSkin();
        }
    }
}
