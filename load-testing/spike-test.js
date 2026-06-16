import http from 'k6/http';
import { check, sleep } from 'k6';

// Spike Test Scenario
// A sudden and extreme surge in traffic (e.g., a viral moment or TV ad).
// Tests how the system behaves under sudden stress and if it recovers gracefully.

export const options = {
  stages: [
    { duration: '10s', target: 10 },   // Normal load
    { duration: '10s', target: 1000 }, // Spike to 1000 VUs in 10s
    { duration: '3m', target: 1000 },  // Sustain the spike
    { duration: '10s', target: 10 },   // Scale down
    { duration: '1m', target: 10 },    // Recovery period
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    // We allow higher latency during spikes, but ensure it doesn't completely fail
    http_req_duration: ['p(95)<500'], // 95% under 500ms
    http_req_failed: ['rate<0.05'],   // Less than 5% error rate allowed during extreme spike
  },
};

const TARGET_URL = __ENV.TARGET_URL || 'http://localhost:8080/TEST1234';

export default function () {
  const res = http.get(TARGET_URL, { redirects: 0 });
  
  check(res, {
    'is status 302': (r) => r.status === 302,
  });

  sleep(1);
}
