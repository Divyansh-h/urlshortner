package com.example.urlshortener.common.security;

import com.example.urlshortener.common.model.ApiKey;
import com.example.urlshortener.common.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyAndActiveTrue(apiKeyHeader);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        apiKey.getUserId(), // Principal: we use the user ID as the principal
                        apiKeyHeader,       // Credentials
                        Collections.emptyList() // Authorities/Roles (empty for now)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
