package gt.agromis.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Colector in-memory de eventos para uso en tests unitarios.
 * La implementacion real vive en shared-eventing (OutboxAppender).
 */
public final class DomainEventPublisher {

    private static final ThreadLocal<List<DomainEvent>> STORE =
            ThreadLocal.withInitial(ArrayList::new);

    private DomainEventPublisher() {}

    public static void publish(DomainEvent event) {
        STORE.get().add(event);
    }

    public static List<DomainEvent> publishedEvents() {
        return Collections.unmodifiableList(STORE.get());
    }

    public static void reset() {
        STORE.get().clear();
    }
}
