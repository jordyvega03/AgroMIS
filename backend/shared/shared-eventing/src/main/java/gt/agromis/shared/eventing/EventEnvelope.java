package gt.agromis.shared.eventing;

import gt.agromis.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Wrapper que agrega metadata de infraestructura alrededor de un DomainEvent. */
public final class EventEnvelope {

    private final String eventId;
    private final String eventType;
    private final String aggregateId;
    private final String aggregateType;
    private final String countryCode;
    private final String correlationId;
    private final String traceId;
    private final Instant occurredAt;
    private final Map<String, String> metadata;
    private final DomainEvent payload;

    private EventEnvelope(Builder builder) {
        this.eventId = Objects.requireNonNull(builder.eventId);
        this.eventType = Objects.requireNonNull(builder.eventType);
        this.aggregateId = Objects.requireNonNull(builder.aggregateId);
        this.aggregateType = Objects.requireNonNull(builder.aggregateType);
        this.countryCode = Objects.requireNonNull(builder.countryCode);
        this.correlationId = builder.correlationId;
        this.traceId = builder.traceId;
        this.occurredAt = Objects.requireNonNull(builder.occurredAt);
        this.metadata = Map.copyOf(builder.metadata);
        this.payload = Objects.requireNonNull(builder.payload);
    }

    public static Builder of(DomainEvent event, String aggregateType) {
        return new Builder(event, aggregateType);
    }

    public String eventId() { return eventId; }
    public String eventType() { return eventType; }
    public String aggregateId() { return aggregateId; }
    public String aggregateType() { return aggregateType; }
    public String countryCode() { return countryCode; }
    public String correlationId() { return correlationId; }
    public String traceId() { return traceId; }
    public Instant occurredAt() { return occurredAt; }
    public Map<String, String> metadata() { return metadata; }
    public DomainEvent payload() { return payload; }

    public static final class Builder {
        private String eventId;
        private String eventType;
        private String aggregateId;
        private String aggregateType;
        private String countryCode;
        private String correlationId;
        private String traceId;
        private Instant occurredAt;
        private Map<String, String> metadata = Map.of();
        private DomainEvent payload;

        private Builder(DomainEvent event, String aggregateType) {
            this.payload = event;
            this.eventId = event.eventId();
            this.eventType = event.eventType();
            this.aggregateId = event.aggregateId();
            this.aggregateType = aggregateType;
            this.countryCode = event.countryCode();
            this.occurredAt = event.occurredAt();
        }

        public Builder correlationId(String v) { this.correlationId = v; return this; }
        public Builder traceId(String v) { this.traceId = v; return this; }
        public Builder metadata(Map<String, String> v) { this.metadata = v; return this; }

        public EventEnvelope build() { return new EventEnvelope(this); }
    }
}
