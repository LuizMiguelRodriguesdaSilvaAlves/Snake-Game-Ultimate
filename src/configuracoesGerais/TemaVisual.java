package configuracoesGerais;

import java.awt.*;
import java.io.Serializable;

/**
 * Represents a complete visual theme used to render the game board.
 */
public class TemaVisual implements Serializable {

    private final Color fundo;
    private final Color grade;
    private final Color comida;
    private final Color bordaCobra;
    private final Color texto;
    private final Font fonte;

    public TemaVisual(
            Color fundo,
            Color grade,
            Color comida,
            Color bordaCobra,
            Color texto,
            Font fonte
    ) {
        this.fundo = fundo;
        this.grade = grade;
        this.comida = comida;
        this.bordaCobra = bordaCobra;
        this.texto = texto;
        this.fonte = fonte;
    }

    public Color getFundo() { return fundo; }
    public Color getGrade() { return grade; }
    public Color getComida() { return comida; }
    public Color getBordaCobra() { return bordaCobra; }
    public Color getTexto() { return texto; }
    public Font getFonte() { return fonte; }
}
