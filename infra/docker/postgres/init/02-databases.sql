-- Crea bases de datos adicionales necesarias para el stack
-- agromis ya fue creada por POSTGRES_DB en el compose

CREATE DATABASE keycloak;
CREATE DATABASE agromis_timescale;

GRANT ALL PRIVILEGES ON DATABASE keycloak TO agromis;
GRANT ALL PRIVILEGES ON DATABASE agromis_timescale TO agromis;

-- Extensiones en agromis_timescale
\c agromis_timescale
CREATE EXTENSION IF NOT EXISTS "timescaledb";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
