package com.dairyfarm.repository;

import com.dairyfarm.model.Breed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<Breed, UUID> {
    List<Breed> findAllByFarm_Uuid(UUID farmUuid);
    Optional<Breed> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    boolean existsByFarm_UuidAndNameIgnoreCase(UUID farmUuid, String name);
    long countByFarm_Uuid(UUID farmUuid);
}
