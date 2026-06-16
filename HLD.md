# High Level Design (HLD): Scalable URL Shortener

## 1. System Architecture Diagram

```text
                                +-------------------+
                                |                   |
                                |   React Clients   | (Web Browser / Mobile)
                                |                   |
                                +---------+---------+
                                          |
                                          | (HTTPS)
                                          v
                                +-------------------+
                                |                   |
                                | CDN (Cloudflare/  | (Edge Redirection)
                                | AWS CloudFront)   |
                                |                   |
                                +---------+---------+
                                          |
                                          | (Cache Miss / API requests)
                                          v
                                +-------------------+
                                |                   |
                                |  Load Balancer    | (NGINX / AWS ALB)
                                |                   |
                                +---------+---------+
                                          |
                                          | (Routes requests)
                                          v
                             +------------+------------+
                             |                         |
                    +--------v-------+        +--------v-------+
                    |                |        |                |
                    | Spring Boot    |        | Spring Boot    |  (Application Servers)
                    | App Instance 1 |        | App Instance 2 |
                    |                |        |                |
                    +--------+-------+        +--------+-------+
                             |                         |
                             |                         |
                    +--------v-------------------------v-------+
                    |                                          |
                    |               Redis Cache                |  (Optional but recommended for read-heavy scaling)
                    |                                          |
                    +--------------------+---------------------+
                                         |
                                         |
                                +--------v--------+
                                |                 |
                                |   PostgreSQL    | (Primary Database)
                                |   (Master/Replica)
                                |                 |
                                +-----------------+
```

## 2. API Flow

### A. URL Shortening Flow (Write Path)
1. **User Input:** A user enters a long URL in the React frontend and clicks "Shorten".
2. **API Request:** The React app sends a `POST /api/v1/urls` request with the `{ "originalUrl": "https://example.com/very/long/url" }` payload to the Load Balancer.
3. **Routing:** The Load Balancer forwards the request to one of the available Spring Boot App Instances.
4. **Validation:** The Spring Boot backend validates the URL format.
5. **Generation:** The backend generates a unique short code (e.g., base62 encoding of a unique sequence or UUID).
6. **Persistence:** 
    - The backend saves the mapping `(shortCode, originalUrl, creationDate, userId)` in PostgreSQL.
    - *(Optional)* It simultaneously writes this mapping to the Redis Cache to speed up immediate reads.
7. **Response:** The backend returns the generated short URL (e.g., `https://short.ly/xyz123`) to the React app.

### B. URL Redirection Flow (Read Path)
1. **User Action:** A user clicks on the short URL `https://short.ly/xyz123`.
2. **DNS & Edge Routing:** The request hits the nearest CDN Edge Server.
3. **CDN Cache Lookup (Edge Redirection):**
    - The CDN checks its edge cache for the path `/xyz123`.
    - If found, the CDN immediately returns a `301 Moved Permanently` (or 302) to the user's browser. The request **never** reaches our backend servers (zero latency, infinite scale).
4. **Origin Request (Cache Miss):**
    - If not found at the edge, the CDN forwards the `GET /xyz123` request to our Load Balancer.
5. **Routing:** The Load Balancer forwards the request to a Spring Boot App Instance.
6. **Backend Cache Lookup (Redis):**
    - The backend checks Redis for `xyz123`. If found, it retrieves the `originalUrl`.
7. **Database Lookup:**
    - If not found in Redis, it queries PostgreSQL, then saves it to Redis.
8. **Redirection & Edge Caching:** 
    - The Spring Boot app responds with an HTTP `302 Found` (or `301`), including proper `Cache-Control` headers (e.g., `Cache-Control: public, max-age=3600`).
    - The CDN intercepts this response, caches the redirection locally for the specified duration, and forwards it to the user's browser.

## 3. Core Components Explanation

### React Frontend (Client Layer)
*   **Purpose:** Provides a user-friendly UI to input long URLs, view generated short URLs, and manage past links (if user authentication is added).
*   **Key Tech:** React, Axios (for API calls), TailwindCSS or similar for styling.
*   **Role in Scaling:** React apps are compiled to static assets (HTML/JS/CSS) which can be hosted on a CDN (Content Delivery Network). This makes the frontend infinitely scalable as it puts zero load on the application backend until an API call is made.

