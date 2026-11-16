package com.dairyfarm.repository;

import com.dairyfarm.model.AavinRateSlab;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinRateSlabRepository extends JpaRepository<AavinRateSlab, UUID> {
    Optional<AavinRateSlab> findByChart_UuidAndFatPercentageAndSnfPercentage(
            UUID chart, BigDecimal fat, BigDecimal snf);
    List<AavinRateSlab> findAllByChart_UuidOrderByFatPercentageAscSnfPercentageAsc(UUID chart);
}
