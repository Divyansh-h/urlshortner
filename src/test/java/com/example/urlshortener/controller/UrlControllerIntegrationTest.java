package com.example.urlshortener.controller;

import com.example.urlshortener.dto.UrlShortenRequest;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    private final String ORIGINAL_URL = "https://www.example.com/very/long/url/to/test";
    private final String SHORT_CODE = "abc1234";
    private final String DOMAIN = "http://localhost:8080/";

    @Test
    void createShortUrl_ValidRequest_Returns201Created() throws Exception {
        // Arrange
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        UrlMapping mockMapping = UrlMapping.builder()
                .shortCode(SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .createdAt(OffsetDateTime.now())
                .build();

        when(urlService.createShortUrl(ORIGINAL_URL, null)).thenReturn(mockMapping);

        // Act & Assert
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(DOMAIN + SHORT_CODE))
                .andExpect(jsonPath("$.originalUrl").value(ORIGINAL_URL))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createShortUrl_InvalidUrlFormat_Returns400BadRequest() throws Exception {
        // Arrange
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("not-a-valid-url"); // Fails @URL validation

        // Act & Assert
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Original URL must be a valid URL format"));
    }

    @Test
    void createShortUrl_CustomAliasCollision_Returns409Conflict() throws Exception {
        // Arrange
        String customAlias = "taken";
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl(ORIGINAL_URL);
        request.setCustomAlias(customAlias);

        when(urlService.createShortUrl(ORIGINAL_URL, customAlias))
                .thenThrow(new IllegalArgumentException("Alias already in use"));

        // Act & Assert
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Alias already in use"));
    }

    @Test
    void redirectToOriginalUrl_Exists_Returns302Found() throws Exception {
        // Arrange
        when(urlService.getOriginalUrl(SHORT_CODE)).thenReturn(ORIGINAL_URL);

        // Act & Assert
        mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isFound()) // HTTP 302
                .andExpect(header().string("Location", ORIGINAL_URL)); // Verifies the redirect header
    }

    @Test
    void redirectToOriginalUrl_DoesNotExist_Returns404NotFound() throws Exception {
        // Arrange
        when(urlService.getOriginalUrl(SHORT_CODE))
                .thenThrow(new ResourceNotFoundException("URL not found for short code: " + SHORT_CODE));

        // Act & Assert
        mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("URL not found for short code: " + SHORT_CODE));
    }
}