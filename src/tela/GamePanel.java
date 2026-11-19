package tela;

import configuracoes.GameConfig;
import configuracoesGerais.TemaVisual;
import customizacao.UserSettings;
import modosDeJogo.GameMode;
import modosDeJogo.GameModeContext;
import modosDeJogo.ModoPadrao;
import pontuacao.ScoreManager;
import personalizacaoCobra.SnakeSkin;
import personalizacaoCobra.SnakeSkinManager;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import javax.swing.JFrame;

/**
 * Main board component. It owns the common movement loop and delegates every
 * optional rule, enemy, obstacle and visual overlay to the active GameMode.
 */
public class GamePanel extends JPanel implements ActionListener {
    private final int cellSize = GameConfig.CELL_SIZE;
    private final int columns = GameConfig.COLS;
    private final int rows = GameConfig.ROWS;
    private final int width = cellSize * columns;
    private final int height = cellSize * rows;

    private final GameMode gameMode;
    private final Random random = new Random();
    private final JPanel boardCanvas;
    private final JPanel modeHudCanvas;
    private final JLabel modeLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel recordLabel = new JLabel();
    private Snake snake;
    private Timer timer;
    private Point food;
    private boolean running;
    private ScoreManager score;

    /** Starts the classic Normal mode when no explicit mode is supplied. */
    public GamePanel() { this(new ModoPadrao()); }

