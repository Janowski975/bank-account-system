package pl.proggo.bankapp.exception;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private String path;
}