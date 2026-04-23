package gt.agromis.shared.eventing.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entidad JPA que mapea la tabla domain_events (outbox). */
@Entity
@Table(name = "domain_events")
public class OutboxEntry {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private short eventVersion;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "payload_avro", nullable = false)
    private byte[] payloadAvro;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "target_topic", nullable = false, length = 120)
    private String targetTopic;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private short publishAttempts = 0;

    @Column(name = "last_publish_error")
    private String lastPublishError;

    protected OutboxEntry() {}

    public OutboxEntry(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            short eventVersion,
            String countryCode,
            byte[] payloadAvro,
            String metadata,
            String targetTopic,
            Instant occurredAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.countryCode = countryCode;
        this.payloadAvro = payloadAvro;
        this.metadata = metadata;
        this.targetTopic = targetTopic;
        this.occurredAt = occurredAt;
    }

    public UUID id() { return id; }
    public String aggregateType() { return aggregateType; }
    public UUID aggregateId() { return aggregateId; }
    public String eventType() { return eventType; }
    public short eventVersion() { return eventVersion; }
    public String countryCode() { return countryCode; }
    public byte[] payloadAvro() { return payloadAvro; }
    public String metadata() { return metadata; }
    public String targetTopic() { return targetTopic; }
    public Instant occurredAt() { return occurredAt; }
    public Instant publishedAt() { return publishedAt; }
    public short publishAttempts() { return publishAttempts; }
    public String lastPublishError() { return lastPublishError; }

    public void markPublished(Instant at) {
        this.publishedAt = at;
    }

    public void recordFailure(String error) {
        this.publishAttempts++;
        this.lastPublishError = error;
    }
}
