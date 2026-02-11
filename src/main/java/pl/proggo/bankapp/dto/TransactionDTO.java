package pl.proggo.bankapp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String status;
    private String description;
    private String referenceNumber;
    private LocalDateTime createdAt;
}