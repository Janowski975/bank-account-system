package pl.proggo.bankapp.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.proggo.bankapp.security.JwtUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private String testSecret;
    private long expirationMs;
    private long refreshExpirationMs;
    private Key key;

    @BeforeEach
    void setUp() {
        // Arrange - Set up test JWT configuration
        testSecret = "test-secret-key-for-testing-purposes-must-be-very-long-at-least-256-bits-long";
        expirationMs = 3600000L; // 1 hour
        refreshExpirationMs = 604800000L; // 7 days
        key = Keys.hmacShaKeyFor(testSecret.getBytes());

        jwtUtils = new JwtUtils(testSecret, expirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Should generate access token successfully")
    void testGenerateToken_Success() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtUtils.generateAccessToken(username);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateToken_Success() {
        // Arrange
        String username = "testuser";
        String token = jwtUtils.generateAccessToken(username);

        // Act
        boolean isValid = jwtUtils.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for expired token")
    void testValidateToken_Expired() {
        // Arrange - Create an expired token
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000); // 1 second in the past

        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(expiredDate)
                .signWith(key)
                .compact();

        // Act
        boolean isValid = jwtUtils.validateToken(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract username from token successfully")
    void testExtractUsername_Success() {
        // Arrange
        String username = "testuser";
        String token = jwtUtils.generateAccessToken(username);

        // Act
        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        // Assert
        assertNotNull(extractedUsername);
        assertEquals(username, extractedUsername);
    }

    @Test
    @DisplayName("Should return false for malformed token")
    void testValidateToken_Malformed() {
        // Arrange
        String malformedToken = "invalid.token.format";

        // Act
        boolean isValid = jwtUtils.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate refresh token successfully")
    void testGenerateRefreshToken_Success() {
        // Arrange
        String username = "testuser";

        // Act
        String refreshToken = jwtUtils.generateRefreshToken(username);

        // Assert
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Should generate different tokens for access and refresh")
    void testGenerateTokens_DifferentTypes() {
        // Arrange
        String username = "testuser";

        // Act
        String accessToken = jwtUtils.generateAccessToken(username);
        String refreshToken = jwtUtils.generateRefreshToken(username);

        // Assert
        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertNotEquals(accessToken, refreshToken);
    }
}
