import http from 'k6/http';
import { check, sleep } from 'k6';

// Write-Heavy Scenario: Bot Creation
// Simulates an API client generating thousands of short URLs rapidly.
// Tests PostgreSQL write performance, Snowflake ID generation, and Base62 encoding.

export const options = {
  stages: [
    { duration: '15s', target: 20 },  // Ramp-up
    { duration: '1m', target: 100 },  // Sustained load
    { duration: '15s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    // 95% of write requests must complete below 50ms
    http_req_duration: ['p(95)<50'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1/urls';

// Provide a valid test API Key if your system requires one for creation
const API_KEY = __ENV.API_KEY || 'test-api-key';

export default function () {
  const payload = JSON.stringify({
    originalUrl: `https://example.com/test/${__VU}/${__ITER}`,
    // Optional: Add customAlias occasionally
    // customAlias: __ITER % 10 === 0 ? `custom-${__VU}-${__ITER}` : null
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': API_KEY, // Assuming the API Key filter looks for this header
    },
  };

  const res = http.post(BASE_URL, payload, params);

  check(res, {
    'is status 201': (r) => r.status === 201,
    'has short code': (r) => r.json('shortCode') !== undefined,
  });

  sleep(0.5);
}
