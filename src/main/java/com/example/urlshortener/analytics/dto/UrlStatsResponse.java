package com.example.urlshortener.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlStatsResponse {
    private String shortCode;
    private String originalUrl;
    private Long clickCount;
    private OffsetDateTime createdAt;
}