package modosDeJogo;

import java.util.List;

/**
 * Central factory for the ten standard modes and five Konami-exclusive modes.
 * Factories create fresh instances so that state is never shared between rounds.
 */
public final class GameModeRegistry {
    private GameModeRegistry() { }

    /** Returns fresh instances of all ten standard game modes. */
    public static List<GameMode> createStandardModes() {
        return List.of(new ModoPadrao(), new ModoCampoMinado(), new ModoPortal(), new ModoReverso(),
                new ModoVelocidade(), new ModoEspelho(), new ModoObstaculos(), new ModoSemParedes(),
                new ModoCombo(), new ModoFrutaDourada());
    }

    /** Returns fresh instances of the five secret game modes. */
    public static List<GameMode> createExclusiveModes() {
        return List.of(new ModoCaos(5), new ModoRunGun(), new ModoBossRush(),
                new ModoContrarrelogio(), new ModoGlitch());
    }

    /** Creates a fresh standard mode by its stable identifier. */
    public static GameMode createStandard(String id) {
        return createStandardModes().stream().filter(mode -> mode.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown standard mode: " + id));
    }
}
