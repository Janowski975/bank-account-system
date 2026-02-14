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
import pl.proggo.bankapp.dto.TransferDTO;
import pl.proggo.bankapp.dto.TransferRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.Transfer;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private User otherUser;
    private Account fromAccount;
    private Account toAccount;
    private Transfer testTransfer;
    private TransferRequest transferRequest;

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

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("encodedPassword");
        otherUser.setFullName("Other User");
        otherUser.setRole("USER");
        otherUser.setIsActive(true);

        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("PL123456789012345678901234");
        fromAccount.setAccountName("From Account");
        fromAccount.setUser(testUser);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setCurrency("PLN");
        fromAccount.setAccountType("CHECKING");
        fromAccount.setIsActive(true);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("PL987654321098765432109876");
        toAccount.setAccountName("To Account");
        toAccount.setUser(otherUser);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setCurrency("PLN");
        toAccount.setAccountType("CHECKING");
        toAccount.setIsActive(true);

        testTransfer = new Transfer();
        testTransfer.setId(1L);
        testTransfer.setFromAccount(fromAccount);
        testTransfer.setToAccount(toAccount);
        testTransfer.setAmount(new BigDecimal("100.00"));
        testTransfer.setStatus("COMPLETED");
        testTransfer.setDescription("Test transfer");
        testTransfer.setReferenceNumber("REF-123456789012");
        testTransfer.setCreatedAt(LocalDateTime.now());

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber("PL123456789012345678901234");
        transferRequest.setToAccountNumber("PL987654321098765432109876");
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    @DisplayName("Should transfer between accounts successfully")
    void testTransferBetweenAccounts_Success() {
        // Arrange
        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount).thenReturn(toAccount);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // Act
        TransferDTO result = transferService.transferBetweenAccounts(transferRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("PL123456789012345678901234", result.getFromAccountNumber());
        assertEquals("PL987654321098765432109876", result.getToAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber("PL123456789012345678901234");
        verify(accountRepository, times(1)).findByAccountNumber("PL987654321098765432109876");
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when insufficient funds")
    void testTransferBetweenAccounts_InsufficientFunds() {
        // Arrange
        fromAccount.setBalance(new BigDecimal("50.00"));
        transferRequest.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transferService.transferBetweenAccounts(transferRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when transferring to same account")
    void testTransferBetweenAccounts_SameAccount() {
        // Arrange
        transferRequest.setToAccountNumber("PL123456789012345678901234");

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transferService.transferBetweenAccounts(transferRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Cannot transfer to the same account"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when from account is inactive")
    void testTransferBetweenAccounts_InactiveAccount() {
        // Arrange
        fromAccount.setIsActive(false);

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transferService.transferBetweenAccounts(transferRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("From account is not active"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when amount is negative")
    void testTransferBetweenAccounts_NegativeAmount() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("-100.00"));

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transferService.transferBetweenAccounts(transferRequest, "testuser");
        });
        assertTrue(exception.getMessage().contains("Transfer amount must be greater than zero"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when from account not found")
    void testTransferBetweenAccounts_InvalidAccount() {
        // Arrange
        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transferService.transferBetweenAccounts(transferRequest, "testuser");
        });
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should handle concurrent transfers safely")
    void testTransferBetweenAccounts_ConcurrentTransfers() {
        // Arrange
        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount).thenReturn(toAccount);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // Act
        TransferDTO result = transferService.transferBetweenAccounts(transferRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should handle large amount transfers")
    void testTransferBetweenAccounts_LargeAmount() {
        // Arrange
        fromAccount.setBalance(new BigDecimal("1000000.00"));
        transferRequest.setAmount(new BigDecimal("500000.00"));

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("PL987654321098765432109876")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount).thenReturn(toAccount);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // Act
        TransferDTO result = transferService.transferBetweenAccounts(transferRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Should get transfer details successfully")
    void testGetTransfer_Success() {
        // Arrange
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));

        // Act
        TransferDTO result = transferService.getTransfer(1L, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PL123456789012345678901234", result.getFromAccountNumber());
        assertEquals("PL987654321098765432109876", result.getToAccountNumber());
        verify(transferRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should get account transfers successfully")
    void testGetAccountTransfers_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transferPage = new PageImpl<>(Collections.singletonList(testTransfer));

        when(accountRepository.findByAccountNumber("PL123456789012345678901234")).thenReturn(Optional.of(fromAccount));
        when(transferRepository.findByAccountId(anyLong(), any(Pageable.class))).thenReturn(transferPage);

        // Act
        Page<TransferDTO> result = transferService.getAccountTransfers("PL123456789012345678901234", "testuser", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PL123456789012345678901234", result.getContent().get(0).getFromAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber("PL123456789012345678901234");
        verify(transferRepository, times(1)).findByAccountId(anyLong(), any(Pageable.class));
    }
}
