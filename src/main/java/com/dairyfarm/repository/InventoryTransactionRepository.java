package com.dairyfarm.repository;

import com.dairyfarm.model.InventoryTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    Page<InventoryTransaction> findAllByFarm_Uuid(UUID farm, Pageable pageable);
    List<InventoryTransaction> findAllByFarm_UuidAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
            UUID farm, LocalDate from, LocalDate to);
}
