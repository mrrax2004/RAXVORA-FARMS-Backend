package com.dairyfarm.repository;

import com.dairyfarm.model.Animal;
import com.dairyfarm.model.PregnancyStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnimalRepository extends JpaRepository<Animal, UUID>, JpaSpecificationExecutor<Animal> {
    Optional<Animal> findByUuidAndFarm_UuidAndArchivedFalse(UUID uuid, UUID farmUuid);
    boolean existsByFarm_UuidAndTagNumberIgnoreCaseAndArchivedFalse(UUID farmUuid, String tagNumber);
    boolean existsByFarm_UuidAndTagNumberIgnoreCaseAndArchivedFalseAndUuidNot(UUID farmUuid, String tagNumber, UUID uuid);
    long countByFarm_UuidAndArchivedFalse(UUID farmUuid);
    long countByFarm_UuidAndStatusAndArchivedFalse(UUID farmUuid, com.dairyfarm.model.AnimalStatus status);
    long countByFarm_UuidAndTypeAndArchivedFalse(UUID farmUuid, com.dairyfarm.model.AnimalType type);
    long countByFarm_UuidAndPregnancyStatusAndArchivedFalse(UUID farmUuid, PregnancyStatus status);
    long countByFarm_UuidAndPregnancyStatusAndExpectedDeliveryDateBetweenAndArchivedFalse(
            UUID farmUuid, PregnancyStatus status, LocalDate start, LocalDate end);
    List<Animal> findAllByFarm_UuidAndPregnancyStatusAndArchivedFalseAndExpectedDeliveryDateBetweenOrderByExpectedDeliveryDateAsc(
            UUID farmUuid, PregnancyStatus status, LocalDate start, LocalDate end);
    List<Animal> findAllByFarm_UuidAndArchivedFalse(UUID farmUuid);
}
