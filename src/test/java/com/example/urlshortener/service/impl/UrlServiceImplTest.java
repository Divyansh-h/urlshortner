package com.example.urlshortener.service.impl;

import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private Base62Encoder base62Encoder;

    @InjectMocks
    private UrlServiceImpl urlService;

    private final String ORIGINAL_URL = "https://www.example.com/very/long/url/to/test";

    @Test
    void createShortUrl_WithRandomAlias_Success() {
        // Arrange
        String generatedCode = "abc1234";
        when(base62Encoder.generateRandomString(7)).thenReturn(generatedCode);
        when(repository.existsByShortCode(generatedCode)).thenReturn(false);

        UrlMapping savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(generatedCode)
                .originalUrl(ORIGINAL_URL)
                .createdAt(OffsetDateTime.now())
                .build();
        when(repository.saveAndFlush(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        UrlMapping response = urlService.createShortUrl(ORIGINAL_URL, null);

        // Assert
        assertNotNull(response);
        assertEquals(generatedCode, response.getShortCode());
        assertEquals(ORIGINAL_URL, response.getOriginalUrl());
        assertNotNull(response.getCreatedAt());

        verify(base62Encoder, times(1)).generateRandomString(7);
        verify(repository, times(1)).saveAndFlush(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_WithCustomAlias_Success() {
        // Arrange
        String customAlias = "mybrand";

        when(repository.existsByShortCode(customAlias)).thenReturn(false);

        UrlMapping savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(customAlias)
                .originalUrl(ORIGINAL_URL)
                .createdAt(OffsetDateTime.now())
                .build();
        when(repository.saveAndFlush(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        UrlMapping response = urlService.createShortUrl(ORIGINAL_URL, customAlias);

        // Assert
        assertNotNull(response);
        assertEquals(customAlias, response.getShortCode());
        verify(base62Encoder, never()).generateRandomString(anyInt()); // Shouldn't generate if custom provided
        verify(repository, times(1)).saveAndFlush(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_WithCustomAliasCollision_ThrowsException() {
        // Arrange
        String customAlias = "takenAlias";

        when(repository.existsByShortCode(customAlias)).thenReturn(true); // Alias is taken

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.createShortUrl(ORIGINAL_URL, customAlias);
        });

        assertEquals("Alias already in use", exception.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void generateShortCode_WithCollision_RetriesUntilUnique() {
        // Arrange
        String collisionCode = "colli12";
        String uniqueCode = "uniq123";

        // First call generates 'collisionCode', second generates 'uniqueCode'
        when(base62Encoder.generateRandomString(7)).thenReturn(collisionCode, uniqueCode);
        
        // First check returns true (collision), second check returns false (unique)
        when(repository.existsByShortCode(collisionCode)).thenReturn(true);
        when(repository.existsByShortCode(uniqueCode)).thenReturn(false);

        UrlMapping savedMapping = UrlMapping.builder().shortCode(uniqueCode).originalUrl(ORIGINAL_URL).build();
        when(repository.saveAndFlush(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        UrlMapping response = urlService.createShortUrl(ORIGINAL_URL, null);

        // Assert
        assertEquals(uniqueCode, response.getShortCode());
        verify(base62Encoder, times(2)).generateRandomString(7); // Verifies the retry logic works
    }

    @Test
    void getOriginalUrl_WhenExists_ReturnsUrl() {
        // Arrange
        String shortCode = "xyz123";
        UrlMapping mapping = UrlMapping.builder().shortCode(shortCode).originalUrl(ORIGINAL_URL).build();
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        // Act
        String result = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals(ORIGINAL_URL, result);
    }

    @Test
    void getOriginalUrl_WhenDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        String shortCode = "notfound";
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            urlService.getOriginalUrl(shortCode);
        });
    }
}