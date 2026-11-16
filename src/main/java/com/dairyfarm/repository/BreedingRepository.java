package com.dairyfarm.repository;

import com.dairyfarm.model.BreedingRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BreedingRepository extends JpaRepository<BreedingRecord, UUID>, JpaSpecificationExecutor<BreedingRecord> {
    Optional<BreedingRecord> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<BreedingRecord> findAllByFarm_UuidAndAnimal_UuidOrderByBreedingDateDescCreatedAtDesc(UUID farmUuid, UUID animalUuid);
    List<BreedingRecord> findAllByFarm_UuidOrderByBreedingDateDescCreatedAtDesc(UUID farmUuid);
}
