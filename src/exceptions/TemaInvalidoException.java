package exceptions;

/** Represents invalid visual theme data. */
public class TemaInvalidoException extends RuntimeException {

    public TemaInvalidoException(String motivo) {
        super("Tema visual inválido: " + motivo);
    }
}
