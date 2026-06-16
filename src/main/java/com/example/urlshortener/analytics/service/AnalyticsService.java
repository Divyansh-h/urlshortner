package com.example.urlshortener.analytics.service;

import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.common.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlMappingRepository repository;

    @Transactional(readOnly = true)
    public UrlMapping getUrlStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found for short code: " + shortCode));
    }

    @Transactional(readOnly = true)
    public Long getTotalSystemClicks() {
        Long total = repository.getTotalClicks();
        return total != null ? total : 0L;
    }
}
