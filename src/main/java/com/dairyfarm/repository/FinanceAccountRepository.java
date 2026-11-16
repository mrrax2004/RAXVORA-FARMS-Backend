package com.dairyfarm.repository;

import com.dairyfarm.model.FinanceAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, UUID> {
    List<FinanceAccount> findAllByFarm_UuidOrderByNameAsc(UUID farm);
    Optional<FinanceAccount> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndNameIgnoreCase(UUID farm, String name);
}
