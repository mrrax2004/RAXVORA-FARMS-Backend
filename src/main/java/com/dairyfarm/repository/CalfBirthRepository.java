package com.dairyfarm.repository;

import com.dairyfarm.model.CalfBirth;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalfBirthRepository extends JpaRepository<CalfBirth, UUID> {
    List<CalfBirth> findAllByCalving_UuidAndVoidedFalseOrderByCreatedAtAsc(UUID calvingUuid);
}
