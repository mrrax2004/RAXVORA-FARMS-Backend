package com.dairyfarm.repository;

import com.dairyfarm.model.MilkBuyer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkBuyerRepository extends JpaRepository<MilkBuyer, UUID> {
    List<MilkBuyer> findAllByFarm_UuidOrderByCentreNameAsc(UUID farm);
    Optional<MilkBuyer> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndCentreCodeIgnoreCase(UUID farm, String centreCode);
    List<MilkBuyer> findAllByFarm_UuidAndStatus(UUID farm, com.dairyfarm.model.MilkBuyerStatus status);
}
