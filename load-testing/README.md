# URL Shortener - Load Testing Suite

This directory contains automated performance tests written using [K6](https://k6.io/).

## Prerequisites
You need K6 installed on your machine.
- **Mac:** `brew install k6`
- **Linux:** `sudo apt-get install k6`
- **Docker:** You can run K6 via Docker without installing it.

## The Scenarios

1. **Read-Heavy (`read-heavy.js`)**
   - Tests the scenario where a single short URL goes viral.
   - Designed to validate the Redis caching layer (expecting sub-10ms response times).
   - **Command:** `k6 run load-testing/read-heavy.js`

2. **Write-Heavy (`write-heavy.js`)**
   - Tests bulk URL creation.
   - Validates PostgreSQL connection pooling and Snowflake ID generation.
   - **Command:** `k6 run load-testing/write-heavy.js`
   - *Note:* You may need to pass a valid API Key: `k6 run -e API_KEY=your-key load-testing/write-heavy.js`

3. **Spike Test (`spike-test.js`)**
   - Simulates a sudden traffic surge from 0 to 1,000 concurrent users.
   - Tests system resilience and graceful degradation.
   - **Command:** `k6 run load-testing/spike-test.js`

## Running via Docker
If you don't have K6 installed locally, you can run the tests using Docker:

```bash
docker run --rm -i grafana/k6 run - <load-testing/read-heavy.js
```
*(Note: If testing against localhost from inside a Docker container on Mac/Windows, you may need to use `host.docker.internal` instead of `localhost` in the scripts, e.g., `k6 run -e TARGET_URL=http://host.docker.internal:8080/TEST1234 load-testing/read-heavy.js`)*
