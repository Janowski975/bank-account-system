package pl.proggo.bankapp.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.proggo.bankapp.dto.TransferDTO;
import pl.proggo.bankapp.dto.TransferRequest;
import pl.proggo.bankapp.service.TransferService;

/**
 * REST Controller for transfer operations.
 * Provides endpoints for transferring money between accounts and viewing transfer history.
 */
@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Transfer money between accounts.
     *
     * @param request Transfer request containing from/to account numbers and amount
     * @param authentication Authenticated user details
     * @return TransferDTO with transfer details
     */
    @PostMapping
    public ResponseEntity<TransferDTO> transferMoney(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        
        TransferDTO transfer = transferService.transferBetweenAccounts(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }

    /**
     * Get transfer details by ID.
     *
     * @param transferId Transfer ID
     * @param authentication Authenticated user details
     * @return TransferDTO with transfer details
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferDTO> getTransferDetails(
            @PathVariable Long transferId,
            Authentication authentication) {
        
        TransferDTO transfer = transferService.getTransfer(transferId, authentication.getName());
        return ResponseEntity.ok(transfer);
    }

    /**
     * Get all transfers for an account (sent and received).
     *
     * @param accountNumber Account number
     * @param authentication Authenticated user details
     * @param page Page number (default 0)
     * @param size Page size (default 10)
     * @return Page of TransferDTO
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransferDTO>> getAccountTransfers(
            @PathVariable String accountNumber,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferDTO> transfers = transferService.getAccountTransfers(accountNumber, authentication.getName(), pageable);
        return ResponseEntity.ok(transfers);
    }
}
