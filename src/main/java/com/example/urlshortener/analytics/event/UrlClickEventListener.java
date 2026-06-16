package com.example.urlshortener.analytics.event;

import com.example.urlshortener.analytics.event.UrlClickedEvent;
import com.example.urlshortener.analytics.job.ClickAnalyticsBatchJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickEventListener {

    private final ClickAnalyticsBatchJob batchJob;

    /**
     * Listens for UrlClickedEvent and buffers it in memory.
     * This avoids immediate database updates for each click, significantly reducing
     * database round-trips by allowing the ClickAnalyticsBatchJob to flush them in batches.
     */
    @EventListener
    public void handleUrlClickedEvent(UrlClickedEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                batchJob.recordClick(event.getShortCode());
                log.debug("Buffered click for short code: {}", event.getShortCode());
            } catch (Exception e) {
                log.error("Failed to async buffer click for short code: {}", event.getShortCode(), e);
            }
        });
    }
}