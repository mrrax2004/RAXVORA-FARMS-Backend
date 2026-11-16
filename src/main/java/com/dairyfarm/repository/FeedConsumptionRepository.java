package com.dairyfarm.repository;

import com.dairyfarm.model.FeedConsumption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedConsumptionRepository extends JpaRepository<FeedConsumption, UUID> {
    Page<FeedConsumption> findAllByFarm_UuidAndDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    List<FeedConsumption> findAllByFarm_UuidAndDateBetween(UUID farm, LocalDate from, LocalDate to);
    Optional<FeedConsumption> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
}
