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
import pl.proggo.bankapp.dto.CreateAccountRequest;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.repository.UserRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("AccountController Integration Tests")
class AccountControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Arrange - Create test user if not exists
        if (!userRepository.existsByUsername("testuser")) {
            testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("test@example.com");
            testUser.setPassword("encodedPassword");
            testUser.setFullName("Test User");
            testUser.setRole("USER");
            testUser.setIsActive(true);
            testUser = userRepository.save(testUser);
        } else {
            testUser = userRepository.findByUsername("testuser").orElseThrow();
        }
    }

    @Test
    @DisplayName("Should create account successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testCreateAccount_Endpoint() throws Exception {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountName("Test Account");
        request.setCurrency("PLN");
        request.setAccountType("CHECKING");

        // Act & Assert
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountName").value("Test Account"))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("Should get account by ID successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetAccountByNumber_Endpoint() throws Exception {
        // Arrange - Create an account first
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountName("Test Account");
        request.setCurrency("PLN");
        request.setAccountType("CHECKING");

        String createResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

        // Act & Assert
        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.accountName").value("Test Account"))
                .andExpect(jsonPath("$.currency").value("PLN"));
    }

    @Test
    @DisplayName("Should get balance successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetBalance_Endpoint() throws Exception {
        // Arrange - Create an account first
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountName("Balance Test Account");
        request.setCurrency("PLN");
        request.setAccountType("CHECKING");

        String createResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

        // Act & Assert
        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    @DisplayName("Should update account successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdateAccount_Endpoint() throws Exception {
        // Arrange - Create an account first
        CreateAccountRequest createRequest = new CreateAccountRequest();
        createRequest.setAccountName("Original Account");
        createRequest.setCurrency("PLN");
        createRequest.setAccountType("CHECKING");

        String createResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

        // Prepare update request
        CreateAccountRequest updateRequest = new CreateAccountRequest();
        updateRequest.setAccountName("Updated Account");
        updateRequest.setCurrency("PLN");
        updateRequest.setAccountType("SAVINGS");

        // Act & Assert
        mockMvc.perform(put("/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Updated Account"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    @DisplayName("Should return 404 when account not found")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetAccount_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/accounts/99999"))
                .andExpect(status().isNotFound());
    }
}
