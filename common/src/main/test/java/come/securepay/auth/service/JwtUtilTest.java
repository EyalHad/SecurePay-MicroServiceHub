package java.come.securepay.auth.service;

import com.securepay.common.jwt.JwtUtil;
import com.securepay.common.jwt.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtConfig jwtConfig;
    @Test
    public void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("user");
        assertNotNull(token);
        assertEquals("user", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, "user"));
    }



    @Test
    public void testExtractUsername() {
        // Generate a test token (use auth-service or mock)
        String token = generateTestToken();
        assertEquals("user", jwtUtil.extractUsername(token));
    }

    @Test
    public void testValidateToken() {
        String token = generateTestToken();
        assertTrue(jwtUtil.validateToken(token, "user"));
    }

    private String generateTestToken() {
        // Mock token generation for testing
        return Jwts.builder()
                .claim("sub", "user")
                .claim("iat", new Date())
                .claim("exp", new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime()))
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .compact();
    }
}