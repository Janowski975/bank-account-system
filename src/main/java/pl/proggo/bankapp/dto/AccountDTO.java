package pl.proggo.bankapp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;
    private String currency;
    private String accountType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}