package com.dairyfarm.repository;

import com.dairyfarm.model.AavinMilkTest;
import com.dairyfarm.model.AavinVerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinMilkTestRepository extends JpaRepository<AavinMilkTest, UUID> {
    java.util.Optional<AavinMilkTest> findBySupply_UuidAndFarm_Uuid(UUID supply, UUID farm);
    boolean existsByFarm_UuidAndRateChart_UuidAndSupply_SupplyDateGreaterThan(
            UUID farm, UUID chart, LocalDate date);
    List<AavinMilkTest> findAllByFarm_UuidAndSupply_SupplyDateBetweenOrderBySupply_SupplyDateAsc(
            UUID farm, LocalDate from, LocalDate to);
    long countByFarm_UuidAndStatus(UUID farm, AavinVerificationStatus status);
}
