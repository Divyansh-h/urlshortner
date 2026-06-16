package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.analytics.dto.LoginRequest;
import com.example.urlshortener.analytics.dto.LoginResponse;
import com.example.urlshortener.common.model.AdminUser;
import com.example.urlshortener.common.repository.AdminUserRepository;
import com.example.urlshortener.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsername(request.getUsername());
        
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            if (passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
                String token = jwtUtil.generateToken(admin.getUsername());
                return ResponseEntity.ok(new LoginResponse(token));
            }
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }
}
