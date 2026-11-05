package exceptions;

/** Represents failures while generating a valid food position. */
public class FoodGenerationException extends RuntimeException {
    public FoodGenerationException(String message) {
        super(message);
    }
}
