package pl.proggo.bankapp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Transfer operations.
 * Contains transfer details including account numbers, amount, and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferDTO {

    private Long id;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String status;
    private String description;
    private String referenceNumber;
    private LocalDateTime timestamp;
}
