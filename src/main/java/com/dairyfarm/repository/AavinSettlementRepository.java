package com.dairyfarm.repository;

import com.dairyfarm.model.AavinSettlement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinSettlementRepository extends JpaRepository<AavinSettlement, UUID> {
    Page<AavinSettlement> findAllByFarm_UuidOrderByPeriodStartDesc(UUID farm, Pageable pageable);
    Optional<AavinSettlement> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndBuyer_UuidAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            UUID farm, UUID buyer, LocalDate start, LocalDate end);
    List<AavinSettlement> findAllByFarm_UuidAndBuyer_UuidOrderByPeriodEndDesc(UUID farm, UUID buyer);
    List<AavinSettlement> findAllByFarm_Uuid(UUID farm);
}
