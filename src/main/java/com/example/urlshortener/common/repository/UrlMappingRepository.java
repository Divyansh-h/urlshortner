package com.example.urlshortener.repository;

import com.example.urlshortener.common.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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
     * Increment click count safely handling concurrency.
     * Executes an atomic UPDATE directly on the database to avoid race conditions.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    /**
     * Finds all URLs where the expiry time has passed.
     * Used by the cleanup job to fetch records before deleting them so their caches can be invalidated.
     * 
     * @param now The current time to compare against.
     * @return List of expired URL mappings.
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.expiryTime IS NOT NULL AND u.expiryTime < :now")
    List<UrlMapping> findExpiredUrls(@Param("now") OffsetDateTime now);

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
    
    @Query("SELECT SUM(u.clickCount) FROM UrlMapping u")
    Long getTotalClicks();
}