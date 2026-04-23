-- Extensiones requeridas para AgroMIS
-- Se ejecuta automaticamente en el primer arranque del contenedor

\c agromis

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "postgis_topology";
CREATE EXTENSION IF NOT EXISTS "timescaledb";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- h3-pg si esta disponible en esta imagen
-- CREATE EXTENSION IF NOT EXISTS "h3";
