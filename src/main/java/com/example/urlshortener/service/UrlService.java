package com.example.urlshortener.service;

import com.example.urlshortener.model.UrlMapping;

public interface UrlService {
    UrlMapping createShortUrl(String originalUrl, String customAlias);
    String getOriginalUrl(String shortCode);
    String generateShortCode();
}