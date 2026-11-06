# Code Documentation

This document is the English technical reference for the current source tree. It explains the responsibility of each package, the main classes, the game-loop sequence, and the extension points used to add or modify game modes. Public classes and important methods also contain inline Javadoc comments in English.

## 1. Application architecture

The application follows an object-oriented design with a shared game loop and mode-specific rules. A mode is not implemented as a branch inside the Swing panel. Instead, it is an object that implements `GameMode` and receives controlled access to the current round through `GameModeContext`.

The main runtime path is:

1. `tela.Main` creates the application window on Swing's Event Dispatch Thread (EDT).
2. `tela.MenuInicial` displays the main menu and listens for the Konami Code sequence.
3. `tela.TelaSelecaoModoJogo` obtains fresh instances of the ten standard modes from `GameModeRegistry`.
4. After the Konami Code is entered, `extras.TelaExtras` obtains the five exclusive modes from the same registry.
5. `tela.GamePanel` creates the player `Snake`, creates a mode-specific `ScoreManager`, initializes the selected mode and starts a Swing timer.
6. On each timer tick, `GamePanel` moves the snake, checks shared collisions, processes regular food collection and calls the active mode's update hook.
7. The mode updates its own entities and may draw overlays, award points, change the desired tick delay or request the end of the round.
8. `tela.GameOverScreen` displays the final score and the best score belonging to that specific mode.

### Design boundaries

- The Swing screens own presentation and user interaction.
- `GamePanel` owns the shared round lifecycle and common movement rules.
- Each `GameMode` owns its unique gameplay rules and entities.
- `Snake` owns the player's body and movement state.
- `ScoreManager` owns current score and persistent high score for one stable mode ID.
- `GameModeRegistry` owns the catalog and construction of game modes.

## 2. Core game-mode contract

### `modosDeJogo.GameMode`

This interface is the extension point for every game mode. The required methods provide a stable identifier, a display name and an initialization entry point. Default hooks cover:

- Per-tick updates and mode-specific drawing.
- Regular-food placement and collection events.
- Keyboard input and direction transformation.
- Whether regular food is enabled or the player is drawn as a hunted fruit.
- Edge wraparound and mode-specific blocked cells.
- Dynamic timer delay and mode-triggered game-over state.

A new mode should override only the capabilities it needs. It should not depend on `GamePanel`, `JFrame` or another Swing component.

### `modosDeJogo.GameModeContext`

This state-transfer object is passed to update and food-placement hooks. It exposes the player snake, the active score manager, a defensive copy of the regular-food position, board dimensions and cell size. It also provides `isPositionBlocked(Point)`, backed by the combined obstacle map of the active mode (including every child of a composite such as Chaos). A mode can request game over without coupling itself to the UI layer.

### `modosDeJogo.GameModeRegistry`

The central factory returns fresh instances of the ten standard modes and five exclusive modes. Fresh objects ensure that mode state from a previous round does not leak into a new selection.

### `modosDeJogo.CompositeGameMode`

This class composes several mode objects into one round. It delegates initialization, updates, drawing, food callbacks and keyboard input to its children in list order. The context-based food-placement callback is forwarded unchanged, so each child can query the combined obstacle map. It combines capabilities such as edge wrapping, blocked cells, timer delay and game-over state.

For the Normal/Reverse conflict, the first of those two modes in the composition order determines whether regular food is enabled and whether the player is rendered as a snake or as the hunted fruit.

### `modosDeJogo.ModoCaos`

Chaos randomly selects zero to ten distinct standard modes. An intensity of zero uses the base Normal experience without adding extra mechanics. Intensities from one through ten activate that many distinct standard modes. The selection is re-rolled whenever a new Chaos round is initialized. If both Normal and Reverse are selected, their relative order is randomized with an independent 50/50 decision.

## 3. Standard game modes

| Class | Stable ID | Responsibility |
|---|---|---|
| `ModoPadrao` | `normal` | Classic Snake rules; the shared game loop handles food, movement and collisions. |
| `ModoCampoMinado` | `minefield` | Mine generation, warning animation, 4x4 blast evaluation and body damage. |
| `ModoPortal` | `portal` | Translates the complete snake to a safe board position after a new fruit is placed. |
| `ModoReverso` | `reverse` | Enemy-snake AI, pursuit/dispersion/confusion states, difficulty progression and survival scoring. |
| `ModoVelocidade` | `speed` | Reduces the timer delay as the score increases, with a minimum delay. |
| `ModoEspelho` | `mirror-controls` | Maps each requested cardinal direction to its opposite. |
| `ModoObstaculos` | `obstacles` | Generates static blocks and reports blocked cells to the common collision layer. |
| `ModoSemParedes` | `wraparound` | Enables movement from one board edge to the opposite edge. |
| `ModoCombo` | `combo` | Tracks consecutive food collections and awards increasing bonus points. |
| `ModoFrutaDourada` | `golden-fruit` | Periodically creates a bonus fruit that awards extra points and growth. |

