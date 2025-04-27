package com.securepay.common.jwt;

import com.securepay.common.jwt.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .claim("sub", username)
                .claim("iat", new Date())
                .claim("exp", new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime()))
                .signWith(secretKey)
                .compact();
    }
    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .claim("sub", username)
                .claim("iat", new Date())
                .claim("roles", roles)
                .claim("exp", new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime()))
                .signWith(secretKey)
                .compact();
    }
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .claim("sub", username)
                .claim("iat", new Date())
                .claim("exp", new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
