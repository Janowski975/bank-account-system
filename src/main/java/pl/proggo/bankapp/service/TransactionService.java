package pl.proggo.bankapp.service;

import pl.proggo.bankapp.dto.TransactionDTO;
import pl.proggo.bankapp.dto.TransactionRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.Transaction;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.TransactionRepository;
import pl.proggo.bankapp.util.ReferenceNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public TransactionDTO createTransaction(Long accountId, TransactionRequest request, String username) {
        log.info("Creating transaction for account: {} by user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to perform transactions on this account");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transaction amount must be greater than zero");
        }

        if ("WITHDRAWAL".equalsIgnoreCase(request.getType())) {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new BusinessException("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(request.getAmount()));
        } else if ("DEPOSIT".equalsIgnoreCase(request.getType())) {
            account.setBalance(account.getBalance().add(request.getAmount()));
        } else {
            throw new BusinessException("Invalid transaction type: " + request.getType());
        }

        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setStatus("COMPLETED");
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(ReferenceNumberGenerator.generate());

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transaction created: {} for account: {}", savedTransaction.getId(), accountId);

        return mapToDTO(savedTransaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getAccountTransactions(Long accountId, String username, Pageable pageable) {
        log.info("Fetching transactions for account: {} by user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to view transactions on this account");
        }

        return transactionRepository.findByAccountId(accountId, pageable)
                .map(this::mapToDTO);
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getReferenceNumber(),
                transaction.getCreatedAt()
        );
    }
}