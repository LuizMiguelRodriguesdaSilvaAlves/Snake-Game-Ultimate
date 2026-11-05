package modosDeJogo;

import exceptions.PortalException;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Snake;

import java.awt.*;
import java.util.Random;

/** Teleports the complete snake to a safe board position after food collection. */
public class ModoPortal implements GameMode {

    private final Random random = new Random();

    private Snake snake;

    private int cols = 30;
    private int rows = 20;

    private boolean primeiraFrutaJaColocada = false;
    private Point ultimaFrutaProcessada = null;

    public ModoPortal() {}

    public ModoPortal(int cols, int rows) {
        if (cols < 5 || rows < 5) {
            throw new PortalException("O mapa é pequeno demais para o modo Portal.");
        }

        this.cols = cols;
        this.rows = rows;
    }

    /** Stable identifier used to keep this mode score independent. */
    @Override
    public String getId() { return "portal"; }

    @Override
    public String getName() {
        return "Portal";
    }

    @Override
    public void init(Snake snake, ScoreManager score) {
        if (snake == null) {
            throw new PortalException("A cobra não pode ser nula no modo Portal.");
        }

        this.snake = snake;
        this.primeiraFrutaJaColocada = false;
        this.ultimaFrutaProcessada = null;
    }

    @Override
    public void update(Snake snake) {
        // Portal mode reacts when a new fruit is placed after a collection.
    }

    @Override
    public void draw(Graphics g, int cellSize) {
        // Portal mode does not render additional board elements.
    }

    @Override
    public void onFoodPlaced(Point food) {
        if (snake == null) {
            throw new PortalException("Modo Portal não foi inicializado corretamente.");
        }

        if (food == null) {
            throw new PortalException("A fruta informada ao modo Portal não pode ser nula.");
        }

        /*
         * The game panel calls onFoodPlaced when the first fruit is created.
         * The player has not collected anything yet, so the snake must not teleport.
         */
        if (!primeiraFrutaJaColocada) {
            primeiraFrutaJaColocada = true;
            ultimaFrutaProcessada = new Point(food);
            return;
        }

        /*
         * Protect against duplicate food-placement notifications.
         * The game panel invokes this callback from placeFood()
         * after a fruit has been collected.
         */
        if (ultimaFrutaProcessada != null && ultimaFrutaProcessada.equals(food)) {
            return;
        }

        ultimaFrutaProcessada = new Point(food);
        teleportarCobra(food);
    }

    private void teleportarCobra(Point food) {
        Point novaCabeca = gerarPosicaoSegura(food);

        if (novaCabeca == null) {
            throw new PortalException("Não foi possível encontrar uma posição segura para teleportar a cobra.");
        }

        snake.teleportTo(novaCabeca);
    }

    private Point gerarPosicaoSegura(Point food) {
        final int maxTentativas = 1000;

        for (int i = 0; i < maxTentativas; i++) {
            int x = 1 + random.nextInt(cols - 2);
            int y = 1 + random.nextInt(rows - 2);

            Point candidato = new Point(x, y);

            if (candidato.equals(food)) {
                continue;
            }

            if (posicaoMantemCorpoSeguro(candidato, food)) {
                return candidato;
            }
        }

        return null;
    }

    private boolean posicaoMantemCorpoSeguro(Point novaCabeca, Point food) {
        Point cabecaAtual = snake.getHead();

        int dx = novaCabeca.x - cabecaAtual.x;
        int dy = novaCabeca.y - cabecaAtual.y;

        for (Point segmento : snake.getBody()) {
            int novoX = segmento.x + dx;
            int novoY = segmento.y + dy;
            if (food != null && food.x == novoX && food.y == novoY) {
                return false;
            }

            /*
             * Keep the entire snake at least one cell away
             * from the board edges.
             */
            if (novoX <= 0 || novoX >= cols - 1) {
                return false;
            }

            if (novoY <= 0 || novoY >= rows - 1) {
                return false;
            }
        }

        return true;
    }
}