package com.example.urlshortener.service.impl;

import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlMappingRepository repository;
    private final Base62Encoder base62Encoder;

    private static final int SHORT_CODE_LENGTH = 7;

    @Override
    @Transactional
    public UrlMapping createShortUrl(String originalUrl, String customAlias) {
        log.debug("Creating short URL for: {}", originalUrl);
        String shortCode;
        
        if (customAlias != null && !customAlias.isBlank()) {
            if (repository.existsByShortCode(customAlias)) {
                log.warn("Custom alias collision detected: {}", customAlias);
                throw new IllegalArgumentException("Alias already in use");
            }
            shortCode = customAlias;
        } else {
            shortCode = generateShortCode();
        }

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .build();
                
        mapping = repository.saveAndFlush(mapping); // Flush to ensure createdAt is populated if DB generated

        log.info("Successfully created short URL {} for {}", shortCode, originalUrl);
        return mapping;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "urls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        log.debug("Cache miss. Fetching original URL from DB for short code: {}", shortCode);
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    log.warn("Original URL not found in database for short code: {}", shortCode);
                    return new ResourceNotFoundException("URL not found for short code: " + shortCode);
                });
        
        return mapping.getOriginalUrl();
    }

    @Override
    public String generateShortCode() {
        String shortCode;
        do {
            shortCode = base62Encoder.generateRandomString(SHORT_CODE_LENGTH);
        } while (repository.existsByShortCode(shortCode)); // Collision handling
        log.debug("Generated random short code: {}", shortCode);
        return shortCode;
    }
}