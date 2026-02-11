package pl.proggo.bankapp.controller;

import pl.proggo.bankapp.dto.AccountDTO;
import pl.proggo.bankapp.dto.CreateAccountRequest;
import pl.proggo.bankapp.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        log.info("Create account endpoint called for user: {}", authentication.getName());
        AccountDTO response = accountService.createAccount(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccount(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Get account endpoint called for account: {}", id);
        AccountDTO response = accountService.getAccount(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getUserAccounts(Authentication authentication) {
        log.info("Get user accounts endpoint called for user: {}", authentication.getName());
        List<AccountDTO> response = accountService.getUserAccounts(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        log.info("Update account endpoint called for account: {}", id);
        AccountDTO response = accountService.updateAccount(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Delete account endpoint called for account: {}", id);
        accountService.deleteAccount(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}