package pl.proggo.bankapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pl.proggo.bankapp.dto.TransactionDTO;
import pl.proggo.bankapp.dto.TransactionRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.Transaction;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;

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
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCurrency("PLN");
        testAccount.setAccountType("CHECKING");
        testAccount.setIsActive(true);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAccount(testAccount);
        testTransaction.setType("DEPOSIT");
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setStatus("COMPLETED");
        testTransaction.setDescription("Test deposit");
        testTransaction.setReferenceNumber("REF-123456789012");
        testTransaction.setCreatedAt(LocalDateTime.now());

        transactionRequest = new TransactionRequest();
        transactionRequest.setType("DEPOSIT");
        transactionRequest.setAmount(new BigDecimal("100.00"));
        transactionRequest.setDescription("Test deposit");
    }

    @Test
    @DisplayName("Should create transaction successfully")
    void testCreateTransaction_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionDTO result = transactionService.createTransaction(1L, transactionRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("DEPOSIT", result.getType());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("Test deposit", result.getDescription());
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw BusinessException for insufficient funds")
    void testCreateTransaction_InsufficientFunds() {
        // Arrange
        testAccount.setBalance(new BigDecimal("50.00"));
        transactionRequest.setType("WITHDRAWAL");
        transactionRequest.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(1L, transactionRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should get transactions by account successfully")
    void testGetTransactionsByAccount_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(Collections.singletonList(testTransaction));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountId(anyLong(), any(Pageable.class))).thenReturn(transactionPage);

        // Act
        Page<TransactionDTO> result = transactionService.getAccountTransactions(1L, "testuser", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("DEPOSIT", result.getContent().get(0).getType());
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, times(1)).findByAccountId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no transactions found")
    void testGetTransactionsByAccount_Empty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountId(anyLong(), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        Page<TransactionDTO> result = transactionService.getAccountTransactions(1L, "testuser", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, times(1)).findByAccountId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account not found")
    void testCreateTransaction_AccountNotFound() {
        // Arrange
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.createTransaction(999L, transactionRequest, "testuser");
        });
        verify(accountRepository, times(1)).findById(999L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when unauthorized user tries to create transaction")
    void testCreateTransaction_UnauthorizedUser() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(1L, transactionRequest, "differentuser");
        });
        assertTrue(exception.getMessage().contains("not authorized"));
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when creating transaction with negative amount")
    void testCreateTransaction_WithNegativeAmount() {
        // Arrange
        transactionRequest.setAmount(new BigDecimal("-100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(1L, transactionRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Transaction amount must be greater than zero") ||
                   exception.getMessage().contains("Amount must be greater than 0"));
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when creating transaction with invalid type")
    void testCreateTransaction_WithInvalidTransactionType() {
        // Arrange
        transactionRequest.setType("TRANSFER");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(1L, transactionRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Invalid transaction type") ||
                   exception.getMessage().contains("TRANSFER"));
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should handle concurrent access to transactions")
    void testGetTransactionsByAccount_WithConcurrentAccess() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(Collections.singletonList(testTransaction));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountId(anyLong(), any(Pageable.class))).thenReturn(transactionPage);

        // Act
        Page<TransactionDTO> result = transactionService.getAccountTransactions(1L, "testuser", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionRepository, times(1)).findByAccountId(anyLong(), any(Pageable.class));
    }
}
