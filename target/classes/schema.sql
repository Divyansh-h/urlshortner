-- PostgreSQL Schema for URL Shortener

CREATE TABLE IF NOT EXISTS urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(15) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_time TIMESTAMP WITH TIME ZONE,
    click_count BIGINT DEFAULT 0 NOT NULL,
    user_id VARCHAR(50) -- Optional: Identifies which user created the link
);

-- ==========================================
-- INDEXING STRATEGY FOR HIGH TRAFFIC
-- ==========================================

-- 1. Primary Lookup Index (B-Tree)
-- Required because all redirection lookups are done by matching the short_code.
-- B-Tree provides O(log N) lookups. Unique constraint enforces data integrity.
CREATE UNIQUE INDEX IF NOT EXISTS idx_urls_short_code ON urls (short_code);

-- 2. Partial Indexing (For Expiry Cron Job)
-- Problem: Our cron job runs `DELETE FROM urls WHERE expiry_time < NOW()`. 
-- If 95% of our URLs are permanent (expiry_time IS NULL), a standard index on `expiry_time` 
-- would waste massive amounts of RAM storing NULL values.
-- Solution: A Partial Index. It ONLY indexes rows where expiry_time is NOT NULL.
-- This keeps the index incredibly small, fast, and memory-efficient.
CREATE INDEX IF NOT EXISTS idx_urls_expiry_partial 
ON urls (expiry_time) 
WHERE expiry_time IS NOT NULL;

-- 3. Composite Index (For User Dashboards)
-- Scenario: If a user logs into a dashboard to view their links, the query is:
-- `SELECT * FROM urls WHERE user_id = 'user123' ORDER BY created_at DESC LIMIT 20;`
-- Solution: A composite index on (user_id, created_at DESC). 
-- This allows the database to instantly find the user's records AND already have them 
-- perfectly sorted by creation date, entirely avoiding expensive "Filesort" operations in memory.
CREATE INDEX IF NOT EXISTS idx_urls_user_created_desc 
ON urls (user_id, created_at DESC);

-- 4. Hash Index (For Collision/Duplication Checks)
-- If we frequently check if a long URL has already been shortened:
-- `SELECT short_code FROM urls WHERE original_url = 'https://verylong...'`
-- Since we only do exact equality checks (=) and never range queries (>, <) on the long URL,
-- a Hash index is typically smaller and faster than a B-Tree for this specific task.
-- CREATE INDEX IF NOT EXISTS idx_urls_original_hash ON urls USING HASH (original_url);
