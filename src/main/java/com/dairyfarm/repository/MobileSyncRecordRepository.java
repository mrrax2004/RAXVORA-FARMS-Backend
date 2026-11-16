package com.dairyfarm.repository;

import com.dairyfarm.model.MobileSyncRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileSyncRecordRepository extends JpaRepository<MobileSyncRecord, UUID> {
    Optional<MobileSyncRecord> findByFarm_UuidAndDeviceUuidAndClientRecordUuid(
            UUID farm, UUID device, UUID clientRecord);
}
