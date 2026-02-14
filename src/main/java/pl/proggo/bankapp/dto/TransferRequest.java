package pl.proggo.bankapp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Request object for creating a transfer between accounts.
 * Contains validation rules for transfer operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "From account number cannot be blank")
    private String fromAccountNumber;

    @NotBlank(message = "To account number cannot be blank")
    private String toAccountNumber;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
