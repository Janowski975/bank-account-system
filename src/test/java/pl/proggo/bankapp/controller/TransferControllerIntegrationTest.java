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
import pl.proggo.bankapp.dto.TransferRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.UserRepository;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("TransferController Integration Tests")
class TransferControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;
    private User otherUser;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Arrange - Create test users if not exist
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

        if (!userRepository.existsByUsername("otheruser")) {
            otherUser = new User();
            otherUser.setUsername("otheruser");
            otherUser.setEmail("other@example.com");
            otherUser.setPassword("encodedPassword");
            otherUser.setFullName("Other User");
            otherUser.setRole("USER");
            otherUser.setIsActive(true);
            otherUser = userRepository.save(otherUser);
        } else {
            otherUser = userRepository.findByUsername("otheruser").orElseThrow();
        }

        // Create test accounts
        fromAccount = new Account();
        fromAccount.setAccountNumber("PL11111111111111111111");
        fromAccount.setAccountName("From Account");
        fromAccount.setUser(testUser);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setCurrency("PLN");
        fromAccount.setAccountType("CHECKING");
        fromAccount.setIsActive(true);
        fromAccount = accountRepository.save(fromAccount);

        toAccount = new Account();
        toAccount.setAccountNumber("PL22222222222222222222");
        toAccount.setAccountName("To Account");
        toAccount.setUser(otherUser);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setCurrency("PLN");
        toAccount.setAccountType("CHECKING");
        toAccount.setIsActive(true);
        toAccount = accountRepository.save(toAccount);
    }

    @Test
    @DisplayName("Should transfer money successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testTransferMoney_Success() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL22222222222222222222");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromAccountNumber").value("PL11111111111111111111"))
                .andExpect(jsonPath("$.toAccountNumber").value("PL22222222222222222222"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.referenceNumber").exists());
    }

    @Test
    @DisplayName("Should return 400 when insufficient funds")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testTransferMoney_InsufficientFunds() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL22222222222222222222");
        request.setAmount(new BigDecimal("2000.00"));
        request.setDescription("Test transfer");

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when transferring to same account")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testTransferMoney_SameAccount() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL11111111111111111111");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get transfer details successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTransferDetails_Success() throws Exception {
        // Arrange - Create a transfer first
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL22222222222222222222");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        String createResponse = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long transferId = objectMapper.readTree(createResponse).get("id").asLong();

        // Act & Assert
        mockMvc.perform(get("/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId))
                .andExpect(jsonPath("$.fromAccountNumber").value("PL11111111111111111111"))
                .andExpect(jsonPath("$.toAccountNumber").value("PL22222222222222222222"));
    }

    @Test
    @DisplayName("Should get all transfers for account successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetAccountTransfers_Success() throws Exception {
        // Arrange - Create a transfer first
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL22222222222222222222");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(get("/transfers/account/PL11111111111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].fromAccountNumber").value("PL11111111111111111111"));
    }

    @Test
    @DisplayName("Should return 403 when transferring without auth")
    void testTransferMoney_WithoutAuth() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("PL11111111111111111111");
        request.setToAccountNumber("PL22222222222222222222");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        // Act & Assert - Spring Security returns 403 for missing auth
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
