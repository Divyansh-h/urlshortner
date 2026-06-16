package com.example.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlShortenResponse {
    private String shortUrl;
    private String originalUrl;
    private OffsetDateTime createdAt;
}