/**
 * Escenario: Consulta de tiles GIS
 * Simula un usuario navegando el mapa de parcelas
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from '../common/auth.js';

const API_URL = __ENV.API_URL || 'https://api-staging.agromis.io';

const tileDuration = new Trend('gis_tile_duration', true);
const tileErrors   = new Rate('gis_tile_errors');

export const options = {
  scenarios: {
    map_browsing: {
      executor: 'ramping-vus',
      stages: [
        { target: 50,  duration: '30s' },
        { target: 100, duration: '2m' },
        { target: 50,  duration: '30s' },
      ],
    },
  },
  thresholds: {
    gis_tile_duration: ['p(95)<1000'],
    gis_tile_errors:   ['rate<0.02'],
  },
};

export function setup() {
  return { token: getToken() };
}

export default function (data) {
  const headers = authHeaders(data.token);

  // Simular zoom 7-10 en Centroamerica
  const zoom = 7 + Math.floor(Math.random() * 4);
  const x    = 20 + Math.floor(Math.random() * 10);
  const y    = 55 + Math.floor(Math.random() * 10);

  const start    = Date.now();
  const response = http.get(
    `${API_URL}/api/v1/gis/tiles/${zoom}/${x}/${y}.mvt?country_code=GT`,
    { headers }
  );
  tileDuration.add(Date.now() - start);

  const ok = check(response, {
    'status 200 o 204': (r) => r.status === 200 || r.status === 204,
  });
  tileErrors.add(!ok);
  sleep(Math.random() * 2 + 0.5);
}
