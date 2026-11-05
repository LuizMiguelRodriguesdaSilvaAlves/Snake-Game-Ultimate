package modosDeJogo;

import exceptions.ReversoException;
import pontuacao.ScoreManager;
import sistemaDeMovimentacaoGeral.Snake;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/** Turns the player into a hunted target pursued by AI-controlled enemy snakes. */
public class ModoReverso implements GameMode {

    private static final int COLS = 30;
    private static final int ROWS = 20;

    private static final int TICKS_PARA_PONTUAR = 10;

    private static final int LIMITE_COBRAS_INIMIGAS = 4;

    private static final int SCORE_SEGUNDA_COBRA = 25;
    private static final int SCORE_TERCEIRA_COBRA = 55;
    private static final int SCORE_QUARTA_COBRA = 90;

    private enum EstadoIA {
        PERSEGUIR,
        DISPERSAR,
        CONFUSO
    }

    private static class CobraInimiga {

        private final LinkedList<Point> corpo = new LinkedList<>();

        private EstadoIA estadoAtual = EstadoIA.DISPERSAR;

        private int contadorEstado = 0;
        private int contadorMovimento = 0;

        private final Point cantoPreferido;

        private CobraInimiga(Point cantoPreferido) {
            this.cantoPreferido = cantoPreferido;
        }
    }

    private final Random random = new Random();

    private ScoreManager scoreManager;

    private final List<CobraInimiga> cobrasInimigas = new ArrayList<>();

    private int contadorPontuacao = 0;
    private boolean jogadorMorto = false;

    private int duracaoPerseguir = 35;
    private int duracaoDispersar = 35;
    private int duracaoConfuso = 18;
    private int intervaloMovimentoInimigo = 3;

    /** Stable identifier used to keep this mode score independent. */
    @Override
    public String getId() { return "reverse"; }

    @Override
    public String getName() {
        return "Reverso";
    }

    @Override
    public void init(Snake snake, ScoreManager score) {
        if (snake == null) {
            throw new ReversoException("A cobra do jogador não pode ser nula no modo Reverso.");
        }

        if (score == null) {
            throw new ReversoException("ScoreManager não pode ser nulo no modo Reverso.");
        }

        this.scoreManager = score;

        contadorPontuacao = 0;
        jogadorMorto = false;

        cobrasInimigas.clear();
        adicionarNovaCobraInimiga(snake.getHead());
    }

    @Override
    public void update(Snake snake) {
        if (snake == null) {
            throw new ReversoException("A cobra do jogador não pode ser nula durante atualização do modo Reverso.");
        }

        if (jogadorMorto) {
            return;
        }

        atualizarDificuldade();
        atualizarQuantidadeDeCobras(snake.getHead());

        for (CobraInimiga cobra : cobrasInimigas) {
            atualizarEstadoIA(cobra);

            cobra.contadorMovimento++;

            if (cobra.contadorMovimento >= intervaloMovimentoInimigo) {
                moverCobraInimiga(cobra, snake.getHead());
                cobra.contadorMovimento = 0;
            }
        }

        verificarColisaoComJogador(snake);
        pontuarPorSobrevivencia();
    }

    @Override
    public void draw(Graphics g, int cellSize) {
        if (g == null) {
            throw new ReversoException("Graphics não pode ser nulo no modo Reverso.");
        }

        for (CobraInimiga cobra : cobrasInimigas) {
            desenharCobraInimiga(g, cellSize, cobra);
        }

        desenharIndicadorEstado(g);
    }

    @Override
    public void onFoodPlaced(Point food) {
        // Reverse mode does not use regular collectible fruit.
    }

    /** The reverse mode has no collectible fruit; the player becomes the target. */
    @Override
    public boolean usesFood() { return false; }

    /** Renders the player as the fruit being hunted by enemy snakes. */
    @Override
    public boolean rendersPlayerAsFood() { return true; }

    /** Exposes the enemy collision state through the common game-mode contract. */
    @Override
    public boolean isGameOver() { return jogadorMorto; }

    public boolean isJogadorMorto() {
        return jogadorMorto;
    }

    public List<Point> getCobraInimiga() {
        List<Point> segmentos = new ArrayList<>();

        for (CobraInimiga cobra : cobrasInimigas) {
            segmentos.addAll(cobra.corpo);
        }

        return segmentos;
    }

    private void atualizarDificuldade() {
        int score = scoreManager.getCurrentScore();

        /*
         * As the score increases:
         * - enemy snakes spend more time chasing the player;
         * - enemy snakes spend less time wandering or acting confused;
         * - enemy movement becomes faster.
         */
        if (score < 20) {
            duracaoPerseguir = 30;
            duracaoDispersar = 40;
            duracaoConfuso = 22;
            intervaloMovimentoInimigo = 3;
        } else if (score < 45) {
            duracaoPerseguir = 45;
            duracaoDispersar = 34;
            duracaoConfuso = 18;
            intervaloMovimentoInimigo = 3;
        } else if (score < 75) {
            duracaoPerseguir = 60;
            duracaoDispersar = 28;
            duracaoConfuso = 14;
            intervaloMovimentoInimigo = 2;
        } else if (score < 110) {
            duracaoPerseguir = 75;
            duracaoDispersar = 22;
            duracaoConfuso = 10;
            intervaloMovimentoInimigo = 2;
        } else {
            duracaoPerseguir = 95;
            duracaoDispersar = 16;
            duracaoConfuso = 8;
            intervaloMovimentoInimigo = 1;
        }
    }

