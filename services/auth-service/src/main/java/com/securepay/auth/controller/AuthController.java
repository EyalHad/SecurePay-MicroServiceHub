package com.securepay.auth.controller;

import com.securepay.auth.model.LoginRequest;
import com.securepay.auth.model.RefreshRequest;
import com.securepay.auth.service.AuthService;
import com.securepay.common.jwt.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        Map<String, String> tokens = authService.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(tokens);
    }
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(@RequestBody RefreshRequest request) {
        String username = jwtUtil.extractUsername(request.getRefreshToken());
        if (jwtUtil.validateToken(request.getRefreshToken(), username)) {
            String newAccessToken = jwtUtil.generateToken(username);
            return ResponseEntity.ok(newAccessToken);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
