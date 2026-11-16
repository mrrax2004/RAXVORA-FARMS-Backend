package com.dairyfarm.repository;

import com.dairyfarm.model.AavinSupplyAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinSupplyAuditRepository extends JpaRepository<AavinSupplyAudit, UUID> {
    List<AavinSupplyAudit> findAllByFarm_UuidAndSupply_UuidOrderByCreatedAtDesc(UUID farm, UUID supply);
}
