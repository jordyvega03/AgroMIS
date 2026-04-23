package gt.agromis.shared.eventing;

import java.util.Objects;

/** Clave de idempotencia para evitar procesamiento doble de eventos Kafka. */
public final class IdempotencyKey {

    private final String value;

    private IdempotencyKey(String value) {
        this.value = Objects.requireNonNull(value, "idempotency key must not be null");
    }

    public static IdempotencyKey of(String eventId, String consumerGroup) {
        return new IdempotencyKey("idem:" + consumerGroup + ":" + eventId);
    }

    public static IdempotencyKey of(String rawKey) {
        return new IdempotencyKey(rawKey);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IdempotencyKey that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
