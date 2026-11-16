package com.dairyfarm.repository;

import com.dairyfarm.model.AavinRateChart;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinRateChartRepository extends JpaRepository<AavinRateChart, UUID> {
    List<AavinRateChart> findAllByFarm_UuidAndBuyer_UuidOrderByEffectiveFromDesc(UUID farm, UUID buyer);
    List<AavinRateChart> findAllByFarm_UuidAndBuyer_UuidAndEffectiveFromLessThanEqual(UUID farm, UUID buyer, LocalDate date);
    java.util.Optional<AavinRateChart> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
}
