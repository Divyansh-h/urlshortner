package com.example.urlshortener.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UrlClickedEvent extends ApplicationEvent {
    
    private final String shortCode;

    public UrlClickedEvent(Object source, String shortCode) {
        super(source);
        this.shortCode = shortCode;
    }
}