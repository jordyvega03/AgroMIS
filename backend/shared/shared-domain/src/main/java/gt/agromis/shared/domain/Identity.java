package gt.agromis.shared.domain;

import java.util.Objects;
import java.util.UUID;

/** Wrapper tipado sobre UUID v7 para identificadores de agregados. */
public abstract class Identity<T extends Identity<T>> implements ValueObject {

    private final UUID value;

    protected Identity(UUID value) {
        this.value = Objects.requireNonNull(value, "identity value must not be null");
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity<?> identity = (Identity<?>) o;
        return Objects.equals(value, identity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
