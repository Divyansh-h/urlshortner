package com.example.urlshortener.analytics.job;

import com.example.urlshortener.analytics.repository.BatchUrlAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickAnalyticsBatchJob {

    private final BatchUrlAnalyticsRepository batchRepository;
    
    // Using a concurrent queue is thread-safe and prevents data loss from race conditions
    private final ConcurrentLinkedQueue<String> clickQueue = new ConcurrentLinkedQueue<>();

    /**
     * Records a click in the in-memory buffer. This method returns immediately.
     */
    public void recordClick(String shortCode) {
        clickQueue.add(shortCode);
    }

    /**
     * Runs periodically to flush all buffered clicks to the database in a single batch.
     */
    @Scheduled(fixedDelayString = "${analytics.batch.delay:5000}")
    public void flushClicksToDatabase() {
        if (clickQueue.isEmpty()) {
            return;
        }

        // Drain the queue into a local aggregation map
        Map<String, Long> clicksToFlush = new HashMap<>();
        String shortCode;
        while ((shortCode = clickQueue.poll()) != null) {
            clicksToFlush.merge(shortCode, 1L, Long::sum);
        }

        if (clicksToFlush.isEmpty()) {
            return;
        }

        try {
            log.debug("Flushing {} unique URL clicks to database...", clicksToFlush.size());
            batchRepository.batchUpdateClickCounts(clicksToFlush);
            log.debug("Successfully flushed clicks to database.");
        } catch (Exception e) {
            log.error("Failed to flush clicks to database, restoring buffer to prevent data loss", e);
            // On failure, re-queue the clicks to try again next time
            clicksToFlush.forEach((code, count) -> {
                for (long i = 0; i < count; i++) {
                    clickQueue.add(code);
                }
            });
        }
    }
}
