package com.dairyfarm.repository;

import com.dairyfarm.model.HealthRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID>, JpaSpecificationExecutor<HealthRecord> {
    boolean existsByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    Optional<HealthRecord> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<HealthRecord> findAllByFarm_UuidAndAnimal_UuidOrderByTreatmentStartDateDescCreatedAtDesc(UUID farmUuid, UUID animalUuid);
    List<HealthRecord> findAllByFarm_UuidOrderByTreatmentStartDateDescCreatedAtDesc(UUID farmUuid);
    long countByFarm_UuidAndTreatmentStartDateLessThanEqualAndTreatmentEndDateGreaterThanEqualOrFarm_UuidAndTreatmentStartDateLessThanEqualAndTreatmentEndDateIsNull(
            UUID farmUuid, LocalDate today, LocalDate today2, UUID farmUuid2, LocalDate today3);
}
