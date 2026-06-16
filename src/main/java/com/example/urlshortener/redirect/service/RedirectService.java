package com.example.urlshortener.redirect.service;

import com.example.urlshortener.analytics.event.UrlClickedEvent;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.common.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CACHE_PREFIX = "urls::";

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        eventPublisher.publishEvent(new UrlClickedEvent(this, shortCode));
        
        String cachedUrl = null;
        try {
            cachedUrl = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Redis unavailable. Falling back to PostgreSQL for short code: {}", shortCode, e);
        }
        
        if (cachedUrl != null) {
            return cachedUrl;
        }

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found for short code: " + shortCode));
        
        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, mapping.getOriginalUrl(), Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("Redis unavailable. Failed to cache retrieved short URL: {}", shortCode, e);
        }
        
        return mapping.getOriginalUrl();
    }
}
