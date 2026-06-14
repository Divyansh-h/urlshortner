package com.example.urlshortener.controller;

import com.example.urlshortener.dto.UrlShortenRequest;
import com.example.urlshortener.dto.UrlShortenResponse;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;

    @Value("${app.domain:http://localhost:8080/}")
    private String domain;

    /**
     * Endpoint to create a new short URL.
     * 
     * @param request The request containing the original URL and optional custom alias.
     * @return The created short URL details with HTTP 201 Created.
     */
    @PostMapping("/api/shorten")
    public ResponseEntity<UrlShortenResponse> createShortUrl(@Valid @RequestBody UrlShortenRequest request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());
        
        // Controller extracts values from DTO and passes primitives/domain objects to the Service
        UrlMapping mapping = urlService.createShortUrl(request.getOriginalUrl(), request.getCustomAlias());
        
        // Controller maps the domain result back to an HTTP-specific DTO
        String shortUrl = domain + mapping.getShortCode();
        UrlShortenResponse response = new UrlShortenResponse(shortUrl, mapping.getOriginalUrl(), mapping.getCreatedAt());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint to redirect the user to the original URL based on the short code.
     * 
     * @param shortCode The short alias to look up.
     * @return HTTP 302 Found with the Location header pointing to the original URL.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
        log.info("Received request to redirect shortCode: {}", shortCode);
        String originalUrl = urlService.getOriginalUrl(shortCode);
        
        // Return 302 Found redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}