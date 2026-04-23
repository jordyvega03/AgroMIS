/**
 * Escenario: Envio de reporte de siembra
 * Target: p95 < 500ms a 100 RPS sostenidos
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { getToken, authHeaders } from '../common/auth.js';

const API_URL = __ENV.API_URL || 'https://api-staging.agromis.io';

const submitDuration = new Trend('submit_report_duration', true);
const submitErrors   = new Rate('submit_report_errors');
const submitCount    = new Counter('submit_report_count');

export const options = {
  scenarios: {
    sustained_load: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 150,
      maxVUs: 300,
    },
    ramp_up: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      stages: [
        { target: 50,  duration: '1m' },
        { target: 100, duration: '2m' },
        { target: 150, duration: '1m' },
        { target: 0,   duration: '30s' },
      ],
      preAllocatedVUs: 200,
    },
  },
  thresholds: {
    submit_report_duration: ['p(95)<500'],
    submit_report_errors:   ['rate<0.01'],
    http_req_failed:        ['rate<0.01'],
  },
};

let token;

export function setup() {
  token = getToken();
  return { token };
}

export default function (data) {
  const headers = authHeaders(data.token);

  const payload = JSON.stringify({
    parcel_id: `parcel-load-test-${Math.floor(Math.random() * 1000)}`,
    crop_code: ['TOMATO', 'ONION', 'POTATO', 'MAIZE', 'BEAN'][Math.floor(Math.random() * 5)],
    planting_date: new Date().toISOString().split('T')[0],
    area_hectares: parseFloat((Math.random() * 5 + 0.5).toFixed(2)),
    notes: 'Reporte generado por load test k6',
  });

  const start = Date.now();
  const response = http.post(`${API_URL}/api/v1/farming/reports`, payload, { headers });
  const duration = Date.now() - start;

  submitDuration.add(duration);
  submitCount.add(1);

  const ok = check(response, {
    'status es 201 o 200': (r) => r.status === 201 || r.status === 200,
    'response tiene report_id': (r) => {
      try { return JSON.parse(r.body).report_id !== undefined; } catch { return false; }
    },
  });

  submitErrors.add(!ok);
  sleep(0.1);
}
