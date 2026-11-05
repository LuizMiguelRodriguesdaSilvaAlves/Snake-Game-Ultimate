package personalizacaoCobra;

import java.awt.Color;

/**
 * Global manager for the currently selected snake skin.
 * The customization UI should call setSkin(...)
 * before a new game starts.
 */
public class SnakeSkinManager {

    private static SnakeSkin currentSkin = new SnakeSkin(
            SkinType.PADRAO,
            Color.GREEN.brighter(),
            Color.GREEN.darker(),
            null
    );

    public static SnakeSkin getSkin() {
        return currentSkin;
    }

    public static void setSkin(SnakeSkin skin) {
        if (skin != null) {
            currentSkin = skin;
        }
    }
}
