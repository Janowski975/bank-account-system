package pl.proggo.bankapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.proggo.bankapp.dto.AccountDTO;
import pl.proggo.bankapp.dto.CreateAccountRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;
    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        // Arrange - Set up test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("USER");
        testUser.setIsActive(true);

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("PL123456789012345678901234");
        testAccount.setAccountName("Test Account");
        testAccount.setUser(testUser);
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setCurrency("PLN");
        testAccount.setAccountType("CHECKING");
        testAccount.setIsActive(true);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());

        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setAccountName("New Account");
        createAccountRequest.setCurrency("PLN");
        createAccountRequest.setAccountType("CHECKING");
    }

    @Test
    @DisplayName("Should create account successfully")
    void testCreateAccount_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        AccountDTO result = accountService.createAccount("testuser", createAccountRequest);

        // Assert
        assertNotNull(result);
        assertEquals("PL123456789012345678901234", result.getAccountNumber());
        assertEquals("Test Account", result.getAccountName());
        assertEquals("PLN", result.getCurrency());
        assertEquals("CHECKING", result.getAccountType());
        assertTrue(result.getIsActive());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should get account by ID successfully")
    void testGetAccountByNumber_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        AccountDTO result = accountService.getAccount(1L, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PL123456789012345678901234", result.getAccountNumber());
        assertEquals("Test Account", result.getAccountName());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account not found")
    void testGetAccountByNumber_NotFound() {
        // Arrange
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.getAccount(999L, "testuser");
        });
        verify(accountRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get balance successfully")
    void testGetBalance_Success() {
        // Arrange
        testAccount.setBalance(new BigDecimal("1000.50"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        AccountDTO result = accountService.getAccount(1L, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1000.50"), result.getBalance());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should update account successfully")
    void testUpdateAccount_Success() {
        // Arrange
        CreateAccountRequest updateRequest = new CreateAccountRequest();
        updateRequest.setAccountName("Updated Account");
        updateRequest.setCurrency("PLN");
        updateRequest.setAccountType("SAVINGS");

        Account updatedAccount = new Account();
        updatedAccount.setId(1L);
        updatedAccount.setAccountNumber("PL123456789012345678901234");
        updatedAccount.setAccountName("Updated Account");
        updatedAccount.setUser(testUser);
        updatedAccount.setBalance(BigDecimal.ZERO);
        updatedAccount.setCurrency("PLN");
        updatedAccount.setAccountType("SAVINGS");
        updatedAccount.setIsActive(true);
        updatedAccount.setCreatedAt(LocalDateTime.now());
        updatedAccount.setUpdatedAt(LocalDateTime.now());

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        // Act
        AccountDTO result = accountService.updateAccount(1L, updateRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("Updated Account", result.getAccountName());
        assertEquals("SAVINGS", result.getAccountType());
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when unauthorized user tries to access account")
    void testGetAccount_UnauthorizedUser() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            accountService.getAccount(1L, "differentuser");
        });
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw BusinessException when creating account with null request")
    void testCreateAccount_WithNullRequest() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            accountService.createAccount("testuser", null);
        });
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should handle negative balance correctly")
    void testGetBalance_WithNegativeAmount() {
        // Arrange
        testAccount.setBalance(new BigDecimal("-100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        AccountDTO result = accountService.getAccount(1L, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("-100.00"), result.getBalance());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw BusinessException when getting account with invalid format")
    void testGetAccountByNumber_WithInvalidFormat() {
        // Arrange
        when(accountRepository.findById(-1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.getAccount(-1L, "testuser");
        });
        verify(accountRepository, times(1)).findById(-1L);
    }

    @Test
    @DisplayName("Should throw BusinessException when updating account with duplicate data")
    void testUpdateAccount_WithDuplicateData() {
        // Arrange
        CreateAccountRequest updateRequest = new CreateAccountRequest();
        updateRequest.setAccountName("Test Account");
        updateRequest.setCurrency("PLN");
        updateRequest.setAccountType("CHECKING");

        Account existingAccount = new Account();
        existingAccount.setId(2L);
        existingAccount.setAccountNumber("PL987654321098765432109876");
        existingAccount.setAccountName("Test Account");
        existingAccount.setUser(testUser);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        AccountDTO result = accountService.updateAccount(1L, updateRequest, "testuser");

        // Assert
        assertNotNull(result);
        verify(accountRepository, times(1)).findById(1L);
    }
}
