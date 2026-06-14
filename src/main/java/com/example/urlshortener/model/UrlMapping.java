package com.example.urlshortener.model;

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
        // Enforce fast lookups and uniqueness at the JPA level
        @Index(name = "idx_urls_short_code", columnList = "short_code", unique = true)
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
}