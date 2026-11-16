package com.dairyfarm.repository;

import com.dairyfarm.model.HealthAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthAttachmentRepository extends JpaRepository<HealthAttachment, UUID> {
    List<HealthAttachment> findAllByHealthRecord_UuidAndFarm_UuidOrderByCreatedAtAsc(UUID recordUuid, UUID farmUuid);
    Optional<HealthAttachment> findByUuidAndHealthRecord_UuidAndFarm_Uuid(UUID uuid, UUID recordUuid, UUID farmUuid);
}