### Standard-mode implementation notes

- `ModoCampoMinado` keeps mine coordinates in a set. When a mine is triggered, it first displays a warning. At detonation, a head hit ends the round; each non-head body segment in the blast removes one tail segment and one current-score point.
- `ModoPortal` moves the complete body by the same translation vector, preserving the snake's shape. It randomizes candidate head positions and validates every translated segment against the board walls, the replacement fruit and all obstacles contributed by the active composite before applying the teleport. If no fully free area exists, the mode reports a `PortalException` rather than placing the snake inside an obstacle.
- `ModoFrutaDourada` searches only interior board cells and rejects the snake, regular food and every cell reported as blocked by the active composite. This keeps the bonus fruit away from outer walls and obstacles even when Golden Fruit is selected alongside other modes in Chaos.
- `ModoReverso` keeps its enemy snakes and their AI state private to that mode. The common panel does not need to know the enemy implementation.
- `ModoVelocidade` changes the Swing timer delay through `getTickDelay`, not by creating a second timer.
- `ModoEspelho` changes input through `adjustDirection`, leaving direction validation inside `Snake`.
- `ModoObstaculos` exposes obstacle cells through `isPositionBlocked`, which is also used when placing regular food.
- `ModoSemParedes` requests wraparound from the shared movement operation.
- `ModoCombo` uses the food-collected callback and its own inactivity counter.
- `ModoFrutaDourada` owns a separate bonus-fruit coordinate and does not replace the regular fruit.

## 4. Konami-exclusive game modes

| Class | Stable ID | Responsibility |
|---|---|---|
| `ModoCaos` | `chaos` | Randomly composes standard mechanics at a selected intensity. |
| `ModoRunGun` | `run-and-gun` | Hostile fruit AI, player/enemy projectiles, aiming, evasion, hit detection and projectile cancellation. |
| `ModoBossRush` | `boss-rush` | Boss entities pursue the player and end the round on contact. |
| `ModoContrarrelogio` | `time-attack` | Ninety-second countdown; the player tries to collect as much regular fruit as possible. |
| `ModoGlitch` | `glitch` | Periodically rotates the requested movement direction by a random quarter-turn. |

### Run & Gun entity lifecycle

`ModoRunGun` contains two private entity types:

- `FruitEnemy` stores an opponent's grid position and its movement/fire cooldowns.
- `Projectile` stores its grid position, movement vector, ownership and previous position.

The mode's update method lets fruit opponents choose evasive movement and fire toward the player. Projectiles move on alternating game ticks. Any two projectiles are removed when they occupy the same cell or cross paths during a movement step. Player projectiles destroy fruit opponents and award points; enemy projectiles that hit the snake request game over.

The Space key fires one player projectile in the snake's current direction. Movement remains controlled by the normal arrow-key or WASD input layer.

## 5. Player movement and collision state

### `sistemaDeMovimentacaoGeral.Snake`

The snake stores its body in a head-to-tail deque of grid coordinates. Its constructor creates contiguous body segments behind the head. The class owns:

- Direction validation, including rejection of an immediate reverse turn.
- One-cell movement and optional edge wraparound.
- Deferred growth on the next movement step.
- Tail shrinking without deleting the head.
- Self-collision checks against the body.
- Whole-body translation for Portal mode.
- The currently selected visual skin.

`shrink(int)` returns the actual number of removed segments. This allows damage mechanics to keep body length and score changes synchronized.

## 6. Independent score persistence

### `pontuacao.ScoreManager`

Every selected mode is assigned its stable ID through `GameMode.getId()`. The ID is sanitized and used as the file name under:

`~/.snakeultimate/scores/<mode-id>.score`

The current score can increase or decrease, but never below zero. The high score remains the best result for that mode and is written when the round ends. The Normal mode imports the legacy `~/.snake_highscore` value when no new Normal-mode record exists. Chaos has its own `chaos.score` record, separate from all standard modes.

## 7. User-interface package

