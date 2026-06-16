package com.example.urlshortener.creation.job;

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
public class UrlExpirationCleanupJob {

    private final UrlMappingRepository urlMappingRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "url::";

    /**
     * Runs at the top of every hour to clean up expired URLs.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting expired URL cleanup job...");
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Find all expired URLs
        List<UrlMapping> expiredUrls = urlMappingRepository.findExpiredUrls(now);

        if (expiredUrls.isEmpty()) {
            log.info("No expired URLs found.");
            return;
        }

        // 2. Remove from Redis Cache to ensure they aren't served from memory
        for (UrlMapping url : expiredUrls) {
            redisTemplate.delete(CACHE_PREFIX + url.getShortCode());
        }

        // 3. Delete from the Database (Hard Delete)
        int deletedCount = urlMappingRepository.deleteExpiredUrls(now);

        log.info("Finished expired URL cleanup. Deleted {} URLs from database and cache.", deletedCount);
    }
}
