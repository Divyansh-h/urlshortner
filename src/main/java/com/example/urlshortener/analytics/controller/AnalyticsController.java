package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.analytics.dto.UrlStatsResponse;
import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable String shortCode) {
        UrlMapping mapping = analyticsService.getUrlStats(shortCode);
        
        UrlStatsResponse response = new UrlStatsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/stats/total")
    public ResponseEntity<Map<String, Long>> getTotalStats() {
        Long totalClicks = analyticsService.getTotalSystemClicks();
        return ResponseEntity.ok(Map.of("totalSystemClicks", totalClicks));
    }
}