    private void atualizarQuantidadeDeCobras(Point jogador) {
        int score = scoreManager.getCurrentScore();

        if (score >= SCORE_SEGUNDA_COBRA && cobrasInimigas.size() < 2) {
            adicionarNovaCobraInimiga(jogador);
        }

        if (score >= SCORE_TERCEIRA_COBRA && cobrasInimigas.size() < 3) {
            adicionarNovaCobraInimiga(jogador);
        }

        if (score >= SCORE_QUARTA_COBRA && cobrasInimigas.size() < 4) {
            adicionarNovaCobraInimiga(jogador);
        }
    }

    private void adicionarNovaCobraInimiga(Point jogador) {
        if (cobrasInimigas.size() >= LIMITE_COBRAS_INIMIGAS) {
            return;
        }

        Point canto = escolherCantoPorIndice(cobrasInimigas.size());
        CobraInimiga novaCobra = new CobraInimiga(canto);

        Point cabeca = gerarPosicaoLongeDoJogador(jogador);

        novaCobra.corpo.add(new Point(cabeca));
        novaCobra.corpo.add(new Point(cabeca.x - 1, cabeca.y));
        novaCobra.corpo.add(new Point(cabeca.x - 2, cabeca.y));

        novaCobra.estadoAtual = EstadoIA.DISPERSAR;
        novaCobra.contadorEstado = 0;
        novaCobra.contadorMovimento = 0;

        cobrasInimigas.add(novaCobra);
    }

    private Point escolherCantoPorIndice(int indice) {
        Point[] cantos = {
                new Point(1, 1),
                new Point(COLS - 2, 1),
                new Point(1, ROWS - 2),
                new Point(COLS - 2, ROWS - 2)
        };

        return cantos[indice % cantos.length];
    }

    private Point gerarPosicaoLongeDoJogador(Point jogador) {
        final int distanciaMinima = 9;
        final int maxTentativas = 700;

        for (int i = 0; i < maxTentativas; i++) {
            int x = 2 + random.nextInt(COLS - 4);
            int y = 2 + random.nextInt(ROWS - 4);

            Point candidato = new Point(x, y);

            if (candidato.distance(jogador) < distanciaMinima) {
                continue;
            }

            if (colideComAlgumaCobra(candidato)) {
                continue;
            }

            return candidato;
        }

        return new Point(COLS - 4, ROWS - 4);
    }

    private boolean colideComAlgumaCobra(Point ponto) {
        for (CobraInimiga cobra : cobrasInimigas) {
            if (cobra.corpo.contains(ponto)) {
                return true;
            }
        }

        return false;
    }

    private void atualizarEstadoIA(CobraInimiga cobra) {
        cobra.contadorEstado++;

        switch (cobra.estadoAtual) {
            case DISPERSAR -> {
                if (cobra.contadorEstado >= duracaoDispersar) {
                    trocarEstado(cobra, EstadoIA.PERSEGUIR);
                }
            }

            case PERSEGUIR -> {
                if (cobra.contadorEstado >= duracaoPerseguir) {
                    trocarEstado(cobra, EstadoIA.CONFUSO);
                }
            }

            case CONFUSO -> {
                if (cobra.contadorEstado >= duracaoConfuso) {
                    trocarEstado(cobra, EstadoIA.DISPERSAR);
                }
            }
        }
    }

    private void trocarEstado(CobraInimiga cobra, EstadoIA novoEstado) {
        cobra.estadoAtual = novoEstado;
        cobra.contadorEstado = 0;
    }

    private void moverCobraInimiga(CobraInimiga cobra, Point jogador) {
        Point cabecaAtual = cobra.corpo.getFirst();
        Point alvo = escolherAlvo(cobra, jogador);

        Point novaCabeca;

        if (cobra.estadoAtual == EstadoIA.CONFUSO) {
            novaCabeca = escolherMovimentoConfuso(cobra, cabecaAtual, jogador);
        } else {
            novaCabeca = calcularMelhorPasso(cobra, cabecaAtual, alvo);
        }

        cobra.corpo.addFirst(novaCabeca);
        cobra.corpo.removeLast();
    }

    private Point escolherAlvo(CobraInimiga cobra, Point jogador) {
        return switch (cobra.estadoAtual) {
            case PERSEGUIR -> jogador;
            case DISPERSAR -> cobra.cantoPreferido;
            case CONFUSO -> jogador;
        };
    }

