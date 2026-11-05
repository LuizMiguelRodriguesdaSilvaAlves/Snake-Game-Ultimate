package modosDeJogo;

/** Represents one Minefield grid cell and whether that cell contains a mine. */
public class CampoMinadoCell {

    public boolean temMina;
    public boolean revelada;

    public CampoMinadoCell(boolean temMina) {
        this.temMina = temMina;
        this.revelada = false;
    }
}
