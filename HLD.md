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
2. **API Request:** The browser sends a `GET /xyz123` request.
3. **Routing:** The Load Balancer forwards the request to a Spring Boot App Instance.
4. **Cache Lookup (Optional but critical for scale):**
    - The backend checks Redis for the key `xyz123`.
    - If found (Cache Hit), it retrieves the `originalUrl` immediately.
5. **Database Lookup:**
    - If not found in Redis (Cache Miss), the backend queries PostgreSQL for the `shortCode`.
    - If found, it saves the result in Redis for future requests.
6. **Redirection:** The Spring Boot app responds with an HTTP `302 Found` (or `301 Moved Permanently`) status, with the `Location` header set to the `originalUrl`.
7. **Browser Redirect:** The user's browser follows the `Location` header to the destination site.

## 3. Core Components Explanation

### React Frontend (Client Layer)
*   **Purpose:** Provides a user-friendly UI to input long URLs, view generated short URLs, and manage past links (if user authentication is added).
*   **Key Tech:** React, Axios (for API calls), TailwindCSS or similar for styling.
*   **Role in Scaling:** React apps are compiled to static assets (HTML/JS/CSS) which can be hosted on a CDN (Content Delivery Network). This makes the frontend infinitely scalable as it puts zero load on the application backend until an API call is made.

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