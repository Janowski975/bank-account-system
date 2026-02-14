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
import pl.proggo.bankapp.dto.TransactionRequest;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.repository.UserRepository;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("TransactionController Integration Tests")
class TransactionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Long testAccountId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Arrange - Create test user if not exists
        if (!userRepository.existsByUsername("transactionuser")) {
            testUser = new User();
            testUser.setUsername("transactionuser");
            testUser.setEmail("transaction@example.com");
            testUser.setPassword("encodedPassword");
            testUser.setFullName("Transaction User");
            testUser.setRole("USER");
            testUser.setIsActive(true);
            testUser = userRepository.save(testUser);
        } else {
            testUser = userRepository.findByUsername("transactionuser").orElseThrow();
        }
    }

    @Test
    @DisplayName("Should create transaction successfully")
    @WithMockUser(username = "transactionuser", roles = {"USER"})
    void testCreateTransaction_Endpoint() throws Exception {
        // Arrange - Create an account first
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setAccountName("Transaction Test Account");
        accountRequest.setCurrency("PLN");
        accountRequest.setAccountType("CHECKING");

        String createAccountResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createAccountResponse).get("id").asLong();

        // Prepare transaction request
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType("DEPOSIT");
        transactionRequest.setAmount(new BigDecimal("100.00"));
        transactionRequest.setDescription("Test deposit");

        // Act & Assert
        mockMvc.perform(post("/accounts/" + accountId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.description").value("Test deposit"))
                .andExpect(jsonPath("$.referenceNumber").exists());
    }

    @Test
    @DisplayName("Should get transactions by account successfully")
    @WithMockUser(username = "transactionuser", roles = {"USER"})
    void testGetTransactionsByAccount_Endpoint() throws Exception {
        // Arrange - Create an account and a transaction
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setAccountName("Transaction List Account");
        accountRequest.setCurrency("PLN");
        accountRequest.setAccountType("CHECKING");

        String createAccountResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createAccountResponse).get("id").asLong();

        // Create a transaction
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType("DEPOSIT");
        transactionRequest.setAmount(new BigDecimal("200.00"));
        transactionRequest.setDescription("Test deposit for list");

        mockMvc.perform(post("/accounts/" + accountId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(get("/accounts/" + accountId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(200.00));
    }

    @Test
    @DisplayName("Should return 400 for insufficient funds")
    @WithMockUser(username = "transactionuser", roles = {"USER"})
    void testCreateTransaction_InsufficientFunds() throws Exception {
        // Arrange - Create an account with no balance
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setAccountName("Insufficient Funds Account");
        accountRequest.setCurrency("PLN");
        accountRequest.setAccountType("CHECKING");

        String createAccountResponse = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long accountId = objectMapper.readTree(createAccountResponse).get("id").asLong();

        // Try to withdraw more than available
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType("WITHDRAWAL");
        transactionRequest.setAmount(new BigDecimal("100.00"));
        transactionRequest.setDescription("Test withdrawal");

        // Act & Assert
        mockMvc.perform(post("/accounts/" + accountId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isBadRequest());
    }
}
