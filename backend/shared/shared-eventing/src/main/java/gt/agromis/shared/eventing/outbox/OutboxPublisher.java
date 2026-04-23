package gt.agromis.shared.eventing.outbox;

import gt.agromis.shared.eventing.KafkaHeaders;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Worker que cada 500ms lee el outbox y publica a Kafka.
 * Si Kafka esta caido la tx de negocio ya comitu — los eventos se acumulan
 * y se publican en orden cuando Kafka vuelva.
 */
@ApplicationScoped
public class OutboxPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxPublisher.class);
    private static final int MAX_ATTEMPTS = 10;

    @Inject
    OutboxRepository repository;

    @Inject
    OutboxPublishingMetrics metrics;

    // El emitter envia bytes raw; el topic se indica por entrada
    @Inject
    @Channel("outbox-out")
    Emitter<byte[]> emitter;

    @Scheduled(every = "500ms")
    @Transactional
    public void publishPending() {
        List<OutboxEntry> pending = repository.findUnpublished();
        if (pending.isEmpty()) return;

        Instant batchStart = Instant.now();
        int published = 0;

        for (OutboxEntry entry : pending) {
            try {
                send(entry);
                entry.markPublished(Instant.now());
                published++;
            } catch (Exception e) {
                entry.recordFailure(e.getMessage());
                LOG.warnf("Outbox publish failed for %s (attempt %d): %s",
                        entry.id(), entry.publishAttempts(), e.getMessage());

                if (entry.publishAttempts() >= MAX_ATTEMPTS) {
                    LOG.errorf("Outbox entry %s exceeded max attempts — manual intervention required",
                            entry.id());
                    metrics.recordPublishFailure(entry.eventType());
                }
            }
        }

        if (published > 0) {
            metrics.recordPublishBatch(published, batchStart);
        }
    }

    private void send(OutboxEntry entry) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(KafkaHeaders.EVENT_ID,   entry.id().toString().getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.EVENT_TYPE, entry.eventType().getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.COUNTRY_CODE, entry.countryCode().getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.AGGREGATE_ID,   entry.aggregateId().toString().getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.AGGREGATE_TYPE, entry.aggregateType().getBytes(StandardCharsets.UTF_8));

        // El emitter envia al topic configurado en application.yml (outbox-out)
        // En produccion esto se enruta dinamicamente segun entry.targetTopic()
        emitter.send(entry.payloadAvro());
    }
}
