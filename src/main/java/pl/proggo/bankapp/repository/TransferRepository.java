package pl.proggo.bankapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.proggo.bankapp.entity.Transfer;

/**
 * Repository for Transfer entity operations.
 * Provides data access methods for transfer-related queries.
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Find all transfers where the specified account is either sender or receiver.
     *
     * @param accountId The account ID to search for
     * @param pageable Pagination information
     * @return Page of transfers
     */
    @Query("SELECT t FROM Transfer t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transfer> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Find all transfers sent from the specified account.
     *
     * @param accountId The sender account ID
     * @param pageable Pagination information
     * @return Page of transfers
     */
    Page<Transfer> findByFromAccountId(Long accountId, Pageable pageable);

    /**
     * Find all transfers received by the specified account.
     *
     * @param accountId The receiver account ID
     * @param pageable Pagination information
     * @return Page of transfers
     */
    Page<Transfer> findByToAccountId(Long accountId, Pageable pageable);
}
