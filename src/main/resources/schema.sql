-- PostgreSQL Schema for URL Shortener

CREATE TABLE IF NOT EXISTS urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(15) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_time TIMESTAMP WITH TIME ZONE,
    click_count BIGINT DEFAULT 0 NOT NULL
);

-- Indexing Strategy

-- 1. Unique Index on short_code
-- Required because all redirection lookups are done by matching the short_code.
-- It ensures that we don't accidentally generate or allow duplicate aliases.
CREATE UNIQUE INDEX IF NOT EXISTS idx_urls_short_code ON urls (short_code);

-- 2. Optional: Index on original_url
-- Useful if you want to prevent users from shortening the exact same long URL multiple times,
-- allowing you to return the existing short_code instead of creating a new one.
-- Depending on requirements, a Hash index might be more efficient here since we only care about equality checks.
-- CREATE INDEX IF NOT EXISTS idx_urls_original_url_hash ON urls USING HASH (original_url);

-- 3. Optional: Index on expiry_time
-- Highly recommended if you have a background worker (e.g., cron job or Spring Scheduled task) 
-- that periodically cleans up expired URLs from the database.
-- B-Tree index is perfect for range queries (e.g., WHERE expiry_time < NOW())
-- CREATE INDEX IF NOT EXISTS idx_urls_expiry_time ON urls (expiry_time);
