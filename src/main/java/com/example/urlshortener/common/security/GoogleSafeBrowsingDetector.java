package com.example.urlshortener.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GoogleSafeBrowsingDetector implements MaliciousUrlDetector {

    @Value("${app.security.safe-browsing.api-key:}")
    private String apiKey;

    private static final String API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isMalicious(String url) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.debug("Google Safe Browsing API key not configured. Bypassing check for URL: {}", url);
            return false;
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL + apiKey, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // If "matches" array is present in the response, the URL is considered a threat
                return response.getBody().containsKey("matches");
            }
        } catch (RestClientException e) {
            log.error("Error communicating with Google Safe Browsing API: {}", e.getMessage());
            // Fail open (allow) if API is down to prevent complete system outage, 
            // or fail closed depending on strictness requirements. Defaulting to fail open.
            return false;
        }

        return false;
    }

    private Map<String, Object> buildRequestBody(String url) {
        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("clientId", "urlshortener");
        clientInfo.put("clientVersion", "1.0.0");

        Map<String, Object> threatEntry = new HashMap<>();
        threatEntry.put("url", url);

        Map<String, Object> threatInfo = new HashMap<>();
        threatInfo.put("threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"));
        threatInfo.put("platformTypes", List.of("ANY_PLATFORM"));
        threatInfo.put("threatEntryTypes", List.of("URL"));
        threatInfo.put("threatEntries", Collections.singletonList(threatEntry));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("client", clientInfo);
        requestBody.put("threatInfo", threatInfo);

        return requestBody;
    }
}
