package pontuacao;

import exceptions.HighScorePersistenceException;
import exceptions.ScoreException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Stores the current score and the personal record for one game mode.
 * Each stable mode identifier maps to a separate file under the user's home directory.
 */
public class ScoreManager {
    private int currentScore;
    private int highScore;
    private final Path saveFile;

    /** Creates a score manager for the standard Normal mode. */
    public ScoreManager() { this("normal"); }

    /** Creates a score manager whose record is isolated from every other mode. */
    public ScoreManager(String modeId) {
        this(Paths.get(System.getProperty("user.home"), ".snakeultimate", "scores", safeId(modeId) + ".score"));
        if ("normal".equals(safeId(modeId)) && highScore == 0) importLegacyNormalScore();
    }

    /** Allows a custom save path, primarily for tests and tools. */
    public ScoreManager(Path saveFile) {
        if (saveFile == null) throw new ScoreException("Score file path cannot be null.");
        this.saveFile = saveFile;
        loadHighScore();
    }

    /** Resets only the current round score; the persistent record is preserved. */
    public void resetScore() { currentScore = 0; }

    /** Adds a non-negative amount and updates the record when a new best is reached. */
    public void addPoints(int points) {
        if (points < 0) throw new ScoreException("Cannot add a negative score value: " + points);
        currentScore += points;
        if (currentScore > highScore) highScore = currentScore;
    }

    /** Removes points without allowing the round score to fall below zero. */
    public void removePoints(int points) {
        if (points < 0) throw new ScoreException("Cannot remove a negative score value: " + points);
        currentScore = Math.max(0, currentScore - points);
    }

    /** Returns the current round score. */
    public int getCurrentScore() { return currentScore; }
    /** Returns the persistent high score for this mode. */
    public int getHighScore() { return highScore; }
    /** Updates the in-memory high score after validating the supplied value. */
    public void setHighScore(int value) {
        if (value < 0) throw new ScoreException("High score cannot be negative.");
        highScore = value;
    }
    /** Returns the file used by this score manager. */
    public Path getSaveFile() { return saveFile; }

    /** Writes the current high score to this mode's dedicated file. */
    public void saveHighScore() {
        try {
            Path parent = saveFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(saveFile, Integer.toString(highScore), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new HighScorePersistenceException("Could not save high score to: " + saveFile, exception);
        }
    }

    /** Loads this mode's record. A missing file represents a new mode record. */
    public void loadHighScore() {
        try {
            if (!Files.exists(saveFile)) return;
            String content = Files.readString(saveFile, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) return;
            int value = Integer.parseInt(content);
            if (value < 0) throw new HighScorePersistenceException("Negative high score stored in: " + saveFile);
            highScore = value;
        } catch (NumberFormatException exception) {
            throw new HighScorePersistenceException("Invalid high score format in: " + saveFile, exception);
        } catch (IOException exception) {
            throw new HighScorePersistenceException("Could not read high score from: " + saveFile, exception);
        }
    }

    /** Imports the old shared score file once so existing Normal mode records are not lost. */
    private void importLegacyNormalScore() {
        Path legacy = Paths.get(System.getProperty("user.home"), ".snake_highscore");
        if (!Files.exists(legacy)) return;
        try {
            String content = Files.readString(legacy, StandardCharsets.UTF_8).trim();
            if (!content.isEmpty()) {
                int value = Integer.parseInt(content);
                if (value > highScore) { highScore = value; saveHighScore(); }
            }
        } catch (IOException | NumberFormatException ignored) {
            // A legacy record is optional; an invalid old file must not prevent the game from starting.
        }
    }

    /** Restricts identifiers to safe filename characters. */
    private static String safeId(String id) {
        if (id == null || id.isBlank()) return "unknown-mode";
        return id.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
    }
}
