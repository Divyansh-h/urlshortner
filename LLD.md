# Low Level Design (LLD): URL Shortener Backend

This document outlines the Low Level Design for the Spring Boot backend, following layered/clean architecture principles (Presentation Layer -> Use Case / Service Layer -> Data Access / Repository Layer -> Database).

## Package Structure
```text
com.example.urlshortener
│
├── controller      # Presentation Layer (REST APIs)
├── dto             # Data Transfer Objects
├── model           # Domain Entities / JPA Entities
├── repository      # Data Access Layer
├── service         # Business Logic / Use Cases
└── exception       # Global Exception Handling
```

---

## 1. Entity Classes (Domain / Model Layer)

The entity represents the schema in the PostgreSQL database.

```java
package com.example.urlshortener.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Long clickCount = 0L;

    public UrlMapping(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }
}
```

---

## 2. Data Transfer Objects (DTOs)

DTOs are used to decouple the external API contracts from the internal database entities.

### Request DTO
```java
package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class UrlShortenRequest {
    @NotBlank(message = "URL cannot be empty")
    @URL(message = "Invalid URL format")
    private String originalUrl;
    
    // Optional: Allow users to specify custom alias or expiry
    private String customAlias;
}
```

### Response DTO
```java
package com.example.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UrlShortenResponse {
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime createdAt;
}
```

---

## 3. Repository Layer (Data Access Layer)

This layer abstracts the database interactions. Spring Data JPA provides the implementation.

```java
package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    
    // Find mapping by short code for redirection
    Optional<UrlMapping> findByShortCode(String shortCode);

    // Optional: Check if a custom alias is already taken
    boolean existsByShortCode(String shortCode);

    // Increment click count safely handling concurrency
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(String shortCode);
}
```

---

## 4. Service Layer (Business Logic)

The service layer orchestrates business rules (validation, short code generation, caching).

### Service Interface
```java
package com.example.urlshortener.service;

import com.example.urlshortener.dto.UrlShortenRequest;
import com.example.urlshortener.dto.UrlShortenResponse;

public interface UrlService {
    UrlShortenResponse createShortUrl(UrlShortenRequest request);
    String getOriginalUrl(String shortCode);
}
```

### Service Implementation
```java
package com.example.urlshortener.service.impl;

import com.example.urlshortener.dto.UrlShortenRequest;
import com.example.urlshortener.dto.UrlShortenResponse;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.util.Base62Encoder; // Assume this utility exists
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlMappingRepository repository;
    private final Base62Encoder base62Encoder; // Component for Base62 logic

    @Value("${app.domain}")
    private String domain; // e.g., https://short.ly/

    @Override
    @Transactional
    public UrlShortenResponse createShortUrl(UrlShortenRequest request) {
        String shortCode;
        
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (repository.existsByShortCode(request.getCustomAlias())) {
                throw new IllegalArgumentException("Alias already in use");
            }
            shortCode = request.getCustomAlias();
        } else {
            // Generate unique short code (simplified: using random or DB sequence + base62)
            shortCode = base62Encoder.generate();
            while (repository.existsByShortCode(shortCode)) {
                shortCode = base62Encoder.generate(); // Handle collisions
            }
        }

        UrlMapping mapping = new UrlMapping(shortCode, request.getOriginalUrl(), null);
        repository.save(mapping);

        String shortUrl = domain + shortCode;
        return new UrlShortenResponse(shortUrl, mapping.getOriginalUrl(), mapping.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "urls", key = "#shortCode") // Redis Caching Integration
    public String getOriginalUrl(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));
        
        // Asynchronous click event could be fired here via Kafka/RabbitMQ or Async method
        // to update clickCount without blocking the redirect.
        
        return mapping.getOriginalUrl();
    }
}
```

---

## 5. Controller Structure (Presentation Layer)

The controller handles incoming HTTP requests and responses. It relies entirely on the Service interface, decoupling it from the database.

```java
package com.example.urlshortener.controller;

import com.example.urlshortener.dto.UrlShortenRequest;
import com.example.urlshortener.dto.UrlShortenResponse;
import com.example.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    /**
     * POST /api/v1/urls
     * Creates a new short URL.
     */
    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlShortenResponse> createShortUrl(@Valid @RequestBody UrlShortenRequest request) {
        UrlShortenResponse response = urlService.createShortUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /{shortCode}
     * Redirects the user to the original URL.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        
        // Return 302 Found redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
```