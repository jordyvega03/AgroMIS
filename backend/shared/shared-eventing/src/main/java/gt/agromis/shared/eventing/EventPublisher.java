package gt.agromis.shared.eventing;

import gt.agromis.shared.domain.DomainEvent;

/**
 * Fachada de publicacion de eventos.
 * La implementacion concreta es OutboxAppender — no publica directo a Kafka.
 */
public interface EventPublisher {

    /**
     * Encola el evento en el outbox dentro de la transaccion activa.
     *
     * @param event      evento de dominio
     * @param targetTopic topic Kafka destino
     */
    void publish(DomainEvent event, String targetTopic);
}
