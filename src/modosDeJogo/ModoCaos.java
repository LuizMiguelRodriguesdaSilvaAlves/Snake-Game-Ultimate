package modosDeJogo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.ChaosModeException;
import sistemaDeMovimentacaoGeral.Snake;

/**
 * Chaos randomly selects up to thirteen unique compatible modes: ten standard
 * modes plus Run & Gun, Time Attack and Glitch. Nested Chaos and Boss Rush are
 * excluded to keep random compositions finite and compatible.
 */
public final class ModoCaos extends CompositeGameMode {
    private static final int MAX_SELECTION = 13;
    private static final Random RANDOM = new Random();
    private final int selectedCount;
    /** Creates Chaos with the requested number of eligible game modes. */
    public ModoCaos(int selectedCount) {
        super("chaos", "Caos (" + validateSelectionCount(selectedCount) + "/" + MAX_SELECTION + ")",
                selectModes(validateSelectionCount(selectedCount)));
        this.selectedCount = selectedCount;
    }
    /** Returns the number of selected modes. */
    public int getSelectedCount() { return selectedCount; }
    /** Re-rolls the combination for every new Chaos round. */
    @Override public void init(Snake snake, ScoreManager score) {
        modes.clear(); modes.addAll(selectModes(selectedCount)); super.init(snake, score);
    }
    private static int validateSelectionCount(int value) {
        if (value < 0 || value > MAX_SELECTION) {
            throw new ChaosModeException("Chaos intensity must be between 0 and " + MAX_SELECTION + "; received: " + value);
        }
        return value;
    }
    /** Builds the eligible pool and randomizes Normal/Reverse precedence independently. */
    private static List<GameMode> selectModes(int amount) {
        validateSelectionCount(amount);
        if (amount == 0) return List.of(new ModoPadrao());
        List<GameMode> candidates = new ArrayList<>(GameModeRegistry.createStandardModes());
        candidates.add(new ModoRunGun());
        candidates.add(new ModoContrarrelogio());
        candidates.add(new ModoGlitch());
        Collections.shuffle(candidates, RANDOM);
        List<GameMode> selected = new ArrayList<>(candidates.subList(0, amount));
        int normalIndex = indexOf(selected, "normal"), reverseIndex = indexOf(selected, "reverse");
        if (normalIndex >= 0 && reverseIndex >= 0 && RANDOM.nextBoolean()) Collections.swap(selected, normalIndex, reverseIndex);
        return selected;
    }
    private static int indexOf(List<GameMode> modes, String id) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).getId().equals(id)) return i;
        return -1;
    }
}
