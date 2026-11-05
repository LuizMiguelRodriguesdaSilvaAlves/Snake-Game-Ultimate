package modosDeJogo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.ChaosModeException;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Secret mode that randomly combines zero to ten standard modes. If Normal and
 * Reverse are both selected, their execution order is independently randomized
 * with a 50/50 chance to resolve their conflicting player/food rules. Values outside
 * the supported 0-10 range are rejected instead of being silently clamped.
 */
public final class ModoCaos extends CompositeGameMode {
    private static final Random RANDOM = new Random();
    private final int selectedCount;

    /** Creates Chaos with the requested number of simultaneously active modes. */
    public ModoCaos(int selectedCount) {
        super("chaos", "Caos (" + validateSelectionCount(selectedCount) + "/10)",
                selectModes(validateSelectionCount(selectedCount)));
        this.selectedCount = selectedCount;
    }
    /** Returns the number of selected standard modes. */
    public int getSelectedCount() { return selectedCount; }

    /** Re-rolls the standard-mode combination whenever a new round is initialized. */
    @Override
    public void init(Snake snake, ScoreManager score) {
        modes.clear();
        modes.addAll(selectModes(selectedCount));
        super.init(snake, score);
    }

    private static int validateSelectionCount(int value) {
        if (value < 0 || value > 10) {
            throw new ChaosModeException("Chaos intensity must be between 0 and 10; received: " + value);
        }
        return value;
    }

    /** Selects unique modes and applies the explicit 50/50 Normal/Reverse ordering rule. */
    private static List<GameMode> selectModes(int amount) {
        validateSelectionCount(amount);
        if (amount == 0) return List.of(new ModoPadrao());
        List<GameMode> candidates = new ArrayList<>(GameModeRegistry.createStandardModes());
        Collections.shuffle(candidates, RANDOM);
        List<GameMode> selected = new ArrayList<>(candidates.subList(0, amount));
        int normalIndex = indexOf(selected, "normal");
        int reverseIndex = indexOf(selected, "reverse");
        if (normalIndex >= 0 && reverseIndex >= 0 && RANDOM.nextBoolean()) {
            Collections.swap(selected, normalIndex, reverseIndex);
        }
        return selected;
    }
    private static int indexOf(List<GameMode> modes, String id) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).getId().equals(id)) return i;
        return -1;
    }
}
