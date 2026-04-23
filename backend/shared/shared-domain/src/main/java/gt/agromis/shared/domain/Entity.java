package gt.agromis.shared.domain;

import java.util.Objects;

/** Base para entidades DDD con igualdad por identidad. */
public abstract class Entity<ID extends Identity<?>> {

    private final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "entity id must not be null");
    }

    public ID id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
