package com.example.urlshortener.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "urls",
    indexes = {
        // 1. Primary Lookup Index
        @Index(name = "idx_urls_short_code", columnList = "short_code", unique = true),
        
        // 2. Composite Index for User Dashboards
        // Optimizes: WHERE user_id = ? ORDER BY created_at DESC
        @Index(name = "idx_urls_user_created_desc", columnList = "user_id, created_at DESC")
        
        // Note: Partial indexes (WHERE expiry_time IS NOT NULL) cannot be defined natively 
        // in standard JPA annotations. They must be created via Flyway, Liquibase, 
        // or a custom schema.sql script, which we have done.
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Short code must not be blank")
    @Size(max = 15, message = "Short code must not exceed 15 characters")
    @Column(name = "short_code", nullable = false, unique = true, length = 15)
    private String shortCode;

    @NotBlank(message = "Original URL must not be blank")
    @URL(message = "Original URL must be a valid URL format")
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expiry_time")
    private OffsetDateTime expiryTime;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "user_id", length = 50)
    private String userId;
}