### Content Delivery Network (CDN - Edge Layer)
*   **Purpose:** The ultimate scaling layer for read-heavy workloads. CDNs like Cloudflare or AWS CloudFront cache static assets AND HTTP responses (URL redirections) geographically close to the user.
*   **Role in Scaling:** By caching the HTTP `301/302` redirects using `Cache-Control` headers, the CDN intercepts viral link clicks. If a link receives millions of clicks globally, the edge servers handle the redirection natively. This offloads up to 99% of read traffic from the Load Balancers, Spring Boot servers, and Database, guaranteeing global sub-10ms redirection latency.

### Load Balancer
*   **Purpose:** Distributes incoming traffic across multiple Spring Boot backend instances.
*   **Role in Scaling:** Prevents any single application server from becoming a bottleneck. If traffic spikes, more Spring Boot instances can be spun up, and the load balancer will start routing traffic to them seamlessly.

### Spring Boot Backend (Application Layer)
*   **Purpose:** Handles business logic: URL validation, short code generation, caching strategy, and database interactions.
*   **Key Tech:** Java, Spring Boot, Spring Web, Spring Data JPA.
*   **Short Code Generation Strategies:**
    *   **Base62 Encoding:** Generate a unique auto-incrementing ID in the DB (or via a dedicated service like Twitter Snowflake), then convert that integer to Base62 (A-Z, a-z, 0-9).
    *   **Hashing:** MD5 or SHA-256 hash the long URL, then take the first 6-8 characters. (Requires collision handling).
*   **Role in Scaling:** Spring Boot applications are stateless. Any request can go to any instance, making horizontal scaling easy.

### PostgreSQL (Database Layer)
*   **Purpose:** The single source of truth for persistent data storage. Stores the URL mappings, user data, and analytics (click counts).
*   **Schema Design (Simplified):**
    *   `urls` table: `id` (PK), `short_code` (Unique Index), `original_url`, `created_at`, `expires_at`, `click_count`.
*   **Role in Scaling:** Relational databases can handle significant scale if indexed correctly. The `short_code` column MUST be indexed for fast read lookups. As read traffic grows, you can configure PostgreSQL Read Replicas.

### Redis (Caching Layer - Critical for High Scale)
*   **Purpose:** URL shorteners are incredibly read-heavy (many more redirects happen than link creations). Querying the DB for every single click will quickly overwhelm PostgreSQL.
*   **Mechanism:** Redis stores the `shortCode -> originalUrl` mapping in RAM. When a read request comes in, the backend checks Redis first. Since RAM lookups take microseconds, this drastically reduces latency and database load.
*   **Eviction Policy:** Uses an LRU (Least Recently Used) policy so that infrequently accessed short links are dropped from the cache, while viral links stay in memory.

## 4. Distributed Cache Strategy

As the system scales to handle millions of requests, a single Redis instance becomes a bottleneck. We employ a distributed cache architecture.

### Redis Cluster Design
Instead of a standalone server, we use a **Redis Cluster**.
*   **Multiple Master Nodes:** Data is split across several Master nodes to multiply available RAM and spread CPU/network load.
*   **High Availability:** Each Master has one or more Replicas. If a Master crashes, a Replica is automatically promoted to Master without manual intervention.

### Cache Sharding (Data Partitioning)
Data must be distributed evenly across all Master nodes to prevent hot spots. Redis Cluster achieves this using **Hash Slots** (there are exactly 16,384 slots).
*   **Algorithm:** `HASH_SLOT = CRC16(KEY) mod 16384`
*   Because our short codes are uniformly distributed (via Base62 encoding of Snowflake IDs), the CRC16 hash naturally spreads the URLs evenly across all slots and, consequently, across all Master nodes.
*   For extreme viral links (the "thundering herd" problem), we employ a Two-Tier Cache (L1 Caffeine in JVM + L2 Redis Cluster) to prevent overwhelming a single Redis shard.