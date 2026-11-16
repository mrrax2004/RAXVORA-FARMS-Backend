package com.dairyfarm.repository;

import com.dairyfarm.model.Farm;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FarmRepository extends JpaRepository<Farm, UUID> {
    Optional<Farm> findFirstByNameIgnoreCase(String name);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Farm f where f.uuid = :uuid")
    Optional<Farm> findLockedByUuid(@Param("uuid") UUID uuid);
}
