import java.awt.Point;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import exceptions.ChaosModeException;
import exceptions.RunAndGunException;
import exceptions.BossRushException;
import exceptions.TimeAttackException;
import exceptions.GlitchModeException;
import exceptions.ComboModeException;
import exceptions.GoldenFruitException;
import exceptions.ObstacleModeException;
import exceptions.MirroredControlsException;
import exceptions.SpeedModeException;
import exceptions.NoWallsException;
import modosDeJogo.ModoBossRush;
import modosDeJogo.ModoCombo;
import modosDeJogo.ModoContrarrelogio;
import modosDeJogo.ModoEspelho;
import modosDeJogo.ModoFrutaDourada;
import modosDeJogo.ModoGlitch;
import modosDeJogo.ModoObstaculos;
import modosDeJogo.ModoSemParedes;
import modosDeJogo.ModoVelocidade;
import modosDeJogo.GameMode;
import modosDeJogo.GameModeContext;
import modosDeJogo.GameModeRegistry;
import modosDeJogo.ModoCaos;
import modosDeJogo.ModoCampoMinado;
import modosDeJogo.ModoRunGun;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/** Dependency-free smoke tests for the mode catalog and core gameplay fixes. */
public final class ModeSystemSmokeTest {
    private ModeSystemSmokeTest() { }

    /** Runs all smoke checks and throws an AssertionError if one fails. */
    public static void main(String[] args) throws Exception {
        verifyModeCatalog();
        verifyChaosComposition();
        verifyChaosOrderChance();
        verifySnakeBodyAndShrink();
        verifyIndependentScores();
        verifyMinefieldDamage();
        verifyRunGunLifecycle();
        verifyRunGunProjectileCancellation();
        verifyModeSpecificExceptions();
        System.out.println("All Snake Ultimate smoke tests passed.");
    }

    /** Confirms that the registry exposes exactly ten standard and five secret modes. */
    private static void verifyModeCatalog() {
        check(GameModeRegistry.createStandardModes().size() == 10, "Expected ten standard modes.");
        check(GameModeRegistry.createExclusiveModes().size() == 5, "Expected five exclusive modes.");
        Set<String> ids = new HashSet<>();
        for (GameMode mode : GameModeRegistry.createStandardModes()) check(ids.add(mode.getId()), "Duplicate standard mode ID.");
        for (GameMode mode : GameModeRegistry.createExclusiveModes()) check(ids.add(mode.getId()), "Duplicate secret mode ID.");
    }

    /** Confirms that Chaos supports both the zero-mode and ten-mode boundaries. */
    private static void verifyChaosComposition() {
        check(new ModoCaos(0).getModes().size() == 1, "Zero Chaos intensity must fall back to Normal mode.");
        ModoCaos maximum = new ModoCaos(10);
        check(maximum.getSelectedCount() == 10, "Chaos must retain the requested count.");
        check(maximum.getModes().size() == 10, "Maximum Chaos intensity must compose ten modes.");
        Set<String> ids = new HashSet<>();
        for (GameMode mode : maximum.getModes()) check(ids.add(mode.getId()), "Chaos selected a duplicate mode.");
    }

    /** Checks that the Normal/Reverse precedence is distributed close to 50/50 over many rounds. */
    private static void verifyChaosOrderChance() {
        int normalFirst = 0;
        int samples = 1_000;
        for (int sample = 0; sample < samples; sample++) {
            ModoCaos chaos = new ModoCaos(10);
            int normalIndex = -1;
            int reverseIndex = -1;
            for (int index = 0; index < chaos.getModes().size(); index++) {
                String id = chaos.getModes().get(index).getId();
                if ("normal".equals(id)) normalIndex = index;
                if ("reverse".equals(id)) reverseIndex = index;
            }
            check(normalIndex >= 0 && reverseIndex >= 0, "Maximum Chaos intensity must include Normal and Reverse.");
            if (normalIndex < reverseIndex) normalFirst++;
        }
        double ratio = (double) normalFirst / samples;
        check(ratio > 0.35 && ratio < 0.65, "Normal/Reverse precedence should be approximately 50/50.");
    }

    /** Verifies that each new mode exposes and throws its dedicated exception for invalid input. */
    private static void verifyModeSpecificExceptions() throws Exception {
        checkThrows(ChaosModeException.class, () -> new ModoCaos(-1), "Chaos must reject negative intensity.");
        checkThrows(ChaosModeException.class, () -> new ModoCaos(11), "Chaos must reject intensity above ten.");

        Path directory = Files.createTempDirectory("snake-mode-exception-test");
        ScoreManager score = new ScoreManager(directory.resolve("test.score"));
        checkThrows(RunAndGunException.class, () -> new ModoRunGun().init(null, score), "Run & Gun must reject a missing player.");
        checkThrows(BossRushException.class, () -> new ModoBossRush().init(null, score), "Boss Rush must reject a missing player.");
        checkThrows(TimeAttackException.class, () -> new ModoContrarrelogio().update((GameModeContext) null), "Time Attack must reject a missing context.");
        checkThrows(GlitchModeException.class, () -> new ModoGlitch().adjustDirection(null), "Glitch must reject a missing direction.");
        checkThrows(ComboModeException.class, () -> new ModoCombo().onFoodCollected(null), "Combo must reject a missing food-collection context.");
        checkThrows(GoldenFruitException.class, () -> new ModoFrutaDourada().update((GameModeContext) null), "Golden Fruit must reject a missing context.");
        checkThrows(ObstacleModeException.class, () -> new ModoObstaculos().init(null, score), "Obstacle mode must reject a missing player.");
        checkThrows(MirroredControlsException.class, () -> new ModoEspelho().adjustDirection(null), "Mirrored Controls must reject a missing direction.");
        checkThrows(SpeedModeException.class, () -> new ModoVelocidade().getTickDelay(-1), "Progressive Speed must reject a negative score.");
        checkThrows(NoWallsException.class, () -> new ModoSemParedes().init(null, score), "No Walls must reject a missing player.");
    }

