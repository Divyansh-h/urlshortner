package com.example.urlshortener.creation.controller;

import com.example.urlshortener.creation.dto.UrlShortenRequest;
import com.example.urlshortener.creation.dto.UrlShortenResponse;
import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.creation.service.ShortenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ShortenController {

    private final ShortenService shortenService;

    @Value("${app.domain:http://localhost:8080/}")
    private String domain;

    @PostMapping("/api/shorten")
    public ResponseEntity<UrlShortenResponse> createShortUrl(@Valid @RequestBody UrlShortenRequest request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());
        
        UrlMapping mapping = shortenService.createShortUrl(request.getOriginalUrl(), request.getCustomAlias());
        
        String shortUrl = domain + mapping.getShortCode();
        UrlShortenResponse response = new UrlShortenResponse(shortUrl, mapping.getOriginalUrl(), mapping.getCreatedAt());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
