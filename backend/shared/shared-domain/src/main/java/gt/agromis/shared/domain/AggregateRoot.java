package gt.agromis.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base para agregados DDD. Acumula eventos no publicados hasta que la tx confirma. */
public abstract class AggregateRoot<ID extends Identity<?>> extends Entity<ID> {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private long version;

    protected AggregateRoot(ID id) {
        super(id);
    }

    /** Registra un evento de dominio para publicacion posterior al commit de la tx. */
    protected final void raise(DomainEvent event) {
        uncommittedEvents.add(event);
    }

    /**
     * Retorna y limpia los eventos acumulados.
     * Llamar tras persistir el agregado para pasarlos al outbox.
     */
    public final List<DomainEvent> pullEvents() {
        List<DomainEvent> copy =
                Collections.unmodifiableList(new ArrayList<>(uncommittedEvents));
        uncommittedEvents.clear();
        return copy;
    }

    public long version() {
        return version;
    }

    protected void incrementVersion() {
        this.version++;
    }
}
