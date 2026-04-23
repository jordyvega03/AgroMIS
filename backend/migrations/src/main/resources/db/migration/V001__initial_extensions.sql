-- V001: Extensiones base de PostgreSQL requeridas por AgroMIS

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "timescaledb";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Funcion UUID v7 (monotonicamente creciente, tiempo-prefijado)
-- Util para primary keys ordenables por tiempo de creacion
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS $$
  SELECT encode(
    set_bit(
      set_bit(
        overlay(uuid_send(gen_random_uuid())
                PLACING substring(int8send(floor(extract(epoch from clock_timestamp()) * 1000)::bigint) from 3)
                FROM 1 FOR 6),
        52, 1),
      53, 1)::uuid
  , 'hex')::uuid;
$$ LANGUAGE SQL VOLATILE;
