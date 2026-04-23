-- V004: Seed paises de Centroamerica

INSERT INTO countries (code, name, iso_3, currency_code, timezone, default_language, metadata) VALUES
  ('GT', 'Guatemala',    'GTM', 'GTQ', 'America/Guatemala',    'spa', '{"calling_code": "+502", "capital": "Guatemala City"}'::jsonb),
  ('SV', 'El Salvador',  'SLV', 'USD', 'America/El_Salvador',  'spa', '{"calling_code": "+503", "capital": "San Salvador"}'::jsonb),
  ('HN', 'Honduras',     'HND', 'HNL', 'America/Tegucigalpa',  'spa', '{"calling_code": "+504", "capital": "Tegucigalpa"}'::jsonb),
  ('NI', 'Nicaragua',    'NIC', 'NIO', 'America/Managua',      'spa', '{"calling_code": "+505", "capital": "Managua"}'::jsonb),
  ('CR', 'Costa Rica',   'CRI', 'CRC', 'America/Costa_Rica',   'spa', '{"calling_code": "+506", "capital": "San Jose"}'::jsonb),
  ('PA', 'Panama',       'PAN', 'PAB', 'America/Panama',       'spa', '{"calling_code": "+507", "capital": "Panama City"}'::jsonb),
  ('BZ', 'Belize',       'BLZ', 'BZD', 'America/Belize',       'eng', '{"calling_code": "+501", "capital": "Belmopan"}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- Tenant MVP para Guatemala
INSERT INTO tenants (code, country_code, name) VALUES
  ('gt-pilot', 'GT', 'AgroMIS Guatemala Pilot')
ON CONFLICT (code) DO NOTHING;
