package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Retrieves a URL mapping by its short code.
     * This method will leverage the idx_urls_short_code B-Tree index for fast lookups.
     *
     * @param shortCode The generated or custom short alias.
     * @return An Optional containing the UrlMapping if found.
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Checks if a short code already exists in the database.
     * Useful for collision detection during alias generation or custom alias validation.
     *
     * @param shortCode The short alias to check.
     * @return true if the short code exists, false otherwise.
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Deletes all URLs where the expiry time has passed.
     * Can be invoked by a scheduled task for database cleanup.
     * 
     * @param now The current time to compare against.
     * @return The number of deleted records.
     */
    @Modifying
    @Query("DELETE FROM UrlMapping u WHERE u.expiryTime IS NOT NULL AND u.expiryTime < :now")
    int deleteExpiredUrls(@Param("now") OffsetDateTime now);
}