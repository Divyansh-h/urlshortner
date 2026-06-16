package com.example.urlshortener.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final String RATE_LIMIT_PREFIX = "rate_limit::";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String identifier = getClientIp(request);
        
        // Identity-Aware Rate Limiting: Use the authenticated user ID if present
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            identifier = "user:" + auth.getPrincipal().toString();
        }

        String key = RATE_LIMIT_PREFIX + identifier;

        // Increment the count for this IP
        Long requests = redisTemplate.opsForValue().increment(key);
        
        // If this is the first request in the window, set the expiration to 1 minute
        if (requests != null && requests == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        // If the count exceeds the limit, block the request
        if (requests != null && requests > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for identifier: {} (Requests: {})", identifier, requests);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                String.format("{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Maximum %d requests per minute allowed.\"}", MAX_REQUESTS_PER_MINUTE)
            );
            return false; // Stop processing
        }

        return true; // Allow processing
    }

    /**
     * Extracts the real client IP, considering potential proxies or Load Balancers (like NGINX).
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}