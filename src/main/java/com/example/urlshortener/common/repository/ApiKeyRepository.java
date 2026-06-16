package com.example.urlshortener.common.repository;

import com.example.urlshortener.common.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyAndActiveTrue(String key);
}
