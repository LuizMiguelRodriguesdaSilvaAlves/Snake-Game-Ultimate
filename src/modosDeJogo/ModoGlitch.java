package modosDeJogo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import pontuacao.ScoreManager;
import exceptions.GlitchModeException;
import sistemaDeMovimentacaoGeral.Direction;
import sistemaDeMovimentacaoGeral.Snake;

/** Secret unstable mode that periodically rotates the directional controls. */
public final class ModoGlitch implements GameMode {
    private final Random random = new Random();
    private int rotation;
    private int ticksUntilGlitch;
    @Override public String getId() { return "glitch"; }
    @Override public String getName() { return "Glitch"; }
    /** Initializes the first control-distortion interval. */
    @Override public void init(Snake snake, ScoreManager score) {
        if (snake == null) throw new GlitchModeException("Glitch mode cannot initialize without a player snake.");
        rotation=0; ticksUntilGlitch=60;
    }
    /** Randomly changes the control rotation every six seconds. */
    @Override public void update(GameModeContext context) {
        if (context == null || context.getSnake() == null) {
            throw new GlitchModeException("Glitch mode requires a valid game update context.");
        }
        if (--ticksUntilGlitch<=0) { rotation=random.nextInt(4); ticksUntilGlitch=45+random.nextInt(45); }
    }
    /** Rotates the requested direction clockwise by the current glitch amount. */
    @Override public Direction adjustDirection(Direction requested) {
        if (requested==null) throw new GlitchModeException("Glitch mode cannot rotate a null movement direction.");
        Direction result=requested;
        for (int i=0;i<rotation;i++) result=switch(result) {
            case UP -> Direction.RIGHT; case RIGHT -> Direction.DOWN; case DOWN -> Direction.LEFT; case LEFT -> Direction.UP;
        };
        return result;
    }
    /** Displays the active directional distortion. */
    @Override public void draw(Graphics g, int cellSize) {
        g.setColor(Color.MAGENTA); g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
        g.drawString("GLITCH ROTATION: " + (rotation*90) + "°", 10, 80);
    }
}
