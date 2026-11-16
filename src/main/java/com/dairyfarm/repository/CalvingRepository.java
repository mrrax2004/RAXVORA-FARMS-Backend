package com.dairyfarm.repository;

import com.dairyfarm.model.CalvingRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CalvingRepository extends JpaRepository<CalvingRecord, UUID>, JpaSpecificationExecutor<CalvingRecord> {
    Optional<CalvingRecord> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    List<CalvingRecord> findAllByFarm_UuidAndMother_UuidOrderByActualDeliveryDateDescCreatedAtDesc(UUID farmUuid, UUID motherUuid);
    List<CalvingRecord> findAllByFarm_UuidOrderByActualDeliveryDateDescCreatedAtDesc(UUID farmUuid);
    Optional<CalvingRecord> findFirstByFarm_UuidAndMother_UuidOrderByActualDeliveryDateDescCreatedAtDesc(UUID farmUuid, UUID motherUuid);
    long countByFarm_UuidAndActualDeliveryDateBetween(UUID farmUuid, LocalDate start, LocalDate end);
}
