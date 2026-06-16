package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.common.model.FeatureFlag;
import com.example.urlshortener.common.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flags")
@RequiredArgsConstructor
public class AdminFeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getAllFlags() {
        return ResponseEntity.ok(featureFlagService.getAllFlags());
    }

    @PutMapping("/{name}")
    public ResponseEntity<FeatureFlag> toggleFlag(@PathVariable String name, @RequestBody Map<String, Boolean> body) {
        if (!body.containsKey("enabled")) {
            return ResponseEntity.badRequest().build();
        }
        boolean enabled = body.get("enabled");
        FeatureFlag updated = featureFlagService.setFlag(name, enabled);
        return ResponseEntity.ok(updated);
    }
}
