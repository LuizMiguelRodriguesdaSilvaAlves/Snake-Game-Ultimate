package modosDeJogo;

import exceptions.PortalException;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Snake;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Teleports the complete snake to a safe board position after food collection. */
public class ModoPortal implements GameMode {

    private final Random random = new Random();
    private Snake snake;
    private int cols = 30;
    private int rows = 20;
    private boolean primeiraFrutaJaColocada = false;
    private Point ultimaFrutaProcessada = null;

    public ModoPortal() { }

    public ModoPortal(int cols, int rows) {
        if (cols < 5 || rows < 5) {
            throw new PortalException("O mapa é pequeno demais para o modo Portal.");
        }
        this.cols = cols;
        this.rows = rows;
    }

    /** Stable identifier used to keep this mode score independent. */
    @Override public String getId() { return "portal"; }

    @Override public String getName() { return "Portal"; }

    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new PortalException("A cobra não pode ser nula no modo Portal.");
        this.snake = snake;
        this.primeiraFrutaJaColocada = false;
        this.ultimaFrutaProcessada = null;
    }

    @Override public void update(Snake snake) {
        // Portal mode reacts when a new fruit is placed after a collection.
    }

    @Override public void draw(Graphics g, int cellSize) {
        // Portal mode does not render additional board elements.
    }

    /**
     * Legacy callback retained for callers that do not provide a round context.
     * Such callers still receive wall and body-safety checks, but cannot expose
     * obstacles belonging to other selected modes.
     */
    @Override public void onFoodPlaced(Point food) {
        processFoodPlacement(food, position -> false);
    }

    /** Receives the composite obstacle map so teleports cannot overlap other selected obstacles. */
    @Override public void onFoodPlaced(GameModeContext context) {
        if (context == null) throw new PortalException("O contexto do teleporte não pode ser nulo.");
        if (context.getColumns() < 3 || context.getRows() < 3) {
            throw new PortalException("O mapa é pequeno demais para encontrar uma área segura.");
        }
        this.cols = context.getColumns();
        this.rows = context.getRows();
        processFoodPlacement(context.getFood(), context::isPositionBlocked);
    }

    /** Handles initial food placement and subsequent portal activations. */
    private void processFoodPlacement(Point food, Predicate<Point> isBlocked) {
        if (snake == null) throw new PortalException("Modo Portal não foi inicializado corretamente.");
        if (food == null) throw new PortalException("A fruta informada ao modo Portal não pode ser nula.");

        // The initial fruit is not a collection event and must not trigger a teleport.
        if (!primeiraFrutaJaColocada) {
            primeiraFrutaJaColocada = true;
            ultimaFrutaProcessada = new Point(food);
            return;
        }

        // Ignore duplicate placement notifications for the same fruit coordinate.
        if (ultimaFrutaProcessada != null && ultimaFrutaProcessada.equals(food)) return;

        ultimaFrutaProcessada = new Point(food);
        teleportarCobra(food, isBlocked);
    }

    /** Selects and applies a translation that leaves every segment in a free cell. */
    private void teleportarCobra(Point food, Predicate<Point> isBlocked) {
        Point novaCabeca = gerarPosicaoSegura(food, isBlocked);
        if (novaCabeca == null) {
            throw new PortalException("Não foi possível encontrar uma área livre para teleportar a cobra.");
        }
        snake.teleportTo(novaCabeca);
    }

    /** Randomizes all possible interior head positions and returns the first fully safe translation. */
    private Point gerarPosicaoSegura(Point food, Predicate<Point> isBlocked) {
        if (cols < 3 || rows < 3) return null;

        List<Point> candidates = new ArrayList<>((cols - 2) * (rows - 2));
        for (int y = 1; y < rows - 1; y++) {
            for (int x = 1; x < cols - 1; x++) candidates.add(new Point(x, y));
        }
        Collections.shuffle(candidates, random);

        for (Point candidate : candidates) {
            if (posicaoMantemCorpoSeguro(candidate, food, isBlocked)) return candidate;
        }
        return null;
    }

    /** Validates the complete translated body, not only the destination head cell. */
    private boolean posicaoMantemCorpoSeguro(Point novaCabeca, Point food, Predicate<Point> isBlocked) {
        Point cabecaAtual = snake.getHead();
        int dx = novaCabeca.x - cabecaAtual.x;
        int dy = novaCabeca.y - cabecaAtual.y;

        for (Point segmento : snake.getBody()) {
            Point destino = new Point(segmento.x + dx, segmento.y + dy);

            // Keep every segment inside the board's inner area, away from all four walls.
            if (destino.x <= 0 || destino.x >= cols - 1 || destino.y <= 0 || destino.y >= rows - 1) {
                return false;
            }
            if (food != null && food.equals(destino)) return false;
            if (isBlocked.test(new Point(destino))) return false;
        }
        return true;
    }
}