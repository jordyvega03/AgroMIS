-- V002: Tablas base del dominio AgroMIS

CREATE TABLE countries (
    code              CHAR(2)       PRIMARY KEY,
    name              VARCHAR(100)  NOT NULL,
    iso_3             CHAR(3)       NOT NULL,
    currency_code     CHAR(3)       NOT NULL,
    timezone          VARCHAR(50)   NOT NULL,
    default_language  CHAR(3)       NOT NULL,
    active            BOOLEAN       NOT NULL DEFAULT true,
    metadata          JSONB         NOT NULL DEFAULT '{}'
);

CREATE TABLE crop_catalog (
    id                  UUID          PRIMARY KEY DEFAULT uuid_generate_v7(),
    country_code        CHAR(2)       NOT NULL REFERENCES countries(code),
    crop_code           VARCHAR(32)   NOT NULL,
    common_name         VARCHAR(100)  NOT NULL,
    scientific_name     VARCHAR(200),
    typical_cycle_days  SMALLINT,
    category            VARCHAR(40),
    active              BOOLEAN       NOT NULL DEFAULT true,
    UNIQUE (country_code, crop_code)
);

-- tenant: concepto logico de aislamiento; 1:1 con country en MVP
CREATE TABLE tenants (
    id            UUID          PRIMARY KEY DEFAULT uuid_generate_v7(),
    code          VARCHAR(32)   NOT NULL UNIQUE,
    country_code  CHAR(2)       NOT NULL REFERENCES countries(code),
    name          VARCHAR(200)  NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Tabla de eventos de dominio (Transactional Outbox Pattern)
CREATE TABLE domain_events (
    id           UUID          PRIMARY KEY DEFAULT uuid_generate_v7(),
    stream_id    VARCHAR(200)  NOT NULL,
    event_type   VARCHAR(200)  NOT NULL,
    payload      JSONB         NOT NULL,
    metadata     JSONB         NOT NULL DEFAULT '{}',
    occurred_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published    BOOLEAN       NOT NULL DEFAULT false
);

CREATE INDEX idx_domain_events_unpublished ON domain_events (occurred_at) WHERE published = false;
CREATE INDEX idx_domain_events_stream      ON domain_events (stream_id, occurred_at);
