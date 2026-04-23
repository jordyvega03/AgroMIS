-- Outbox pattern: los agregados escriben aqui en la misma tx de negocio.
-- Un worker asincrono lee y publica a Kafka.
-- La tabla esta particionada por mes para facilitar purga historica.

CREATE TABLE domain_events (
    id               UUID        PRIMARY KEY DEFAULT uuid_generate_v7(),
    aggregate_type   VARCHAR(50) NOT NULL,
    aggregate_id     UUID        NOT NULL,
    event_type       VARCHAR(80) NOT NULL,
    event_version    SMALLINT    NOT NULL,
    country_code     CHAR(2)     NOT NULL,
    payload_avro     BYTEA       NOT NULL,
    metadata         JSONB       NOT NULL DEFAULT '{}',
    target_topic     VARCHAR(120) NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    sequence_number  BIGSERIAL   NOT NULL,
    published_at     TIMESTAMPTZ,
    publish_attempts SMALLINT    NOT NULL DEFAULT 0,
    last_publish_error TEXT
) PARTITION BY RANGE (occurred_at);

-- Particion inicial: abril 2026
CREATE TABLE domain_events_2026_04 PARTITION OF domain_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- Particion mayo 2026
CREATE TABLE domain_events_2026_05 PARTITION OF domain_events
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- Particion junio 2026
CREATE TABLE domain_events_2026_06 PARTITION OF domain_events
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

-- Indice principal: el worker lee solo los no publicados ordenados por sequence
CREATE INDEX idx_outbox_unpublished
    ON domain_events (sequence_number)
    WHERE published_at IS NULL;

-- Indice para auditoria: buscar todos los eventos de un agregado
CREATE INDEX idx_outbox_aggregate
    ON domain_events (aggregate_id, sequence_number);

-- Indice para alertas: eventos con muchos intentos fallidos
CREATE INDEX idx_outbox_failed
    ON domain_events (publish_attempts)
    WHERE published_at IS NULL AND publish_attempts > 3;

-- Funcion auxiliar para crear la particion del proximo mes
-- Llamar via pg_cron mensualmente: SELECT create_next_domain_events_partition();
CREATE OR REPLACE FUNCTION create_next_domain_events_partition()
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    next_month      DATE;
    partition_name  TEXT;
    start_date      TEXT;
    end_date        TEXT;
BEGIN
    next_month     := date_trunc('month', now()) + INTERVAL '1 month';
    partition_name := 'domain_events_' || to_char(next_month, 'YYYY_MM');
    start_date     := to_char(next_month, 'YYYY-MM-01');
    end_date       := to_char(next_month + INTERVAL '1 month', 'YYYY-MM-01');

    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = partition_name
    ) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF domain_events FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END IF;
END;
$$;

COMMENT ON TABLE domain_events IS
    'Outbox de eventos de dominio. Worker asincrono publica a Kafka y marca published_at.';
