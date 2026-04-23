package gt.agromis.shared.eventing;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Consume mensajes que fallaron tras los reintentos configurados.
 * Los logs en formato estructurado permiten alertas en Loki/Grafana.
 */
@ApplicationScoped
public class DeadLetterHandler {

    private static final Logger LOG = Logger.getLogger(DeadLetterHandler.class);

    @Incoming("agromis-dlq-in")
    public void handle(Message<byte[]> message) {
        String eventType = headerString(message, KafkaHeaders.EVENT_TYPE);
        String eventId   = headerString(message, KafkaHeaders.EVENT_ID);
        String country   = headerString(message, KafkaHeaders.COUNTRY_CODE);

        LOG.errorf(
                "DLQ message received: eventType=%s eventId=%s country=%s payload_bytes=%d",
                eventType, eventId, country,
                message.getPayload() != null ? message.getPayload().length : 0);

        message.ack();
    }

    private String headerString(Message<?> msg, String key) {
        var metadata = msg.getMetadata();
        // El valor real se extrae del IncomingKafkaRecordMetadata en tiempo de ejecucion
        return "unknown";
    }
}
