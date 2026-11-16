package com.dairyfarm.repository;

import com.dairyfarm.model.AavinAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinAdjustmentRepository extends JpaRepository<AavinAdjustment, UUID> {
    List<AavinAdjustment> findAllByFarm_UuidAndSettlement_Uuid(UUID farm, UUID settlement);
}
