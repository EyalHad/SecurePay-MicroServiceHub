package com.securepay.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service for generating and validating JWT tokens for user authentication.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationTime;

    /**
     * Constructs a JwtService with the specified secret key and expiration time.
     *
     * @param secret the secret key for signing JWT tokens (must be at least 32 bytes)
     * @param expirationTime the expiration time for tokens in milliseconds
     * @throws IllegalArgumentException if the secret key is too short
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-time:86400000}") long expirationTime) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
        logger.info("JwtService initialized with expiration time: {} ms", expirationTime);
    }

    /**
     * Generates a JWT token for the given user ID.
     *
     * @param userId the ID of the user to generate a token for
     * @return the generated JWT token
     * @throws IllegalArgumentException if userId is null
     */
    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();

        logger.debug("Generated JWT token for user ID: {}", userId);
        return token;
    }

    /**
     * Validates the given JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     * @throws IllegalArgumentException if the token is null or empty
     */
    public boolean isValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Attempted to validate null or empty token");
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            logger.debug("Token validated successfully: {}", token);
            return true;
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}. Reason: {}", token, e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the user ID from the given JWT token.
     *
     * @param token the JWT token to extract the user ID from
     * @return the user ID as a Long
     * @throws JwtException if the token is invalid or expired
     * @throws IllegalArgumentException if the token is null/empty or the subject is invalid
     */
    public Long getUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Attempted to extract user ID from null or empty token");
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            Long userId = Long.parseLong(subject);
            logger.debug("Extracted user ID: {} from token", userId);
            return userId;
        } catch (JwtException e) {
            logger.error("Failed to parse token: {}. Reason: {}", token, e.getMessage());
            throw e;
        } catch (NumberFormatException e) {
            logger.error("Invalid subject in token: {}. Expected a numeric user ID", token);
            throw new IllegalArgumentException("Token subject must be a valid user ID", e);
        }
    }

    /**
     * Gets the remaining validity time of the token in milliseconds.
     *
     * @param token the JWT token to check
     * @return the remaining time in milliseconds until expiration, or -1 if expired/invalid
     */
    public long getRemainingValidity(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            long remaining = expiration.getTime() - now;
            return remaining > 0 ? remaining : -1;
        } catch (JwtException e) {
            logger.warn("Cannot determine remaining validity for token: {}. Reason: {}", token, e.getMessage());
            return -1;
        }
    }

    /**
     * Refreshes the token by generating a new one with the same claims but updated expiration.
     *
     * @param token the JWT token to refresh
     * @return the new JWT token
     * @throws JwtException if the token is invalid
     */
    public String refreshToken(String token) {
        if (!isValid(token)) {
            throw new JwtException("Cannot refresh an invalid token");
        }

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        String newToken = Jwts.builder()
                .claims()
                .add(claims) // adds all your claims
                .issuedAt(now)
                .expiration(expiryDate)
                .and() // back to builder
                .signWith(secretKey)
                .compact();

        logger.debug("Refreshed JWT token for user ID: {}", claims.getSubject());
        return newToken;
    }
}