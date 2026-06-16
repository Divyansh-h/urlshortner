import http from 'k6/http';
import { check, sleep } from 'k6';

// Read-Heavy Scenario: Viral URL
// Simulates millions of users clicking the same short URL simultaneously.
// Tests the effectiveness of the Redis cache.

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up to 50 users
    { duration: '1m', target: 500 },  // Spike to 500 concurrent users
    { duration: '30s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    // 95% of read requests must complete below 10ms (from Redis)
    http_req_duration: ['p(95)<10'],
    // Less than 1% error rate
    http_req_failed: ['rate<0.01'],
  },
};

// You should replace 'TEST1234' with an actual short code present in your database.
const TARGET_URL = __ENV.TARGET_URL || 'http://localhost:8080/TEST1234';

export default function () {
  const res = http.get(TARGET_URL, { redirects: 0 }); // We don't follow redirects to measure exactly the backend response time
  
  check(res, {
    'is status 302': (r) => r.status === 302,
    'has location header': (r) => r.headers['Location'] !== undefined,
  });

  // Short sleep to simulate real user pacing
  sleep(0.1);
}
