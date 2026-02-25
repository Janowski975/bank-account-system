package pl.proggo.bankapp.service;

import pl.proggo.bankapp.dto.AccountDTO;
import pl.proggo.bankapp.dto.CreateAccountRequest;
import pl.proggo.bankapp.dto.DepositRequest;
import pl.proggo.bankapp.entity.Account;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.exception.ResourceNotFoundException;
import pl.proggo.bankapp.repository.AccountRepository;
import pl.proggo.bankapp.repository.UserRepository;
import pl.proggo.bankapp.util.AccountNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public AccountDTO createAccount(String username, CreateAccountRequest request) {
        log.info("Creating account for user: {}", username);

        if (request == null) {
            throw new BusinessException("Account request cannot be null");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String accountNumber = AccountNumberGenerator.generate();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = AccountNumberGenerator.generate();
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountName(request.getAccountName());
        account.setUser(user);
        account.setCurrency(request.getCurrency());
        account.setAccountType(request.getAccountType());
        account.setIsActive(true);

        Account savedAccount = accountRepository.save(account);

        log.info("Account created: {} for user: {}", accountNumber, username);

        return mapToDTO(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountDTO getAccount(Long accountId, String username) {
        log.info("Fetching account: {} for user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to access this account");
        }

        return mapToDTO(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getUserAccounts(String username) {
        log.info("Fetching accounts for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return accountRepository.findActiveAccountsByUserId(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO updateAccount(Long accountId, CreateAccountRequest request, String username) {
        log.info("Updating account: {} for user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to update this account");
        }

        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());

        Account updatedAccount = accountRepository.save(account);

        log.info("Account updated: {}", accountId);

        return mapToDTO(updatedAccount);
    }

    /**
     * Deposit money into account.
     *
     * @param accountId Account ID
     * @param request Deposit request
     * @param username Username
     * @return Updated AccountDTO
     */
    public AccountDTO depositMoney(Long accountId, DepositRequest request, String username) {
        log.info("Processing deposit for account: {} by user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        // Validate ownership
        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to deposit to this account");
        }

        // Validate account is active
        if (!account.getIsActive()) {
            throw new BusinessException("Account is not active");
        }

        // Add amount to balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        log.info("Deposit completed for account: {}", accountId);
        return mapToDTO(account);
    }

    public void deleteAccount(Long accountId, String username) {
        log.info("Deleting account: {} for user: {}", accountId, username);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new BusinessException("User is not authorized to delete this account");
        }

        account.setIsActive(false);
        accountRepository.save(account);

        log.info("Account deleted: {}", accountId);
    }

    private AccountDTO mapToDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountType(),
                account.getIsActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}