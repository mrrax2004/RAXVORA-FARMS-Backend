package com.dairyfarm.repository;

import com.dairyfarm.model.InAppNotification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<InAppNotification, UUID> {
    Page<InAppNotification> findAllByFarm_UuidAndUser_UuidOrderByCreatedAtDesc(UUID farmUuid, UUID userUuid, Pageable pageable);
    Page<InAppNotification> findAllByFarm_UuidAndUser_UuidAndReadFalseOrderByCreatedAtDesc(UUID farmUuid, UUID userUuid, Pageable pageable);
    Optional<InAppNotification> findByUuidAndFarm_UuidAndUser_Uuid(UUID uuid, UUID farmUuid, UUID userUuid);
    boolean existsByUser_UuidAndReminder_Uuid(UUID userUuid, UUID reminderUuid);
    long countByFarm_UuidAndUser_UuidAndReadFalse(UUID farmUuid, UUID userUuid);
    java.util.List<InAppNotification> findAllByReminder_Uuid(UUID reminderUuid);
}
