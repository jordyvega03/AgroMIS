import http from 'k6/http';

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'https://keycloak.agromis.io';
const REALM = __ENV.KC_REALM || 'agromis';
const CLIENT_ID = __ENV.KC_CLIENT_ID || 'agromis-mobile';
const USERNAME = __ENV.TEST_USERNAME || 'load-test-user';
const PASSWORD = __ENV.TEST_PASSWORD || 'load-test-pass';

export function getToken() {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

  const payload = {
    grant_type: 'password',
    client_id: CLIENT_ID,
    username: USERNAME,
    password: PASSWORD,
    scope: 'openid',
  };

  const response = http.post(tokenUrl, payload);

  if (response.status !== 200) {
    throw new Error(`Error obteniendo token: ${response.status} ${response.body}`);
  }

  return JSON.parse(response.body).access_token;
}

export function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}
