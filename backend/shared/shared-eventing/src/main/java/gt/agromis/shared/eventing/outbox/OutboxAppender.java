package gt.agromis.shared.eventing.outbox;

import gt.agromis.shared.domain.DomainEvent;
import gt.agromis.shared.eventing.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.apache.avro.generic.GenericRecord;
import org.jboss.logging.Logger;

/**
 * Implementacion de EventPublisher que escribe en el outbox dentro de la misma tx.
 * El aggregate llama outbox.append(event) — nunca publica directo a Kafka.
 */
@ApplicationScoped
public class OutboxAppender implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxAppender.class);

    @Inject
    OutboxRepository repository;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void publish(DomainEvent event, String targetTopic) {
        append(event, targetTopic, (short) 1, new byte[0]);
    }

    /**
     * Variante con payload Avro serializado.
     *
     * @param payloadAvro bytes del GenericRecord serializado
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void append(DomainEvent event, String targetTopic, short version, byte[] payloadAvro) {
        OutboxEntry entry = new OutboxEntry(
                UUID.fromString(event.eventId()),
                deriveAggregateType(event),
                UUID.fromString(event.aggregateId()),
                event.eventType(),
                version,
                event.countryCode(),
                payloadAvro,
                "{}",
                targetTopic,
                event.occurredAt());

        repository.persist(entry);
        LOG.debugf("Outbox append: %s -> %s", event.eventType(), targetTopic);
    }

    private String deriveAggregateType(DomainEvent event) {
        // El tipo se puede sobre-escribir via subclase o convencion del eventType
        String[] parts = event.eventType().split("\\.");
        return parts.length > 0 ? parts[0] : "Unknown";
    }
}
