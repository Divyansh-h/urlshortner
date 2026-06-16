package com.example.urlshortener.common.service;

import com.example.urlshortener.common.model.FeatureFlag;
import com.example.urlshortener.common.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    @Cacheable(value = "feature_flags", key = "#name")
    public boolean isEnabled(String name) {
        log.debug("Cache miss for feature flag: {}", name);
        Optional<FeatureFlag> flag = featureFlagRepository.findByName(name);
        return flag.map(FeatureFlag::isEnabled).orElse(false);
    }

    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "feature_flags", key = "#name")
    public FeatureFlag setFlag(String name, boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByName(name)
                .orElse(FeatureFlag.builder().name(name).build());
        
        flag.setEnabled(enabled);
        log.info("Feature flag '{}' set to {}", name, enabled);
        return featureFlagRepository.save(flag);
    }
}
