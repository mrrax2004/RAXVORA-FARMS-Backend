package com.dairyfarm.repository;

import com.dairyfarm.model.FinanceTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
    List<FinanceTransaction> findAllByFarm_UuidOrderByDateAscCreatedAtAsc(UUID farm);
    List<FinanceTransaction> findAllByFarm_UuidAndDateBetweenOrderByDateDescCreatedAtDesc(
            UUID farm, LocalDate from, LocalDate to);
    List<FinanceTransaction> findAllByFarm_UuidAndAccount_Uuid(UUID farm, UUID account);
    List<FinanceTransaction> findAllByFarm_UuidAndAccount_UuidAndDateBetween(
            UUID farm, UUID account, LocalDate from, LocalDate to);
    boolean existsByRecurringTemplate_UuidAndOccurrenceDate(UUID template, LocalDate date);
}
