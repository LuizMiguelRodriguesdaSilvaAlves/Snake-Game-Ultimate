package multiplayer;

import tela.Main;
import configuracoes.GameConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import sistemaDeMovimentacaoGeral.Direction;

/** Client-side renderer and input surface for the host-authoritative LAN match. */
public final class MultiplayerGamePanel extends JPanel {
    private record PlayerView(int id, String name, int headRgb, int bodyRgb,
            int outlineRgb, int score, boolean alive, List<Point> body) { }

    private final LanServer server;
    private final LanClient client;
    private final boolean host;
    private final BoardCanvas board = new BoardCanvas();
    private final JLabel status = new JLabel("Waiting for host...", SwingConstants.CENTER);
    private final JButton restartButton = new JButton("Restart match");
    private final Timer repaintTimer;
    private volatile String snapshot;
    private volatile boolean gameOver;
    private volatile int countdown = -1;
    private volatile Color backgroundColor = Color.BLACK;
    private volatile Color gridColor = new Color(55, 55, 55);
    private volatile Color foodColor = Color.RED;

    public MultiplayerGamePanel(LanServer server, LanClient client, boolean host) {
        this.server = server;
        this.client = client;
        this.host = host;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setFocusable(true);
        board.setPreferredSize(new Dimension(GameConfig.COLS * GameConfig.CELL_SIZE,
                GameConfig.ROWS * GameConfig.CELL_SIZE + 58));

        JPanel footer = new JPanel(new BorderLayout(12, 6));
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        footer.setBackground(Color.BLACK);
        status.setForeground(Color.WHITE);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(Color.BLACK);
        restartButton.setVisible(false);
        restartButton.addActionListener(event -> requestRestart());
        JButton leaveButton = new JButton("Leave match");
        leaveButton.addActionListener(event -> leaveMatch());
        actions.add(restartButton);
        actions.add(leaveButton);
        footer.add(status, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);

        add(board, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        installControls();
        repaintTimer = new Timer(80, event -> board.repaint());
        repaintTimer.start();
    }

    /** Receives an authoritative snapshot from the host server. */
    public void acceptSnapshot(String line) {
        snapshot = line;
        board.repaint();
    }

    /** Applies countdown and map colors sent by the host's server. */
    public void showCountdown(int value, int backgroundRgb, int gridRgb, int foodRgb) {
        backgroundColor = new Color(backgroundRgb, true);
        gridColor = new Color(gridRgb, true);
        foodColor = new Color(foodRgb, true);
        countdown = value;
        gameOver = false;
        restartButton.setVisible(false);
        restartButton.setEnabled(true);
        status.setText("Match starts in " + value + "...");
        board.setBackground(backgroundColor);
        board.repaint();
    }

    /** Removes the overlay after the server starts the simulation. */
    public void markStarted() {
        countdown = -1;
        gameOver = false;
        restartButton.setVisible(false);
        restartButton.setEnabled(true);
        status.setText("Match in progress — host controls the restart.");
        board.repaint();
    }

    /** Shows the finished state; only the host gets a restart button. */
    public void markGameOver() {
        gameOver = true;
        countdown = -1;
        if (host) {
            restartButton.setVisible(true);
            restartButton.setEnabled(true);
            status.setText("Match finished. The host can start another round.");
        } else {
            restartButton.setVisible(false);
            status.setText("Match finished. Waiting for the host to restart or leave.");
        }
        board.repaint();
    }

    private void requestRestart() {
        if (!host || !gameOver) return;
        restartButton.setEnabled(false);
        status.setText("Restart requested. Preparing the next round...");
        try {
            client.restartMatch();
        } catch (RuntimeException exception) {
            restartButton.setEnabled(true);
            status.setText(exception.getMessage());
        }
    }

    private void installControls() {
        bindKey(KeyEvent.VK_UP, "up", Direction.UP);
        bindKey(KeyEvent.VK_W, "w", Direction.UP);
        bindKey(KeyEvent.VK_DOWN, "down", Direction.DOWN);
        bindKey(KeyEvent.VK_S, "s", Direction.DOWN);
        bindKey(KeyEvent.VK_LEFT, "left", Direction.LEFT);
        bindKey(KeyEvent.VK_A, "a", Direction.LEFT);
        bindKey(KeyEvent.VK_RIGHT, "right", Direction.RIGHT);
        bindKey(KeyEvent.VK_D, "d", Direction.RIGHT);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "leave");
        getActionMap().put("leave", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { leaveMatch(); }
        });
    }

    private void bindKey(int keyCode, String name, Direction direction) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (!gameOver && countdown < 0) {
                    try { client.setDirection(direction); }
                    catch (RuntimeException exception) { status.setText(exception.getMessage()); }
                }
            }
        });
    }

    private void leaveMatch() {
        shutdown();
        Main.mostrarMenu();
    }

    /** Stops local rendering and leaves the LAN session; ESC never restarts it. */
    public void shutdown() {
        repaintTimer.stop();
        client.close();
        if (server != null) server.close();
    }

    private final class BoardCanvas extends JPanel {
        @Override protected void paintComponent(Graphics graphics) {
            setBackground(backgroundColor);
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            int cell = GameConfig.CELL_SIZE;
            int width = GameConfig.COLS * cell;
            int height = GameConfig.ROWS * cell;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("LAN MULTIPLAYER  |  ESC: LEAVE MATCH", 10, 19);
            g.setColor(gridColor);
            for (int x = 0; x <= width; x += cell) g.drawLine(x, 58, x, height + 58);
            for (int y = 0; y <= height; y += cell) g.drawLine(0, y + 58, width, y + 58);
            String state = snapshot;
            if (state != null) drawSnapshot(g, state, cell);
            if (countdown > 0) drawOverlay(g, width, height, Integer.toString(countdown), "GET READY");
            else if (gameOver) drawOverlay(g, width, height, "MATCH FINISHED",
                    host ? "The host can restart the match below." : "Waiting for the host...");
            g.dispose();
        }
    }

    private void drawOverlay(Graphics2D g, int width, int height, String headline, String subtitle) {
        int boxWidth = Math.min(width - 40, 430);
        int boxHeight = 116;
        int x = (width - boxWidth) / 2;
        int y = 58 + (height - boxHeight) / 2;
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(x, y, boxWidth, boxHeight, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, headline.length() > 3 ? 25 : 58));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(headline, (width - metrics.stringWidth(headline)) / 2, y + 58);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.drawString(subtitle, (width - g.getFontMetrics().stringWidth(subtitle)) / 2, y + 88);
    }

    private void drawSnapshot(Graphics2D g, String line, int cell) {
        String[] fields = line.split("\\|", -1);
        if (fields.length < 5 || !"SNAP".equals(fields[0])) return;
        try {
            int foodX = Integer.parseInt(fields[2]);
            int foodY = Integer.parseInt(fields[3]);
            g.setColor(foodColor);
            g.fillOval(foodX * cell, foodY * cell + 58, cell, cell);
            int labelX = 10;
            for (int index = 5; index < fields.length; index++) {
                PlayerView player = parsePlayer(fields[index]);
                if (player == null) continue;
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g.drawString("P" + player.id() + " " + player.name() + ": " + player.score()
                        + (player.alive() ? "" : " (OUT)"), labelX, 42);
                labelX += 145;
                boolean head = true;
                for (Point point : player.body()) {
                    g.setColor(new Color(head ? player.headRgb() : player.bodyRgb(), true));
                    g.fillRect(point.x * cell, point.y * cell + 58, cell, cell);
                    g.setColor(new Color(player.outlineRgb(), true));
                    g.drawRect(point.x * cell, point.y * cell + 58, cell, cell);
                    head = false;
                }
            }
        } catch (NumberFormatException ignored) {
            // Ignore an incomplete snapshot instead of breaking Swing painting.
        }
    }

    private PlayerView parsePlayer(String field) {
        String[] parts = field.split("~", -1);
        if (parts.length != 8) return null;
        try {
            String name = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            List<Point> body = new ArrayList<>();
            if (!parts[7].isBlank()) {
                for (String coordinate : parts[7].split(",")) {
                    String[] xy = coordinate.split(":");
                    if (xy.length != 2) return null;
                    body.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
            }
            return new PlayerView(Integer.parseInt(parts[0]), name, Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]),
                    "1".equals(parts[6]), body);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
