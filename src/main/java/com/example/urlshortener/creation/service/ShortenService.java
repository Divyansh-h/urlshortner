package com.example.urlshortener.creation.service;

import com.example.urlshortener.common.exception.MaliciousUrlException;
import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.common.repository.UrlMappingRepository;
import com.example.urlshortener.common.security.MaliciousUrlDetector;
import com.example.urlshortener.common.service.FeatureFlagService;
import com.example.urlshortener.common.util.Base62Encoder;
import com.example.urlshortener.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortenService {

    private final UrlMappingRepository repository;
    private final Base62Encoder base62Encoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final StringRedisTemplate redisTemplate;
    private final MaliciousUrlDetector maliciousUrlDetector;
    private final FeatureFlagService featureFlagService;

    private static final String CACHE_PREFIX = "urls::";

    @Transactional
    public UrlMapping createShortUrl(String originalUrl, String customAlias) {
        log.debug("Creating short URL for: {}", originalUrl);
        
        if (featureFlagService.isEnabled("MALICIOUS_URL_DETECTION")) {
            if (maliciousUrlDetector.isMalicious(originalUrl)) {
                log.warn("Blocked attempt to shorten malicious URL: {}", originalUrl);
                throw new MaliciousUrlException("The provided URL has been flagged as malicious and cannot be shortened.");
            }
        }

        String shortCode;
        
        if (customAlias != null && !customAlias.isBlank()) {
            if (repository.existsByShortCode(customAlias)) {
                log.warn("Custom alias collision detected: {}", customAlias);
                throw new IllegalArgumentException("Alias already in use");
            }
            shortCode = customAlias;
        } else {
            long uniqueId = snowflakeIdGenerator.nextId();
            shortCode = base62Encoder.encode(uniqueId);
        }

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .build();
                
        mapping = repository.saveAndFlush(mapping);

        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, originalUrl, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("Redis unavailable. Skipping cache for short code: {}", shortCode, e);
        }

        return mapping;
    }
}