    private Point calcularMelhorPasso(CobraInimiga cobra, Point origem, Point alvo) {
        List<Point> opcoes = gerarMovimentosValidos(cobra, origem);

        if (opcoes.isEmpty()) {
            return origem;
        }

        Point melhor = opcoes.get(0);
        double melhorDistancia = melhor.distance(alvo);

        for (Point opcao : opcoes) {
            double distancia = opcao.distance(alvo);

            if (distancia < melhorDistancia) {
                melhorDistancia = distancia;
                melhor = opcao;
            }
        }

        return melhor;
    }

    private Point escolherMovimentoConfuso(CobraInimiga cobra, Point origem, Point jogador) {
        List<Point> opcoes = gerarMovimentosValidos(cobra, origem);

        if (opcoes.isEmpty()) {
            return origem;
        }

        /*
         * As difficulty increases, even the confused state becomes less random.
         */
        int score = scoreManager.getCurrentScore();
        int chanceAleatoria;

        if (score < 40) {
            chanceAleatoria = 75;
        } else if (score < 80) {
            chanceAleatoria = 60;
        } else {
            chanceAleatoria = 45;
        }

        if (random.nextInt(100) < chanceAleatoria) {
            return opcoes.get(random.nextInt(opcoes.size()));
        }

        return calcularMelhorPasso(cobra, origem, jogador);
    }

    private List<Point> gerarMovimentosValidos(CobraInimiga cobra, Point origem) {
        List<Point> opcoes = new ArrayList<>();

        Point cima = new Point(origem.x, origem.y - 1);
        Point baixo = new Point(origem.x, origem.y + 1);
        Point esquerda = new Point(origem.x - 1, origem.y);
        Point direita = new Point(origem.x + 1, origem.y);

        adicionarSeValido(cobra, opcoes, cima);
        adicionarSeValido(cobra, opcoes, baixo);
        adicionarSeValido(cobra, opcoes, esquerda);
        adicionarSeValido(cobra, opcoes, direita);

        return opcoes;
    }

    private void adicionarSeValido(CobraInimiga cobra, List<Point> opcoes, Point ponto) {
        if (!posicaoValida(ponto)) {
            return;
        }

        if (cobra.corpo.size() > 1 && ponto.equals(cobra.corpo.get(1))) {
            return;
        }

        opcoes.add(ponto);
    }

    private boolean posicaoValida(Point p) {
        return p.x >= 0 && p.x < COLS && p.y >= 0 && p.y < ROWS;
    }

    private void verificarColisaoComJogador(Snake snake) {
        Point jogador = snake.getHead();

        for (CobraInimiga cobra : cobrasInimigas) {
            for (Point segmento : cobra.corpo) {
                if (segmento.equals(jogador)) {
                    jogadorMorto = true;
                    return;
                }
            }
        }
    }

    private void pontuarPorSobrevivencia() {
        contadorPontuacao++;

        if (contadorPontuacao >= TICKS_PARA_PONTUAR) {
            scoreManager.addPoints(1);
            contadorPontuacao = 0;
        }
    }

    private void desenharCobraInimiga(Graphics g, int cellSize, CobraInimiga cobra) {
        for (int i = 0; i < cobra.corpo.size(); i++) {
            Point parte = cobra.corpo.get(i);

            aplicarCorPorEstado(g, cobra.estadoAtual, i == 0);

            g.fillRect(
                    parte.x * cellSize,
                    parte.y * cellSize,
                    cellSize,
                    cellSize
            );

            g.setColor(Color.BLACK);
            g.drawRect(
                    parte.x * cellSize,
                    parte.y * cellSize,
                    cellSize,
                    cellSize
            );
        }
    }

    private void aplicarCorPorEstado(Graphics g, EstadoIA estado, boolean cabeca) {
        switch (estado) {
            case PERSEGUIR -> g.setColor(
                    cabeca ? new Color(130, 0, 0) : new Color(190, 30, 30)
            );

            case DISPERSAR -> g.setColor(
                    cabeca ? new Color(30, 60, 170) : new Color(60, 120, 220)
            );

            case CONFUSO -> g.setColor(
                    cabeca ? new Color(130, 30, 160) : new Color(190, 80, 220)
            );
        }
    }

    private void desenharIndicadorEstado(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 14));

        int score = scoreManager.getCurrentScore();

        g.setColor(Color.WHITE);
        g.drawString("Cobras inimigas: " + cobrasInimigas.size() + "/" + LIMITE_COBRAS_INIMIGAS, 10, 80);

        g.drawString("Perseguição: " + duracaoPerseguir + " ticks", 10, 100);

        if (score < SCORE_SEGUNDA_COBRA) {
            g.drawString("Próxima cobra: " + SCORE_SEGUNDA_COBRA, 10, 120);
        } else if (score < SCORE_TERCEIRA_COBRA) {
            g.drawString("Próxima cobra: " + SCORE_TERCEIRA_COBRA, 10, 120);
        } else if (score < SCORE_QUARTA_COBRA) {
            g.drawString("Próxima cobra: " + SCORE_QUARTA_COBRA, 10, 120);
        } else {
            g.drawString("Limite máximo de cobras atingido", 10, 120);
        }
    }
}