    /** Creates a board for the selected mode and initializes its first round. */
    public GamePanel(GameMode mode) {
        if (mode == null) throw new IllegalArgumentException("Game mode cannot be null.");
        gameMode = mode;
        setLayout(new java.awt.BorderLayout(0, 0));
        setPreferredSize(new Dimension(width, height + 105));
        setFocusable(true);
        setBackground(Color.BLACK);
        JPanel commonHud = new JPanel(new java.awt.GridLayout(1, 3, 12, 0));
        commonHud.setBackground(Color.BLACK);
        commonHud.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        styleHudLabel(modeLabel); styleHudLabel(scoreLabel); styleHudLabel(recordLabel);
        commonHud.add(modeLabel); commonHud.add(scoreLabel); commonHud.add(recordLabel);
        modeHudCanvas = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                gameMode.drawHud(graphics, getWidth(), getHeight());
            }
        };
        modeHudCanvas.setBackground(Color.BLACK);
        modeHudCanvas.setPreferredSize(new Dimension(width, 70));
        JPanel header = new JPanel(new java.awt.BorderLayout(0, 0));
        header.setBackground(Color.BLACK);
        header.add(commonHud, java.awt.BorderLayout.NORTH);
        header.add(modeHudCanvas, java.awt.BorderLayout.CENTER);
        boardCanvas = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                paintBoard(graphics);
            }
        };
        boardCanvas.setBackground(Color.BLACK);
        boardCanvas.setPreferredSize(new Dimension(width, height));
        boardCanvas.setFocusable(false);
        boardCanvas.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(header, java.awt.BorderLayout.NORTH);
        add(boardCanvas, java.awt.BorderLayout.CENTER);
        setupKeyListener();
        initGame();
        refreshHud();
    }

    /** Creates the snake, mode-specific score manager and Swing game timer. */
    private void initGame() {
        if (columns <= 0 || rows <= 0) throw new IllegalStateException("Invalid board dimensions.");
        Point start = new Point(columns / 2, rows / 2);
        snake = new Snake(start, Direction.RIGHT, 4);
        snake.setSkin(SnakeSkinManager.getSkin());
        score = new ScoreManager(gameMode.getId());
        gameMode.init(snake, score);
        running = true;
        if (gameMode.usesFood()) placeFood(); else food = null;
        timer = new Timer(gameMode.getTickDelay(score.getCurrentScore()), this);
        timer.start();
    }

    /** Routes key input to mode-specific actions and common movement controls. */
    private void setupKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (!running && event.getKeyCode() == KeyEvent.VK_SPACE) { restartGame(); return; }
                gameMode.onKeyPressed(event.getKeyCode());
                Direction requested = switch (event.getKeyCode()) {
                    case KeyEvent.VK_UP, KeyEvent.VK_W -> Direction.UP;
                    case KeyEvent.VK_DOWN, KeyEvent.VK_S -> Direction.DOWN;
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> Direction.LEFT;
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> Direction.RIGHT;
                    default -> null;
                };
                if (requested != null) snake.setDirection(gameMode.adjustDirection(requested));
            }
        });
    }

    /** Advances one game tick, applies common collisions and then updates the active mode. */
    @Override public void actionPerformed(ActionEvent event) {
        if (!running) return;
        try {
            Point newHead = snake.move(gameMode.wrapsAtEdges(), columns, rows);
            if (!isInsideBoard(newHead) || gameMode.isPositionBlocked(newHead) || snake.collidesWithSelf(newHead)) {
                gameOver(); return;
            }

            GameModeContext context;
            if (gameMode.usesFood() && food != null && newHead.equals(food)) {
                snake.grow();
                score.addPoints(1);
                placeFood();
                context = createContext();
                gameMode.onFoodCollected(context);
            }

            context = createContext();
            gameMode.update(context);
            food = context.getFood();
            if (context.isGameOverRequested() || gameMode.isGameOver()) { gameOver(); return; }

            timer.setDelay(gameMode.getTickDelay(score.getCurrentScore()));
            refreshHud(); boardCanvas.repaint(); modeHudCanvas.repaint();
        } catch (RuntimeException exception) {
            // Mode-specific failures end the round without leaving the Swing timer running.
            gameOver();
            exception.printStackTrace();
        }
    }

    /** Creates an isolated update context for the active mode. */
    private GameModeContext createContext() {
        return new GameModeContext(snake, score, food, columns, rows, cellSize,
                gameMode::isPositionBlocked, gameMode.rendersPlayerAsFood());
    }

    /** Places regular food in a cell not occupied by the snake or mode obstacles. */
    private void placeFood() {
        if (snake.getBody().size() >= columns * rows) throw new IllegalStateException("No free board cell remains.");
        for (int attempt = 0; attempt < columns * rows * 2; attempt++) {
            Point candidate = new Point(random.nextInt(columns), random.nextInt(rows));
            if (!snake.getBody().contains(candidate) && !gameMode.isPositionBlocked(candidate)) {
                food = candidate;
                gameMode.onFoodPlaced(createContext());
                return;
            }
        }
        for (int y = 0; y < rows; y++) for (int x = 0; x < columns; x++) {
            Point candidate = new Point(x, y);
            if (!snake.getBody().contains(candidate) && !gameMode.isPositionBlocked(candidate)) {
                food = candidate;
                gameMode.onFoodPlaced(createContext());
                return;
            }
        }
        throw new IllegalStateException("No valid position is available for food.");
    }

    /** Tests whether a coordinate is inside the rectangular board. */
    private boolean isInsideBoard(Point point) {
        return point.x >= 0 && point.x < columns && point.y >= 0 && point.y < rows;
    }

    /** Stops the game timer, persists the mode record and opens the game-over screen. */
    private void gameOver() {
        if (!running) return;
        running = false;
        if (timer != null) timer.stop();
        try { score.saveHighScore(); }
        catch (RuntimeException exception) { System.err.println(exception.getMessage()); }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (!(window instanceof JFrame frame)) return;
        GameOverScreen screen = new GameOverScreen(frame, score, gameMode);
        screen.setPreferredSize(new Dimension(width, height + 105));
        frame.setContentPane(screen);
        frame.pack(); frame.setLocationRelativeTo(null); frame.revalidate(); frame.repaint();
    }

    /** Recreates the selected mode's round without returning to the mode selector. */
    private void restartGame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (!(window instanceof JFrame frame)) return;
        GamePanel nextRound = new GamePanel(gameMode);
        frame.setContentPane(nextRound); frame.pack(); frame.setLocationRelativeTo(null);
        frame.revalidate(); frame.repaint(); nextRound.requestFocusInWindow();
    }

    /** Returns the current user-selected visual theme. */
    private TemaVisual getTheme() { return UserSettings.getTema(); }

    /** Paints only board contents; status labels are rendered in separate HUD panels. */
    private void paintBoard(Graphics graphics) {
        TemaVisual theme = getTheme();
        boardCanvas.setBackground(theme.getFundo());
        drawGrid(graphics, theme);
        if (gameMode.usesFood() && food != null) drawFood(graphics, theme);
        if (gameMode.rendersPlayerAsFood()) drawPlayerAsFood(graphics, theme);
        else drawSnake(graphics, theme);
        gameMode.draw(graphics, cellSize);
    }
    /** Styles a shared session label. */
    private void styleHudLabel(JLabel label) {
        label.setForeground(Color.WHITE);
        label.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
    }
    /** Refreshes common mode, score and record labels after each tick. */
    private void refreshHud() {
        modeLabel.setText("MODO: " + gameMode.getName());
        scoreLabel.setText("PONTUAÇÃO: " + score.getCurrentScore());
        recordLabel.setText("RECORDE: " + score.getHighScore());
    }

    /** Draws the board grid using the selected theme's grid color. */
    private void drawGrid(Graphics graphics, TemaVisual theme) {
        graphics.setColor(theme.getGrade());
        for (int x = 0; x <= width; x += cellSize) graphics.drawLine(x, 0, x, height);
        for (int y = 0; y <= height; y += cellSize) graphics.drawLine(0, y, width, y);
    }

    /** Draws the regular collectible fruit. */
    private void drawFood(Graphics graphics, TemaVisual theme) {
        graphics.setColor(theme.getComida());
        graphics.fillOval(food.x * cellSize, food.y * cellSize, cellSize, cellSize);
    }

    /** Draws the player's snake with the configured skin and theme outline. */
    private void drawSnake(Graphics graphics, TemaVisual theme) {
        SnakeSkin skin = snake.getSkin();
        Color headColor = skin.getPrimaryColor() != null ? skin.getPrimaryColor() : Color.GREEN.brighter();
        Color bodyColor = skin.getSecondaryColor() != null ? skin.getSecondaryColor() : Color.GREEN.darker();
        boolean first = true;
        for (Point segment : snake.getBody()) {
            int x = segment.x * cellSize, y = segment.y * cellSize;
            graphics.setColor(first ? headColor : bodyColor);
            graphics.fillRect(x, y, cellSize, cellSize);
            Color outlineColor = skin.getOutlineColor() != null ? skin.getOutlineColor() : theme.getBordaCobra();
            graphics.setColor(outlineColor);
            graphics.drawRect(x, y, cellSize, cellSize);
            first = false;
        }
    }

    /** Renders the player as the hunted fruit in Reverse mode. */
    private void drawPlayerAsFood(Graphics graphics, TemaVisual theme) {
        Point player = snake.getHead();
        graphics.setColor(theme.getComida());
        graphics.fillOval(player.x * cellSize, player.y * cellSize, cellSize, cellSize);
        graphics.setColor(Color.BLACK);
        graphics.drawOval(player.x * cellSize, player.y * cellSize, cellSize, cellSize);
    }

    /** Draws the active mode name, current score and mode-specific best score. */
    private void drawHud(Graphics graphics, TemaVisual theme) {
        graphics.setColor(theme.getTexto()); graphics.setFont(theme.getFonte());
        graphics.drawString("Pontuação: " + score.getCurrentScore(), 10, 20);
        graphics.drawString("Recorde: " + score.getHighScore(), 10, 40);
        graphics.drawString("Modo: " + gameMode.getName(), 10, 60);
    }
}