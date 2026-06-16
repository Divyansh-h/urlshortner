package com.example.urlshortener.analytics.job;

import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.common.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredUrlCleanupJob {

    private final UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;
    
    private static final String CACHE_PREFIX = "urls::";

    /**
     * Runs every hour to clean up expired URLs from both the database and Redis.
     * Uses fixedRate = 3600000 ms (1 hour).
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredUrls() {
        OffsetDateTime now = OffsetDateTime.now();
        log.info("Starting scheduled cleanup job for expired URLs at {}", now);

        // 1. Find all expired URLs
        List<UrlMapping> expiredUrls = repository.findExpiredUrls(now);

        if (expiredUrls.isEmpty()) {
            log.info("No expired URLs found to clean up.");
            return;
        }

        log.info("Found {} expired URLs. Proceeding with cache invalidation and database deletion.", expiredUrls.size());

        // 2. Invalidate Redis Cache for each expired URL
        for (UrlMapping url : expiredUrls) {
            String cacheKey = CACHE_PREFIX + url.getShortCode();
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated Redis cache for expired short code: {}", url.getShortCode());
        }

        // 3. Delete from PostgreSQL Database
        int deletedCount = repository.deleteExpiredUrls(now);
        log.info("Successfully deleted {} expired URLs from the database.", deletedCount);
    }
}