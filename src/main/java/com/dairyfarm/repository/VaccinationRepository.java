package com.dairyfarm.repository;

import com.dairyfarm.model.VaccinationRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VaccinationRepository extends JpaRepository<VaccinationRecord, UUID>, JpaSpecificationExecutor<VaccinationRecord> {
    Optional<VaccinationRecord> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<VaccinationRecord> findAllByFarm_UuidAndAnimal_UuidOrderByVaccinationDateDescCreatedAtDesc(UUID farmUuid, UUID animalUuid);
    List<VaccinationRecord> findAllByFarm_UuidOrderByVaccinationDateDescCreatedAtDesc(UUID farmUuid);
    long countByFarm_UuidAndNextVaccinationDateBetween(UUID farmUuid, LocalDate start, LocalDate end);
}
