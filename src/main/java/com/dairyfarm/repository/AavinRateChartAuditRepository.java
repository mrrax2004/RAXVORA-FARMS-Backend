package com.dairyfarm.repository;

import com.dairyfarm.model.AavinRateChartAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinRateChartAuditRepository extends JpaRepository<AavinRateChartAudit, UUID> {
    List<AavinRateChartAudit> findAllByFarm_UuidAndChart_UuidOrderByCreatedAtDesc(UUID farm, UUID chart);
}
