-- V005: Catalogo inicial de cultivos piloto para Guatemala

INSERT INTO crop_catalog (country_code, crop_code, common_name, scientific_name, typical_cycle_days, category) VALUES
  ('GT', 'TOMATO',  'Tomate',        'Solanum lycopersicum',  75,  'VEGETABLE'),
  ('GT', 'ONION',   'Cebolla',       'Allium cepa',           120, 'VEGETABLE'),
  ('GT', 'POTATO',  'Papa',          'Solanum tuberosum',     100, 'VEGETABLE'),
  ('GT', 'MAIZE',   'Maiz',          'Zea mays',              120, 'GRAIN'),
  ('GT', 'BEAN',    'Frijol',        'Phaseolus vulgaris',    90,  'GRAIN'),
  ('GT', 'CARROT',  'Zanahoria',     'Daucus carota',         80,  'VEGETABLE'),
  ('GT', 'CABBAGE', 'Repollo',       'Brassica oleracea',     75,  'VEGETABLE'),
  ('GT', 'PEPPER',  'Chile pimiento','Capsicum annuum',        85,  'VEGETABLE'),
  ('GT', 'BROCCOLI','Brocoli',       'Brassica oleracea var. italica', 70, 'VEGETABLE'),
  ('GT', 'COFFEE',  'Cafe',          'Coffea arabica',        1095,'EXPORT_CROP')
ON CONFLICT (country_code, crop_code) DO NOTHING;
