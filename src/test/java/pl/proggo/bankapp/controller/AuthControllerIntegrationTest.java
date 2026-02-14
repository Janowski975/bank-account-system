package pl.proggo.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pl.proggo.bankapp.dto.LoginRequest;
import pl.proggo.bankapp.dto.RegisterRequest;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Endpoint() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("Password123");
        request.setFullName("New User");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Endpoint() throws Exception {
        // Arrange - Create a test user first
        User testUser = new User();
        testUser.setUsername("loginuser");
        testUser.setEmail("loginuser@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123"));
        testUser.setFullName("Login User");
        testUser.setRole("USER");
        testUser.setIsActive(true);
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("loginuser");
        request.setPassword("Password123");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.email").value("loginuser@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when login with invalid credentials")
    void testLoginWithInvalidCredentials_Endpoint() throws Exception {
        // Arrange - Create a test user first
        User testUser = new User();
        testUser.setUsername("invaliduser");
        testUser.setEmail("invaliduser@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123"));
        testUser.setFullName("Invalid User");
        testUser.setRole("USER");
        testUser.setIsActive(true);
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("invaliduser");
        request.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when registering with duplicate username")
    void testRegister_DuplicateUsername() throws Exception {
        // Arrange - Create a user first
        User existingUser = new User();
        existingUser.setUsername("duplicateuser");
        existingUser.setEmail("duplicate@example.com");
        existingUser.setPassword(passwordEncoder.encode("Password123"));
        existingUser.setFullName("Existing User");
        existingUser.setRole("USER");
        existingUser.setIsActive(true);
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicateuser");
        request.setEmail("newemail@example.com");
        request.setPassword("Password123");
        request.setFullName("Duplicate User");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
