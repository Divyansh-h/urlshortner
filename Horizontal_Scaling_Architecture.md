# Horizontal Scaling Architecture

To support millions of global users and high-throughput URL shorten/redirect operations, the backend must be horizontally scalable. This means adding more traffic capacity simply by spinning up additional Spring Boot application instances, rather than upgrading a single server's CPU/RAM (vertical scaling).

This document details the mechanics of our horizontal scaling strategy.

## 1. Load Balancer (NGINX / AWS ALB)

The Load Balancer acts as the single entry point for all API traffic (from the React clients or CDN Edge misses). 

### Responsibilities:
*   **Traffic Distribution:** Distributes incoming HTTP requests evenly across all healthy Spring Boot instances using routing algorithms like Round Robin or Least Connections.
*   **Health Checks:** Continuously pings the `/actuator/health` endpoint of each Spring Boot instance. If an instance crashes or becomes unresponsive, the Load Balancer instantly removes it from the routing pool.
*   **SSL Termination:** Decrypts incoming HTTPS traffic at the edge, sending unencrypted HTTP traffic to the internal Spring Boot instances. This offloads CPU-intensive cryptography from the application servers.

## 2. Stateless Backend Instances

The cornerstone of horizontal scaling is ensuring that the Spring Boot application servers are **100% Stateless**. 
Any request must be able to be served by *any* instance without relying on local server memory from previous requests.

### Key Stateless Design Decisions:
1.  **No HTTP Sessions:** We do not use Tomcat's `HttpSession`. If user authentication is added, it will be strictly token-based (e.g., stateless JWTs). The server validates the token mathematically without needing server-side session state.
2.  **Distributed Caching:** Rather than using local, single-node `ConcurrentHashMap` caching for core read operations, we use a **Redis Cluster**. If Instance 1 caches a URL and a subsequent request goes to Instance 2, Instance 2 can still access the cached data from Redis.
3.  *(Exception)* **L1 Caching / Buffering:** While core data is in Redis, we use transient local memory for things like the `ClickAnalyticsBatchJob` buffer. Since clicks are merely aggregated integers, it doesn't matter which server holds the buffer; they independently flush their unique local counts to the centralized database.

## 3. Distributed Concurrency & Synchronization

When multiple backend instances run simultaneously, they will race to access the same centralized resources (PostgreSQL, Redis). Our design handles distributed concurrency safely:

### Short Code Collision Prevention
We cannot rely on a single database `AUTO_INCREMENT` sequence efficiently if thousands of requests hit different servers simultaneously. 
*   **Solution:** We use a distributed ID generator like **Twitter Snowflake**. Snowflake generates 64-bit IDs that are guaranteed to be globally unique across all application instances without requiring network coordination or database locking.

### Click Analytics Batching
*   **Solution:** When different servers flush analytics, they perform atomic SQL updates: `UPDATE urls SET click_count = click_count + ? WHERE short_code = ?`. Because this is an atomic increment, Instance 1 and Instance 2 will never overwrite each other's click counts.

## 4. Auto-Scaling Groups

In a cloud environment (e.g., AWS EC2 Auto Scaling or Kubernetes HPA), the number of backend instances dynamically fluctuates based on demand.
*   **Scale Out:** If average CPU utilization exceeds 70%, the infrastructure spins up Instance 3 and Instance 4, automatically registering them with the Load Balancer.
*   **Scale In:** During off-peak hours, unneeded instances are gracefully terminated to save costs, leaving a baseline of instances (e.g., 2) for redundancy.
