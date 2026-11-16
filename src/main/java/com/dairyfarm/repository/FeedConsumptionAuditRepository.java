package com.dairyfarm.repository;

import com.dairyfarm.model.FeedConsumptionAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedConsumptionAuditRepository extends JpaRepository<FeedConsumptionAudit, UUID> {
    List<FeedConsumptionAudit> findAllByFarm_UuidAndConsumption_UuidOrderByCreatedAtDesc(UUID farm, UUID consumption);
}
