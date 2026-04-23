package gt.agromis.shared.eventing;

/** Nombres de headers Kafka estandarizados para todos los mensajes AgroMIS. */
public final class KafkaHeaders {

    public static final String EVENT_ID       = "X-Event-Id";
    public static final String EVENT_TYPE     = "X-Event-Type";
    public static final String COUNTRY_CODE   = "X-Country-Code";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String TRACE_ID       = "X-Trace-Id";
    public static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";
    public static final String AGGREGATE_ID   = "X-Aggregate-Id";
    public static final String AGGREGATE_TYPE = "X-Aggregate-Type";

    private KafkaHeaders() {}
}
