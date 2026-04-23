package gt.agromis.shared.domain;

/** Se lanza cuando una invariante del dominio no se cumple. Mapeada a HTTP 422. */
public class InvariantViolatedException extends BusinessException {

    public InvariantViolatedException(String message) {
        super("INVARIANT_VIOLATED", message);
    }

    public InvariantViolatedException(String field, String message) {
        super("INVARIANT_VIOLATED", field + ": " + message);
    }
}
