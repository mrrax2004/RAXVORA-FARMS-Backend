package com.dairyfarm.repository;

import com.dairyfarm.model.FinanceTemplateStatus;
import com.dairyfarm.model.RecurringFinanceTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringFinanceTemplateRepository extends JpaRepository<RecurringFinanceTemplate, UUID> {
    List<RecurringFinanceTemplate> findAllByFarm_UuidOrderByNextDateAsc(UUID farm);
    List<RecurringFinanceTemplate> findAllByStatusAndNextDateLessThanEqualOrderByNextDateAsc(
            FinanceTemplateStatus status, LocalDate date);
    java.util.Optional<RecurringFinanceTemplate> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
}
