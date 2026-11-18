package multiplayer;

import exceptions.MultiplayerException;

import java.awt.Color;
import java.util.Objects;

/** Stores a local player's display name and the three colors used by their snake. */
public record MultiplayerProfile(String name, int headRgb, int bodyRgb, int outlineRgb) {
    public MultiplayerProfile {
        if (name == null || name.isBlank()) throw new MultiplayerException("Player name cannot be empty.");
        name = name.trim().replace("|", "").replace(";", "").replace("~", "");
        if (name.length() > 16) name = name.substring(0, 16);
        Objects.requireNonNull(name);
        new Color(headRgb, true);
        new Color(bodyRgb, true);
        new Color(outlineRgb, true);
    }
    /** Creates a network profile from the skin saved on this computer. */
    public static MultiplayerProfile fromLocalSkin(String name, personalizacaoCobra.SnakeSkin skin) {
        if (skin == null) throw new MultiplayerException("A local snake skin is required.");
        return new MultiplayerProfile(name,
                skin.getPrimaryColor().getRGB(), skin.getSecondaryColor().getRGB(),
                skin.getOutlineColor() == null ? Color.BLACK.getRGB() : skin.getOutlineColor().getRGB());
    }
    public Color headColor() { return new Color(headRgb, true); }
    public Color bodyColor() { return new Color(bodyRgb, true); }
    public Color outlineColor() { return new Color(outlineRgb, true); }
}