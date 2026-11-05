# Snake Game Ultimate

An expanded Snake game built with Java and Swing. The project focuses on object-oriented design, modular game rules, reusable mode contracts, customizable visuals and independent score records.

## Features

- Java Swing desktop interface with keyboard controls.
- Ten standard game modes and five Konami Code-exclusive modes.
- Custom snake skins and visual themes.
- Independent high scores for each mode.
- A modular `GameMode` contract that allows new mechanics to be added without turning the game panel into a collection of mode-specific conditionals.
- Minefield blast damage that removes both score points and the corresponding number of snake tail segments.

## Standard game modes

| Mode | Description |
|---|---|
| Normal | Classic Snake rules. Collect fruit, grow and avoid collisions. |
| Minefield | Mines appear during the round. Their 4x4 blast can end the game or damage the snake. |
| Portal | Collecting fruit relocates the complete snake to a safe position. |
| Reverse | The player becomes a hunted fruit while AI-controlled snakes pursue them. |
| Progressive Speed | The game gets faster as the score increases. |
| Mirrored Controls | Directional input is mirrored to its opposite direction. |
| Obstacle Maze | Static blocks are placed throughout the board. |
| Wraparound | Leaving one board edge makes the snake re-enter from the opposite edge. |
| Combo | Consecutive fruit collections grant increasing bonus points. |
| Golden Fruit | A bonus fruit appears periodically and grants extra points and growth. |

## Secret modes

Enter the following sequence while the main menu is focused to open the secret selector:

`Up, Up, Down, Down, Left, Right, Left, Right, B, A`

| Mode | Description |
|---|---|
| Chaos | Randomly activates 0 to 10 distinct standard modes simultaneously and re-rolls the combination on each new round. |
| Run & Gun | Press `Space` to fire at hostile fruit. Fruit opponents evade the player and shoot back. |
| Boss Rush | Survive against boss cores that steadily hunt the snake. |
| Time Attack | Collect as much fruit as possible before the 90-second timer expires. |
| Glitch | Directional controls periodically rotate by a random quarter-turn. |

### Chaos rules

When Chaos is selected, the player chooses an intensity from 0 to 10. Zero activates the base Normal experience; values from 1 to 10 randomly select that many distinct standard modes. If both Normal and Reverse are selected, their execution order is randomized independently with a 50/50 chance. The first one in the composition order determines whether the round uses regular food and whether the player is drawn as a snake or as the hunted fruit.

### Run & Gun rules

- Use the arrow keys or `W`, `A`, `S`, `D` to move.
- Press `Space` to fire a player projectile in the current movement direction.
- Hostile fruit opponents move to evade the snake and periodically fire aimed projectiles.
- Any two projectiles are removed when they collide in the same cell or cross paths during a movement tick.
- A player projectile destroys a fruit opponent and awards 10 points. An enemy projectile hitting the snake ends the round.

## Controls

- Movement: Arrow keys or `W`, `A`, `S`, `D`.
- Run & Gun firing: `Space`.
- Restart after a game-over screen: select **Play Again**.

## Architecture

The project uses object-oriented composition and polymorphism:

- `GameMode` defines the common lifecycle and optional capabilities of a mode.
- `GameModeContext` exposes round state to mode implementations without coupling them to Swing components.
- `GameModeRegistry` is the single catalog for standard and secret modes.
- `CompositeGameMode` coordinates multiple modes in one round.
- `ModoCaos` builds a randomized composite from the ten standard modes.
- `GamePanel` owns the shared movement loop, common collisions, food placement, rendering and score display. Mode-specific rules remain inside mode classes.
- `Snake` owns body movement, growth, shrinking, wraparound movement and teleportation.
- `ScoreManager` stores one record per stable mode identifier.
- Dedicated mode exceptions inherit from `GameModeException`, allowing precise diagnostics while preserving the shared game-over recovery path.

See [Code Documentation](docs/CODE_DOCUMENTATION.md) for package responsibilities, class details and the game-tick lifecycle.

## Score storage

Each mode has its own record file under:

`~/.snakeultimate/scores/<mode-id>.score`

The Normal mode imports the old `~/.snake_highscore` value when no new Normal record exists, so a previous classic-mode record is preserved. Chaos has its own record, separate from the ten standard modes.

## Requirements

- JDK 17 or newer.
- Any Java IDE is supported. Eclipse project metadata is included.
- No third-party libraries are required; the interface uses Java Swing.

## Build and run

From the project root on macOS/Linux:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out tela.Main
```

On Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
java -cp out tela.Main
```

In Eclipse, import the folder as an existing Java project, select a JDK 17+ runtime and run `tela.Main`.

## Run the smoke tests

The project includes a dependency-free smoke test:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
javac -encoding UTF-8 -cp out -d out tests/ModeSystemSmokeTest.java
java -ea -Djava.awt.headless=true -cp out ModeSystemSmokeTest
```

The tests verify the mode catalog sizes, Chaos composition and 50/50 precedence, strict Chaos intensity validation, snake body continuity and shrinking, independent score files, Minefield blast damage, Run & Gun projectile behavior, and the dedicated exception contract for all eleven new modes.

## Project status

This release contains the expanded mode system and the corrections documented above. The Java source is organized under `src/`; compiled output, IDE caches and personal score files should not be committed.
