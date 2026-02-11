package pl.proggo.bankapp.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account name cannot be blank")
    @Size(min = 3, max = 50)
    private String accountName;

    @NotBlank(message = "Currency cannot be blank")
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank(message = "Account type cannot be blank")
    private String accountType;
}