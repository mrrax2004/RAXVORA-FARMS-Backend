package com.dairyfarm.repository;

import com.dairyfarm.model.MilkProductionAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilkProductionAuditRepository extends JpaRepository<MilkProductionAudit, UUID> {
    List<MilkProductionAudit> findAllByFarm_UuidAndProduction_UuidOrderByCreatedAtDesc(UUID farm, UUID production);
}
