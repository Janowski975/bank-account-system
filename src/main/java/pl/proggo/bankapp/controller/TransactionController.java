package pl.proggo.bankapp.controller;

import pl.proggo.bankapp.dto.TransactionDTO;
import pl.proggo.bankapp.dto.TransactionRequest;
import pl.proggo.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @PathVariable Long accountId,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        log.info("Create transaction endpoint called for account: {}", accountId);
        TransactionDTO response = transactionService.createTransaction(accountId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        log.info("Get transactions endpoint called for account: {}", accountId);
        Page<TransactionDTO> response = transactionService.getAccountTransactions(accountId, authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }
}