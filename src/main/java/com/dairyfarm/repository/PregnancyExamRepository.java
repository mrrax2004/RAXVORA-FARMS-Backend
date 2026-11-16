package com.dairyfarm.repository;

import com.dairyfarm.model.PregnancyExam;
import com.dairyfarm.model.PregnancyResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PregnancyExamRepository extends JpaRepository<PregnancyExam, UUID>, JpaSpecificationExecutor<PregnancyExam> {
    Optional<PregnancyExam> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<PregnancyExam> findAllByFarm_UuidAndAnimal_UuidOrderByExamDateDescCreatedAtDesc(UUID farmUuid, UUID animalUuid);
    List<PregnancyExam> findAllByFarm_UuidOrderByExamDateDescCreatedAtDesc(UUID farmUuid);
    Optional<PregnancyExam> findFirstByFarm_UuidAndAnimal_UuidAndResultNotOrderByExamDateDescCreatedAtDesc(
            UUID farmUuid, UUID animalUuid, PregnancyResult excluded);
}
