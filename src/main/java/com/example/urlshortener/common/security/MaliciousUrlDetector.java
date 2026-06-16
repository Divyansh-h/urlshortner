package com.example.urlshortener.common.security;

public interface MaliciousUrlDetector {
    /**
     * Checks if a given URL is considered malicious.
     * @param url The URL to check.
     * @return true if malicious, false if safe.
     */
    boolean isMalicious(String url);
}
