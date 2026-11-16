package com.dairyfarm.repository;

import com.dairyfarm.model.FeedRation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRationRepository extends JpaRepository<FeedRation, UUID> {
    List<FeedRation> findAllByFarm_UuidOrderByNameAsc(UUID farm);
    Optional<FeedRation> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
}
