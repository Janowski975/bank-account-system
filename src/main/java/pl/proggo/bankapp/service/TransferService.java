package pl.proggo.bankapp.service;

import pl.proggo.bankapp.dto.TransferDTO;
import pl.proggo.bankapp.dto.TransferRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.Transfer;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.TransferRepository;
import pl.proggo.bankapp.util.ReferenceNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for handling money transfers between accounts.
 * Provides transfer operations with comprehensive validation and transaction management.
 */
@Slf4j
@Service
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    public TransferService(TransferRepository transferRepository, AccountRepository accountRepository) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Transfer money between two accounts.
     * Validates transfer eligibility and updates account balances atomically.
     *
     * @param request Transfer request containing account details and amount
     * @param username Username of the user initiating the transfer
     * @return TransferDTO containing transfer details
     * @throws BusinessException if validation fails
     * @throws ResourceNotFoundException if accounts not found
     */
    public TransferDTO transferBetweenAccounts(TransferRequest request, String username) {
        log.info("Processing transfer from {} to {} for amount {} by user {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount(), username);

        // Find accounts by account number
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("From account not found: " + request.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("To account not found: " + request.getToAccountNumber()));

        // Validate transfer eligibility
        validateTransferEligibility(fromAccount, toAccount, request.getAmount(), username);

        // Perform transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Log transfer transaction
        Transfer transfer = logTransferTransaction(fromAccount, toAccount, request.getAmount(), request.getDescription());

        log.info("Transfer completed: {} from {} to {}", transfer.getId(),
                request.getFromAccountNumber(), request.getToAccountNumber());

        return mapToDTO(transfer);
    }

    /**
     * Get transfer details by ID.
     *
     * @param transferId Transfer ID
     * @param username Username requesting the transfer details
     * @return TransferDTO containing transfer details
     * @throws ResourceNotFoundException if transfer not found
     * @throws BusinessException if user not authorized
     */
    @Transactional(readOnly = true)
    public TransferDTO getTransfer(Long transferId, String username) {
        log.info("Fetching transfer: {} for user: {}", transferId, username);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        // Check if user is authorized to view this transfer
        if (!transfer.getFromAccount().getUser().getUsername().equals(username) &&
            !transfer.getToAccount().getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to view this transfer");
        }

        return mapToDTO(transfer);
    }

    /**
     * Get all transfers for an account (sent and received).
     *
     * @param accountNumber Account number
     * @param username Username requesting the transfers
     * @param pageable Pagination information
     * @return Page of TransferDTO
     * @throws ResourceNotFoundException if account not found
     * @throws BusinessException if user not authorized
     */
    @Transactional(readOnly = true)
    public Page<TransferDTO> getAccountTransfers(String accountNumber, String username, Pageable pageable) {
        log.info("Fetching transfers for account: {} by user: {}", accountNumber, username);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to view transfers for this account");
        }

        return transferRepository.findByAccountId(account.getId(), pageable)
                .map(this::mapToDTO);
    }

    /**
     * Validate if transfer is eligible.
     * Checks for sufficient funds, active accounts, different accounts, and authorization.
     *
     * @param fromAccount Source account
     * @param toAccount Destination account
     * @param amount Transfer amount
     * @param username Username initiating the transfer
     * @throws BusinessException if validation fails
     */
    private void validateTransferEligibility(Account fromAccount, Account toAccount, BigDecimal amount, String username) {
        // Check authorization
        if (!fromAccount.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to transfer from this account");
        }

        // Check if transferring to same account
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BusinessException("Cannot transfer to the same account");
        }

        // Check if accounts are active
        if (!fromAccount.getIsActive()) {
            throw new BusinessException("From account is not active");
        }

        if (!toAccount.getIsActive()) {
            throw new BusinessException("To account is not active");
        }

        // Check for negative amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be greater than zero");
        }

        // Check sufficient funds
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds in the account");
        }
    }

    /**
     * Log transfer transaction to database.
     *
     * @param fromAccount Source account
     * @param toAccount Destination account
     * @param amount Transfer amount
     * @param description Transfer description
     * @return Saved Transfer entity
     */
    private Transfer logTransferTransaction(Account fromAccount, Account toAccount, BigDecimal amount, String description) {
        Transfer transfer = new Transfer();
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(amount);
        transfer.setStatus("COMPLETED");
        transfer.setDescription(description);
        transfer.setReferenceNumber(ReferenceNumberGenerator.generate());

        return transferRepository.save(transfer);
    }

    /**
     * Map Transfer entity to DTO.
     *
     * @param transfer Transfer entity
     * @return TransferDTO
     */
    private TransferDTO mapToDTO(Transfer transfer) {
        return new TransferDTO(
                transfer.getId(),
                transfer.getFromAccount().getAccountNumber(),
                transfer.getToAccount().getAccountNumber(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getReferenceNumber(),
                transfer.getCreatedAt()
        );
    }
}
