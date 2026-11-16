package com.dairyfarm.repository;

import com.dairyfarm.model.Reminder;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReminderRepository extends JpaRepository<Reminder, UUID>, JpaSpecificationExecutor<Reminder> {
    Optional<Reminder> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    Optional<Reminder> findByFarm_UuidAndSourceKey(UUID farmUuid, String sourceKey);
    Page<Reminder> findAllByFarm_Uuid(UUID farmUuid, Pageable pageable);
    Page<Reminder> findAllByFarm_UuidAndStatus(UUID farmUuid, ReminderStatus status, Pageable pageable);
    Page<Reminder> findAllByFarm_UuidAndDueAtBetween(UUID farmUuid, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Reminder> findAllByFarm_UuidAndStatusAndDueAtBetween(UUID farmUuid, ReminderStatus status,
                                                               LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Reminder> findAllByFarm_UuidAndDueAtBefore(UUID farmUuid, LocalDateTime before, Pageable pageable);
    Page<Reminder> findAllByFarm_UuidAndStatusAndDueAtBefore(UUID farmUuid, ReminderStatus status,
                                                              LocalDateTime before, Pageable pageable);
    List<Reminder> findAllByFarm_UuidAndCategoryAndDueAtBetweenAndStatus(
            UUID farmUuid, ReminderCategory category, LocalDateTime start, LocalDateTime end, ReminderStatus status);
    List<Reminder> findAllByFarm_UuidAndAnimal_UuidAndCategoryAndStatus(
            UUID farmUuid, UUID animalUuid, ReminderCategory category, ReminderStatus status);
    List<Reminder> findAllByFarm_UuidAndAnimal_UuidAndCategoryAndStatusIn(
            UUID farmUuid, UUID animalUuid, ReminderCategory category, java.util.Collection<ReminderStatus> statuses);
    long countByFarm_UuidAndStatusAndDueAtBefore(UUID farmUuid, ReminderStatus status, LocalDateTime dueAt);
    long countByFarm_UuidAndStatusAndCategoryAndDueAtBetween(
            UUID farmUuid, ReminderStatus status, ReminderCategory category, LocalDateTime start, LocalDateTime end);
}
