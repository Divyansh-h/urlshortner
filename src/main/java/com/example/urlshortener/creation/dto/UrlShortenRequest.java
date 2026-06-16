package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class UrlShortenRequest {
    @NotBlank(message = "URL cannot be empty")
    @URL(message = "Invalid URL format")
    private String originalUrl;
    
    private String customAlias;
}