- `Main`: application entry point, main frame and centralized screen navigation.
- `MenuInicial`: main menu and Konami Code recognition.
- `TelaSelecaoModoJogo`: scrollable catalog of the ten standard modes.
- `extras.TelaExtras`: catalog of five exclusive modes and the Chaos intensity prompt.
- `GamePanel`: board rendering, shared game loop, common input, common collision checks and transition to game over.
- `GameOverScreen`: final score, mode-specific record, restart and main-menu actions.
- `TelaPersonalizacaoCobra`: color customization and live snake preview.
- `TelaSelecaoTema`: visual theme selection.
- `TelaConfiguracaoMapa`: map appearance configuration and preview.
- `TelaPreJogo`: legacy screen retained for compatibility; current navigation uses the main menu and mode selectors.

## 8. Configuration, appearance and support packages

- `configuracoes.GameConfig`: board rows, columns, cell size and base timer delay.
- `configuracoesGerais.TemaVisual`: visual theme data.
- `configuracoesGerais.Temas`: predefined Classic and Neon themes.
- `customizacao.UserSettings`: persistent user preferences.
- `personalizacaoCobra.SnakeSkin`: snake appearance data.
- `personalizacaoCobra.SnakeSkinManager`: selected-skin state and persistence.
- `personalizacaoCobra.SnakeCustomizer`: helpers for predefined and image-based skins.
- `personalizacaoCobra.SkinType`: supported skin categories.
- `exceptions`: a shared `GameModeException` base plus dedicated exceptions for individual modes, score, movement, persistence and navigation.

## 9. Minefield damage rule

The blast rectangle is evaluated after the warning animation. If the head is inside the rectangle, `ModoCampoMinado` throws `MinaExplodiuException` and the common panel ends the round. Otherwise, the mode counts every non-head segment inside the blast rectangle, calls `Snake.shrink(hitSegments)` and calls `ScoreManager.removePoints(hitSegments)`. The method returns the actual number of segments removed, and the body never becomes empty.

## 10. Game-mode exception hierarchy

Every mode-specific exception extends `exceptions.GameModeException`, which itself extends `RuntimeException`. This keeps mode failures compatible with the shared `GamePanel` recovery path while allowing callers and tests to identify the failing mechanic precisely.

| Mode | Exception type |
|---|---|
| Chaos | `ChaosModeException` |
| Run & Gun | `RunAndGunException` |
| Boss Rush | `BossRushException` |
| Time Attack | `TimeAttackException` |
| Glitch | `GlitchModeException` |
| Combo | `ComboModeException` |
| Golden Fruit | `GoldenFruitException` |
| Obstacle Maze | `ObstacleModeException` |
| Mirrored Controls | `MirroredControlsException` |
| Progressive Speed | `SpeedModeException` |
| No Walls | `NoWallsException` |

These exceptions represent invalid lifecycle inputs, malformed update contexts, impossible mode-specific operations or violated invariants. Expected gameplay events—such as a projectile hitting a fruit, a boss reaching the player, a countdown expiring, or a player colliding with an obstacle—remain ordinary game-state transitions and are not modeled as exceptions. Chaos intensity must be explicitly within the supported 0–10 range; invalid values are rejected instead of silently clamped.

## 11. Adding a new game mode

1. Create a class in `modosDeJogo` and implement `GameMode`.
2. Give it a stable, unique ID from `getId()` and a display name from `getName()`.
3. Initialize all per-round fields in `init` so restarting cannot retain stale state.
4. Put the mode's rules and entities in that class. Use `GameModeContext` for the snake, score and board dimensions.
5. Override only the required hooks, such as `update`, `draw`, `onKeyPressed`, `onFoodCollected`, `isPositionBlocked` or `getTickDelay`.
6. Register the new factory in `GameModeRegistry` and add its enum value to `GameModeType`.
7. Add its UI description and update this document and the README.
8. Add or extend smoke tests for its core mechanics.

## 12. Build and verification

The application targets Java 17 or newer and uses only the Java standard library. Run `scripts/test.sh` from the project root to compile the source and execute the dependency-free smoke tests. The tests cover the catalog size, unique mode IDs, Chaos boundaries and Normal/Reverse ordering probability, snake continuity/shrinking, independent score files, Minefield damage, Run & Gun lifecycle and projectile cancellation, Golden Fruit wall/obstacle avoidance, Portal relocation with a composite obstacle map, and invalid-input handling for each of the eleven mode-specific exceptions.