    /** Confirms that an action throws the expected exception type. */
    private static void checkThrows(Class<? extends RuntimeException> expected, Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            check(expected.isInstance(exception), message + " Wrong exception: " + exception.getClass().getSimpleName());
            return;
        }
        throw new AssertionError(message + " No exception was thrown.");
    }

    /** Confirms that the snake is contiguous and can lose tail segments safely. */
    private static void verifySnakeBodyAndShrink() {
        Snake snake = new Snake(new Point(10, 10), Direction.RIGHT, 5);
        Point previous = null;
        for (Point segment : snake.getBody()) {
            if (previous != null) check(previous.distance(segment) == 1.0, "Snake body segments must be contiguous.");
            previous = segment;
        }
        check(snake.shrink(2) == 2, "Snake must remove the requested number of tail segments.");
        check(snake.getBody().size() == 3, "Snake length must reflect tail damage.");
        check(snake.shrink(50) == 2, "Snake must keep its head when asked to shrink below one segment.");
        check(snake.getBody().size() == 1, "Snake must never become empty.");
    }

    /** Confirms that two mode identifiers persist records to different files. */
    private static void verifyIndependentScores() throws Exception {
        Path directory = Files.createTempDirectory("snake-score-test");
        ScoreManager normal = new ScoreManager(directory.resolve("normal.score"));
        ScoreManager reverse = new ScoreManager(directory.resolve("reverse.score"));
        normal.addPoints(17); normal.saveHighScore();
        reverse.addPoints(4); reverse.saveHighScore();
        ScoreManager loadedNormal = new ScoreManager(directory.resolve("normal.score"));
        ScoreManager loadedReverse = new ScoreManager(directory.resolve("reverse.score"));
        check(loadedNormal.getHighScore() == 17, "Normal record was not persisted independently.");
        check(loadedReverse.getHighScore() == 4, "Reverse record was not persisted independently.");
        check(!normal.getSaveFile().equals(reverse.getSaveFile()), "Mode score paths must be different.");
    }

    /** Invokes the Minefield blast handler and checks that body length and score drop together. */
    private static void verifyMinefieldDamage() throws Exception {
        Snake snake = new Snake(new Point(10, 10), Direction.RIGHT, 6);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-mine-test").resolve("minefield.score"));
        score.addPoints(20);
        ModoCampoMinado minefield = new ModoCampoMinado();
        minefield.init(snake, score);
        Method applyBlast = ModoCampoMinado.class.getDeclaredMethod("applyExplosionEffect", Snake.class, Point.class);
        applyBlast.setAccessible(true);
        applyBlast.invoke(minefield, snake, new Point(7, 10));
        check(snake.getBody().size() == 2, "Minefield blast must remove the damaged number of body segments.");
        check(score.getCurrentScore() == 16, "Minefield blast must subtract one point per damaged segment.");
    }

    /** Confirms that Run & Gun can initialize, receive fire input and advance safely. */
    private static void verifyRunGunLifecycle() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-run-gun-test").resolve("run-and-gun.score"));
        ModoRunGun mode = new ModoRunGun();
        mode.init(snake, score);
        check(!mode.usesFood(), "Run & Gun must replace regular fruit with enemy fruit.");
        mode.onKeyPressed(KeyEvent.VK_SPACE);
        GameModeContext context = new GameModeContext(snake, score, null, 30, 20, 20);
        for (int tick = 0; tick < 40; tick++) mode.update(context);
        check(score.getCurrentScore() >= 0, "Run & Gun score must remain non-negative.");
    }

    /** Confirms that opposing bullets disappear when they cross paths in the same movement tick. */
    @SuppressWarnings("unchecked")
    private static void verifyRunGunProjectileCancellation() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-projectile-test").resolve("run-and-gun.score"));
        ModoRunGun mode = new ModoRunGun();
        mode.init(snake, score);

        java.lang.reflect.Field bulletsField = ModoRunGun.class.getDeclaredField("bullets");
        bulletsField.setAccessible(true);
        java.util.List<Object> bullets = (java.util.List<Object>) bulletsField.get(mode);

        Class<?> projectileType = Class.forName("modosDeJogo.ModoRunGun$Projectile");
        java.lang.reflect.Constructor<?> constructor = projectileType.getDeclaredConstructor(
                int.class, int.class, int.class, int.class, boolean.class);
        constructor.setAccessible(true);
        bullets.add(constructor.newInstance(10, 10, 1, 0, true));
        bullets.add(constructor.newInstance(11, 10, -1, 0, false));

        GameModeContext context = new GameModeContext(snake, score, null, 30, 20, 20);
        mode.update(context); // Odd tick: projectiles retain their positions.
        mode.update(context); // Even tick: projectiles cross and must cancel.
        check(bullets.isEmpty(), "Opposing projectiles must cancel when their paths cross.");
    }

    /** Throws an assertion with a readable message when a test condition is false. */
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}