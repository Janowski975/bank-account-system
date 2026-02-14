package pl.proggo.bankapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a transfer between two accounts.
 * Tracks money transfers with full audit trail including amounts, status, and timestamps.
 */
@Data
@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_from_account_id", columnList = "from_account_id"),
        @Index(name = "idx_to_account_id", columnList = "to_account_id"),
        @Index(name = "idx_transfer_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String referenceNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version;
}
