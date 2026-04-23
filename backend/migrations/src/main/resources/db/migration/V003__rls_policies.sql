-- V003: Rol de aplicacion y funcion helper para Row-Level Security

-- Rol que usa Quarkus para todas las queries. La password viene como placeholder de Flyway.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'agromis_app') THEN
        EXECUTE format('CREATE ROLE agromis_app LOGIN PASSWORD %L', :'app_password');
    END IF;
END
$$;

GRANT CONNECT ON DATABASE agromis TO agromis_app;
GRANT USAGE ON SCHEMA public TO agromis_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO agromis_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO agromis_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO agromis_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO agromis_app;

-- Funcion helper para leer el tenant actual del contexto de la sesion
-- El backend hace: SET app.current_country = 'GT' al inicio de cada request
CREATE OR REPLACE FUNCTION current_country_code() RETURNS TEXT AS $$
  SELECT current_setting('app.current_country', true);
$$ LANGUAGE SQL STABLE SECURITY DEFINER;

-- RLS se habilita tabla por tabla en migraciones posteriores cuando el schema este mas maduro.
-- Ejemplo de como se activara:
-- ALTER TABLE farming_reports ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY tenant_isolation ON farming_reports
--   USING (country_code = current_country_code());
