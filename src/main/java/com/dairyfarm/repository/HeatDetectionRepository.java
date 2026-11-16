package com.dairyfarm.repository;

import com.dairyfarm.model.HeatDetection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HeatDetectionRepository extends JpaRepository<HeatDetection, UUID>, JpaSpecificationExecutor<HeatDetection> {
    Optional<HeatDetection> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<HeatDetection> findAllByFarm_UuidAndAnimal_UuidOrderByDetectionDateDescCreatedAtDesc(UUID farmUuid, UUID animalUuid);
    List<HeatDetection> findAllByFarm_UuidOrderByDetectionDateDescCreatedAtDesc(UUID farmUuid);
    long countByFarm_UuidAndDetectionDateBetween(UUID farmUuid, LocalDate start, LocalDate end);
    @org.springframework.data.jpa.repository.Query("select count(distinct h.animal.uuid) from HeatDetection h where h.farm.uuid = :farmUuid and h.detectionDate between :start and :end")
    long countDistinctAnimalsInHeat(UUID farmUuid, LocalDate start, LocalDate end);
}
