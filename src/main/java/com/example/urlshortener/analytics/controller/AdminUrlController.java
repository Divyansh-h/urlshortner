package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.common.model.UrlMapping;
import com.example.urlshortener.common.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/urls")
@RequiredArgsConstructor
public class AdminUrlController {

    private final UrlMappingRepository urlMappingRepository;

    @GetMapping
    public ResponseEntity<Page<UrlMapping>> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UrlMapping> urls = urlMappingRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(urls);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        if (!urlMappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        urlMappingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
