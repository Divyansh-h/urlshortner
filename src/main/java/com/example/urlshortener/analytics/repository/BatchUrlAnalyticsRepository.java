package com.example.urlshortener.analytics.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BatchUrlAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes a batch UPDATE against the database to aggregate multiple clicks in a single round trip.
     * 
     * @param clickCounts A map of shortCode to the number of new clicks to add.
     */
    public void batchUpdateClickCounts(Map<String, Long> clickCounts) {
        if (clickCounts == null || clickCounts.isEmpty()) {
            return;
        }

        String sql = "UPDATE urls SET click_count = click_count + ? WHERE short_code = ?";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<String, Long> entry : clickCounts.entrySet()) {
            batchArgs.add(new Object[]{entry.getValue(), entry.getKey()});
        }

        try {
            int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Successfully executed batch update for {} URLs", updateCounts.length);
        } catch (Exception e) {
            log.error("Failed to execute batch update for URL clicks", e);
            throw e;
        }
    }
}
