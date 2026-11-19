import java.awt.Point;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;
import java.util.List;
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
import exceptions.MinaExplodiuException;
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
import modosDeJogo.ModoPortal;
import modosDeJogo.ModoRunGun;
import modosDeJogo.ModoReverso;
import modosDeJogo.CompositeGameMode;
import multiplayer.LanClient;
import multiplayer.LanServer;
import exceptions.MultiplayerException;
import exceptions.MultiplayerLobbyException;
import multiplayer.MultiplayerProfile;
import multiplayer.MultiplayerRuleset;
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
        verifyReverseSuppressionWhenNormalHasPriority();
        verifyGoldenFruitSuppressesReverseIdentity();
        verifySnakeBodyAndShrink();
        verifyIndependentScores();
        verifyMinefieldDamage();
        verifyMinefieldFruitIdentityMilestonesRespectMaze();
        verifyMinefieldFruitIdentityExplosionIsFatal();
        verifyRunGunLifecycle();
        verifyBossRushCombatAndProgression();
        verifyRunGunProjectileCancellation();
        verifyModeSpecificExceptions();
        verifyGoldenFruitAvoidsWallsAndObstacles();
        verifyPortalAvoidsCompositeObstacles();
        verifyMultiplayerContracts();
        verifyLanMajorityVoteAndSnapshots();
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
        ModoCaos maximum = new ModoCaos(13);
        check(maximum.getSelectedCount() == 13, "Chaos must retain the requested count.");
        check(maximum.getModes().size() == 13, "Maximum Chaos intensity must compose thirteen compatible modes.");
        check(maximum.getModes().stream().noneMatch(mode -> "boss-rush".equals(mode.getId()) || "chaos".equals(mode.getId())),
                "Chaos must exclude Boss Rush and nested Chaos.");
        Set<String> ids = new HashSet<>();
        for (GameMode mode : maximum.getModes()) check(ids.add(mode.getId()), "Chaos selected a duplicate mode.");
    }

    /** Checks that the Normal/Reverse precedence is distributed close to 50/50 over many rounds. */
    private static void verifyChaosOrderChance() {
        int normalFirst = 0;
        int samples = 1_000;
        for (int sample = 0; sample < samples; sample++) {
            ModoCaos chaos = new ModoCaos(13);
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
        checkThrows(ChaosModeException.class, () -> new ModoCaos(14), "Chaos must reject intensity above thirteen.");

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


    /** Confirms that Reverse's pursuit AI is fully disabled when Normal has priority. */
    private static void verifyReverseSuppressionWhenNormalHasPriority() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-chaos-reverse-test").resolve("chaos.score"));
        List<GameMode> ordered = new java.util.ArrayList<>();
        ordered.add(GameModeRegistry.createStandard("normal"));
        ordered.add(GameModeRegistry.createStandard("reverse"));
        modosDeJogo.CompositeGameMode composition =
                new modosDeJogo.CompositeGameMode("test", "Test", ordered);
        composition.init(snake, score);
        ModoReverso reverse = (ModoReverso) ordered.get(1);
        check(reverse.getCobraInimiga().isEmpty(), "Reverse AI must not initialize when Normal precedes Reverse.");
    }

    /** Ensures Golden Fruit keeps the player as a snake and disables Reverse AI in Chaos. */
    private static void verifyGoldenFruitSuppressesReverseIdentity() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-chaos-golden-reverse-test").resolve("chaos.score"));
        List<GameMode> ordered = new java.util.ArrayList<>();
        ordered.add(GameModeRegistry.createStandard("reverse"));
        ordered.add(GameModeRegistry.createStandard("golden-fruit"));
        modosDeJogo.CompositeGameMode composition = new modosDeJogo.CompositeGameMode("test-golden-reverse", "Test", ordered);
        composition.init(snake, score);
        ModoReverso reverse = (ModoReverso) ordered.get(0);
        check(reverse.getCobraInimiga().isEmpty(), "Golden Fruit must prevent Reverse AI initialization regardless of selection order.");
        check(composition.usesFood(), "Golden Fruit must preserve regular fruit for a snake player.");
        check(!composition.rendersPlayerAsFood(), "Golden Fruit must prevent fruit-player rendering.");
        check(!composition.isModeActive(reverse), "Reverse must be inactive whenever Golden Fruit is selected.");
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


    /** Verifies random Minefield waves every ten hunted-fruit points and obstacle-safe placement. */
    private static void verifyMinefieldFruitIdentityMilestonesRespectMaze() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-minefield-fruit-score-test").resolve("minefield.score"));
        ModoCampoMinado minefield = new ModoCampoMinado();
        ModoReverso reverse = new ModoReverso();
        ModoObstaculos maze = new ModoObstaculos();
        CompositeGameMode chaos = new CompositeGameMode("minefield-fruit-test", "Minefield Fruit Test",
                List.of(minefield, reverse, maze));
        chaos.init(snake, score);
        check(chaos.rendersPlayerAsFood(), "Reverse must establish fruit identity in this Chaos composition.");

        GameModeContext context = new GameModeContext(snake, score, null, 30, 20, 20,
                chaos::isPositionBlocked, chaos.rendersPlayerAsFood());
        score.addPoints(10);
        chaos.update(context);
        int firstWave = minefield.getMineCount();
        check(firstWave >= 1 && firstWave <= 5, "The first ten-point milestone must create one to five random mines.");
        verifyMinesAvoidWallsObstaclesAndSnake(minefield, maze, snake, 30, 20);

        chaos.update(context);
        check(minefield.getMineCount() == firstWave, "A single score milestone must not create duplicate mine waves.");

        score.addPoints(10);
        chaos.update(context);
        check(minefield.getMineCount() >= firstWave + 1 && minefield.getMineCount() <= firstWave + 5,
                "The next ten-point milestone must create another wave of one to five mines.");
        verifyMinesAvoidWallsObstaclesAndSnake(minefield, maze, snake, 30, 20);
    }

    /** Verifies that any mine triggered by a fruit-identity player ends the round by exception. */
    private static void verifyMinefieldFruitIdentityExplosionIsFatal() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-minefield-fruit-explosion-test").resolve("minefield.score"));
        ModoCampoMinado minefield = new ModoCampoMinado();
        ModoReverso reverse = new ModoReverso();
        CompositeGameMode chaos = new CompositeGameMode("minefield-fruit-explosion-test", "Minefield Fruit Explosion Test",
                List.of(minefield, reverse));
        chaos.init(snake, score);
        check(chaos.rendersPlayerAsFood(), "The explosion test must use fruit identity.");

        java.lang.reflect.Field minesField = ModoCampoMinado.class.getDeclaredField("minas");
        minesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Point> mines = (Set<Point>) minesField.get(minefield);
        mines.add(new Point(snake.getHead()));

        GameModeContext context = new GameModeContext(snake, score, null, 30, 20, 20,
                chaos::isPositionBlocked, chaos.rendersPlayerAsFood());
        chaos.update(context); // Trigger the warning by stepping on the mine.
        boolean exploded = false;
        try {
            for (int tick = 0; tick < 6; tick++) chaos.update(context);
        } catch (MinaExplodiuException expected) {
            exploded = true;
        }
        check(exploded, "A triggered mine must throw MinaExplodiuException for a hunted-fruit player.");
    }

    /** Confirms every generated mine avoids the outer wall, Maze obstacles and snake body. */
    private static void verifyMinesAvoidWallsObstaclesAndSnake(
            ModoCampoMinado minefield, ModoObstaculos maze, Snake snake, int columns, int rows) throws Exception {
        java.lang.reflect.Field minesField = ModoCampoMinado.class.getDeclaredField("minas");
        minesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Point> mines = (Set<Point>) minesField.get(minefield);
        for (Point mine : mines) {
            check(mine.x > 0 && mine.x < columns - 1 && mine.y > 0 && mine.y < rows - 1,
                    "Mines must never spawn on the outer wall.");
            check(!maze.isPositionBlocked(mine), "Mines must never spawn inside an Obstacle Maze block.");
            check(!snake.getBody().contains(mine), "Mines must never spawn inside the snake.");
        }
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


    /** Exercises Boss Rush resources, projectile damage, guardian progression and damage protection. */
    private static void verifyBossRushCombatAndProgression() throws Exception {
        Snake snake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-boss-rush-resource-test").resolve("boss-rush.score"));
        ModoBossRush mode = new ModoBossRush();
        mode.init(snake, score);
        check(mode.usesFood(), "Boss Rush must keep regular fruit enabled.");
        check(mode.getLives() == 3, "Boss Rush must start with three lives.");
        check(mode.getEnergyCharges() == 2, "Boss Rush must start with two energy charges.");
        check(mode.getCurrentBossName().equals("Crimson Hunter"), "Boss Rush must begin with Crimson Hunter.");

        GameModeContext context = new GameModeContext(snake, score, null, 30, 20, 20);
        mode.onKeyPressed(KeyEvent.VK_SPACE);
        check(mode.getEnergyCharges() == 1, "Firing must consume one energy charge.");
        mode.onFoodCollected(context);
        check(mode.getEnergyCharges() == 2, "Collecting regular fruit must restore one energy charge.");

        Snake combatSnake = new Snake(new Point(15, 10), Direction.RIGHT, 4);
        ScoreManager combatScore = new ScoreManager(Files.createTempDirectory("snake-boss-rush-combat-test").resolve("boss-rush.score"));
        ModoBossRush combatMode = new ModoBossRush();
        combatMode.init(combatSnake, combatScore);
        GameModeContext combatContext = new GameModeContext(combatSnake, combatScore, null, 30, 20, 20);

        java.lang.reflect.Field bossField = ModoBossRush.class.getDeclaredField("boss");
        bossField.setAccessible(true);
        Object boss = bossField.get(combatMode);
        setPrivateField(boss, "position", new Point(18, 10));
        setPrivateIntField(boss, "moveCooldown", 1_000);
        setPrivateIntField(boss, "actionCooldown", 1_000);
        setPrivateIntField(boss, "health", 1);

        combatMode.onKeyPressed(KeyEvent.VK_SPACE);
        for (int tick = 0; tick < 4; tick++) combatMode.update(combatContext);
        check(combatMode.getBossesDefeated() == 1, "A lethal projectile must defeat the active guardian.");
        check(combatScore.getCurrentScore() == 5, "Defeating a guardian must award five points.");

        for (int tick = 0; tick < 20; tick++) combatMode.update(combatContext);
        check(combatMode.getCurrentBossName().equals("Volt Sentinel"), "Boss Rush must spawn the next guardian after the transition.");

        boss = bossField.get(combatMode);
        setPrivateField(boss, "position", combatSnake.getHead());
        combatMode.update(combatContext);
        check(combatMode.getLives() == 2, "Contact with a guardian must remove one life.");
        combatMode.update(combatContext);
        check(combatMode.getLives() == 2, "The damage-protection window must prevent consecutive hits.");

        checkThrows(BossRushException.class, () -> combatMode.update((GameModeContext) null),
                "Boss Rush must reject a missing update context.");
    }

    /** Sets one private object field for deterministic mode smoke tests. */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** Sets one private integer field for deterministic mode smoke tests. */
    private static void setPrivateIntField(Object target, String name, int value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
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


    /** Ensures Golden Fruit only uses a free interior cell, never a wall or obstacle cell. */
    private static void verifyGoldenFruitAvoidsWallsAndObstacles() throws Exception {
        Snake snake = new Snake(new Point(4, 4), Direction.RIGHT, 2);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-golden-fruit-test").resolve("golden.score"));
        Set<Point> onlyFreeCell = Set.of(new Point(3, 3));
        GameModeContext context = new GameModeContext(snake, score, new Point(6, 6), 8, 8, 20,
                position -> !onlyFreeCell.contains(position));
        ModoFrutaDourada goldenFruitMode = new ModoFrutaDourada();
        goldenFruitMode.init(snake, score);
        for (int tick = 0; tick < 100; tick++) goldenFruitMode.update(context);

        java.lang.reflect.Field fruitField = ModoFrutaDourada.class.getDeclaredField("goldenFruit");
        fruitField.setAccessible(true);
        Point goldenFruit = (Point) fruitField.get(goldenFruitMode);
        check(goldenFruit != null, "Golden Fruit should spawn when its timer expires.");
        check(goldenFruit.x > 0 && goldenFruit.x < context.getColumns() - 1
                        && goldenFruit.y > 0 && goldenFruit.y < context.getRows() - 1,
                "Golden Fruit must never spawn on the outer wall cells.");
        check(!snake.getBody().contains(goldenFruit), "Golden Fruit must not spawn inside the snake.");
        check(!goldenFruit.equals(context.getFood()), "Golden Fruit must not overlap regular food.");
        check(!context.isPositionBlocked(goldenFruit), "Golden Fruit must not spawn inside an obstacle from another selected mode.");
        check(goldenFruit.equals(new Point(3, 3)), "Golden Fruit should use the only available interior cell.");
    }

    /** Ensures Portal composition checks every translated segment against obstacles from other modes. */
    private static void verifyPortalAvoidsCompositeObstacles() throws Exception {
        Snake snake = new Snake(new Point(8, 2), Direction.RIGHT, 3);
        ScoreManager score = new ScoreManager(Files.createTempDirectory("snake-portal-obstacle-test").resolve("portal.score"));

        // Only this horizontal three-cell area is free for the translated snake.
        Set<Point> freeCells = Set.of(
                new Point(3, 5), new Point(4, 5), new Point(5, 5),
                new Point(8, 8), new Point(8, 7));
        ModoPortal portal = new ModoPortal(10, 10);
        CompositeGameMode chaosComposition = new CompositeGameMode("chaos-test", "Chaos test",
                List.of(portal, new TestObstacleMode(freeCells)));
        chaosComposition.init(snake, score);

        GameModeContext initialFood = new GameModeContext(snake, score, new Point(8, 8), 10, 10, 20,
                chaosComposition::isPositionBlocked);
        chaosComposition.onFoodPlaced(initialFood);

        GameModeContext replacementFood = new GameModeContext(snake, score, new Point(8, 7), 10, 10, 20,
                chaosComposition::isPositionBlocked);
        chaosComposition.onFoodPlaced(replacementFood);

        Set<Point> expectedBody = Set.of(new Point(3, 5), new Point(4, 5), new Point(5, 5));
        check(new HashSet<>(snake.getBody()).equals(expectedBody),
                "Portal must find the only free destination area in the combined obstacle map.");
        for (Point segment : snake.getBody()) {
            check(!chaosComposition.isPositionBlocked(segment),
                    "No translated snake segment may overlap an obstacle from another selected mode.");
            check(segment.x > 0 && segment.x < 9 && segment.y > 0 && segment.y < 9,
                    "Portal must keep every snake segment inside the board's inner area.");
        }
        check(!snake.getBody().contains(replacementFood.getFood()), "Portal must not teleport the snake onto the replacement fruit.");
    }

    /** Minimal deterministic obstacle mode used to exercise composite collision rules. */
    private static final class TestObstacleMode implements GameMode {
        private final Set<Point> freeCells;
        private TestObstacleMode(Set<Point> freeCells) { this.freeCells = new HashSet<>(freeCells); }
        @Override public String getId() { return "test-obstacles"; }
        @Override public String getName() { return "Test Obstacles"; }
        @Override public void init(Snake snake, ScoreManager score) { }
        @Override public boolean isPositionBlocked(Point position) { return position != null && !freeCells.contains(position); }
    }

    /** Verifies LAN player-profile sanitization, supported votes and port validation. */
    private static void verifyMultiplayerContracts() {
        MultiplayerProfile profile = new MultiplayerProfile("Player|One", 0xFF00FF00, 0xFF008800, 0xFF000000);
        check("PlayerOne".equals(profile.name()), "LAN player names must be sanitized before entering the protocol.");
        check(MultiplayerRuleset.fromWire("COMBO") == MultiplayerRuleset.COMBO, "The multiplayer vote catalog must resolve supported rulesets.");
        checkThrows(MultiplayerException.class, () -> MultiplayerRuleset.fromWire("BOSS_RUSH"),
                "Single-player Boss Rush must not be accepted as a multiplayer ruleset.");
        checkThrows(MultiplayerLobbyException.class, () -> new multiplayer.LanServer(80),
                "LAN server ports below 1024 must be rejected.");
    }

    /** Runs a loopback LAN match to verify handshakes, majority voting and host tie-break. */
    private static void verifyLanMajorityVoteAndSnapshots() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
        LanServer server = new LanServer(port);
        CountDownLatch hostWelcome = new CountDownLatch(1);
        CountDownLatch clientWelcome = new CountDownLatch(1);
        CountDownLatch votesReady = new CountDownLatch(1);
        CountDownLatch hostStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        CountDownLatch snapshotReceived = new CountDownLatch(1);
        AtomicReference<String> selectedMode = new AtomicReference<>();
        LanClient host = null;
        LanClient guest = null;
        try {
            server.start();
            host = new LanClient(new LanClient.Listener() {
                @Override public void onWelcome(int id, boolean isHost) {
                    check(id == 1 && isHost, "The first LAN connection must be the host.");
                    hostWelcome.countDown();
                }
                @Override public void onLobby(String lobby) {
                    if (lobby.contains("1:CLASSIC") && lobby.contains("2:COMBO")) votesReady.countDown();
                }
                @Override public void onStarted(String mode) {
                    selectedMode.set(mode);
                    hostStarted.countDown();
                }
                @Override public void onSnapshot(String snapshot) { snapshotReceived.countDown(); }
            });
            guest = new LanClient(new LanClient.Listener() {
                @Override public void onWelcome(int id, boolean isHost) {
                    check(id == 2 && !isHost, "The second LAN connection must be a guest.");
                    clientWelcome.countDown();
                }
                @Override public void onStarted(String mode) { clientStarted.countDown(); }
            });
            MultiplayerProfile hostProfile = new MultiplayerProfile("Host", 0xFF00FF00, 0xFF008800, 0xFF000000);
            MultiplayerProfile guestProfile = new MultiplayerProfile("Guest", 0xFFFF8800, 0xFFCC5500, 0xFF000000);
            host.connect("127.0.0.1", port, hostProfile);
            guest.connect("127.0.0.1", port, guestProfile);
            check(hostWelcome.await(5, TimeUnit.SECONDS), "LAN host handshake timed out.");
            check(clientWelcome.await(5, TimeUnit.SECONDS), "LAN guest handshake timed out.");
            host.vote(MultiplayerRuleset.CLASSIC);
            guest.vote(MultiplayerRuleset.COMBO);
            check(votesReady.await(5, TimeUnit.SECONDS), "LAN votes were not registered by the host.");
            host.startMatch();
            check(hostStarted.await(5, TimeUnit.SECONDS), "The host did not receive the match-start packet.");
            check(clientStarted.await(5, TimeUnit.SECONDS), "The guest did not receive the match-start packet.");
            check(snapshotReceived.await(5, TimeUnit.SECONDS), "The server did not broadcast a match snapshot.");
            check("CLASSIC".equals(selectedMode.get()), "A tied vote must be resolved using the host's vote.");
        } finally {
            if (host != null) host.close();
            if (guest != null) guest.close();
            server.close();
        }
    }

    /** Throws an assertion with a readable message when a test condition is false. */
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}