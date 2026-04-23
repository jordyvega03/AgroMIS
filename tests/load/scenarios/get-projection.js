/**
 * Escenario: Consulta de proyeccion de cosecha
 * Flujo tipico de un analista consultando proyecciones por municipio
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from '../common/auth.js';

const API_URL = __ENV.API_URL || 'https://api-staging.agromis.io';

const projDuration = new Trend('projection_query_duration', true);
const projErrors   = new Rate('projection_query_errors');

export const options = {
  scenarios: {
    read_heavy: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 300,
    },
  },
  thresholds: {
    projection_query_duration: ['p(95)<800', 'p(99)<2000'],
    projection_query_errors:   ['rate<0.01'],
  },
};

const CROPS    = ['TOMATO', 'ONION', 'MAIZE', 'BEAN'];
const H3_CELLS = ['8765432100fffff', '8765432101fffff', '8765432102fffff'];

export function setup() {
  return { token: getToken() };
}

export default function (data) {
  const headers = authHeaders(data.token);
  const crop    = CROPS[Math.floor(Math.random() * CROPS.length)];
  const cell    = H3_CELLS[Math.floor(Math.random() * H3_CELLS.length)];

  const start    = Date.now();
  const response = http.get(
    `${API_URL}/api/v1/projections?crop_code=${crop}&h3_cell=${cell}&country_code=GT`,
    { headers }
  );
  projDuration.add(Date.now() - start);

  const ok = check(response, {
    'status es 200': (r) => r.status === 200,
  });
  projErrors.add(!ok);
  sleep(0.05);
}
