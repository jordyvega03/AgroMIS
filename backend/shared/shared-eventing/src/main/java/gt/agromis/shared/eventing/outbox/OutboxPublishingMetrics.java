package gt.agromis.shared.eventing.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;

/** Registra metricas Prometheus del outbox para alertas y dashboards. */
@ApplicationScoped
public class OutboxPublishingMetrics {

    private final MeterRegistry registry;
    private final OutboxRepository repository;
    private final Timer publishTimer;

    @Inject
    public OutboxPublishingMetrics(MeterRegistry registry, OutboxRepository repository) {
        this.registry = registry;
        this.repository = repository;

        // Gauge actualizado en cada lectura — lazy
        Gauge.builder("outbox_unpublished_count", repository, OutboxRepository::countUnpublished)
                .description("Number of domain events pending publication to Kafka")
                .register(registry);

        this.publishTimer = Timer.builder("outbox_publish_duration_seconds")
                .description("Time taken to publish a batch of outbox entries to Kafka")
                .register(registry);
    }

    public void recordPublishBatch(int count, Instant batchStart) {
        long lagMs = Duration.between(batchStart, Instant.now()).toMillis();
        publishTimer.record(Duration.ofMillis(lagMs));

        registry.counter("outbox_published_total")
                .increment(count);
    }

    public void recordPublishFailure(String eventType) {
        registry.counter("outbox_publish_failures_total",
                        "event_type", eventType)
                .increment();
    }

    /** outbox_lag_seconds = age del evento mas antiguo sin publicar. */
    public void recordLag(Instant oldestUnpublishedOccurredAt) {
        if (oldestUnpublishedOccurredAt == null) return;
        double lagSeconds = Duration.between(oldestUnpublishedOccurredAt, Instant.now())
                .toMillis() / 1000.0;
        registry.gauge("outbox_lag_seconds", lagSeconds);
    }
}
