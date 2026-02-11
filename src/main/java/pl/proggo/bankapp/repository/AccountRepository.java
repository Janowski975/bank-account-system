package pl.proggo.bankapp.repository;

import pl.proggo.bankapp.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account account = new pl.proggo.bankapp.entity.Account();
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.isActive = true")
    List<Account> findActiveAccountsByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    List<Account> findAllByUserId(@Param("userId") Long userId);

    boolean existsByAccountNumber(String accountNumber);
}