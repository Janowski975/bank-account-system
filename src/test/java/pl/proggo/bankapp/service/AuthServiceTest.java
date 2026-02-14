package pl.proggo.bankapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.proggo.bankapp.dto.AuthResponse;
import pl.proggo.bankapp.dto.LoginRequest;
import pl.proggo.bankapp.dto.RegisterRequest;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.repository.UserRepository;
import pl.proggo.bankapp.security.JwtUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Arrange - Set up test data
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123");
        registerRequest.setFullName("New User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("USER");
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtils.generateAccessToken("testuser")).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refreshToken");

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.getToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(3600L, result.getExpiresIn());
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtils, times(1)).generateAccessToken(anyString());
        verify(jwtUtils, times(1)).generateRefreshToken(anyString());
    }

    @Test
    @DisplayName("Should throw BusinessException when username already exists")
    void testRegister_DuplicateUser() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });
        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateAccessToken("testuser")).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refreshToken");

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.getToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("Password123", "encodedPassword");
        verify(jwtUtils, times(1)).generateAccessToken("testuser");
        verify(jwtUtils, times(1)).generateRefreshToken("testuser");
    }

    @Test
    @DisplayName("Should throw BusinessException with invalid credentials")
    void testLogin_InvalidCredentials() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        loginRequest.setPassword("wrongpassword");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("Invalid username or password"));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpassword", "encodedPassword");
        verify(jwtUtils, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should generate token successfully")
    void testGenerateToken_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateAccessToken("testuser")).thenReturn("generatedAccessToken");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("generatedRefreshToken");

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals("generatedAccessToken", result.getToken());
        assertEquals("generatedRefreshToken", result.getRefreshToken());
        verify(jwtUtils, times(1)).generateAccessToken("testuser");
        verify(jwtUtils, times(1)).generateRefreshToken("testuser");
    }

    @Test
    @DisplayName("Should throw BusinessException when user not found during login")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("Invalid username or password"));
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw BusinessException when registering with weak password")
    void testRegister_WithWeakPassword() {
        // Arrange
        registerRequest.setPassword("weak");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });
        assertTrue(exception.getMessage().contains("Password must be at least 8 characters") ||
                   exception.getMessage().contains("Password does not meet requirements"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when registering with invalid email format")
    void testRegister_WithInvalidEmailFormat() {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });
        assertTrue(exception.getMessage().contains("Invalid email format") ||
                   exception.getMessage().contains("email"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when logging in with locked account")
    void testLogin_WithLockedAccount() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("Account is locked") ||
                   exception.getMessage().contains("Account is") ||
                   exception.getMessage().contains("inactive"));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(jwtUtils, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should throw BusinessException when generating token for expired user")
    void testGenerateToken_WithExpiredUser() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("Account is locked") ||
                   exception.getMessage().contains("Account is") ||
                   exception.getMessage().contains("inactive"));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(jwtUtils, never()).generateAccessToken(anyString());
    }
}
