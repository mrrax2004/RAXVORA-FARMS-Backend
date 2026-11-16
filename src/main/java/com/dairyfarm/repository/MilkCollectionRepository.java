package com.dairyfarm.repository;

import com.dairyfarm.model.MilkCollection;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilkCollectionRepository extends JpaRepository<MilkCollection, UUID> {
    Page<MilkCollection> findAllByFarm_UuidAndCollectionDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    java.util.List<MilkCollection> findAllByFarm_UuidAndCollectionDateBetweenOrderByCollectionDateAsc(UUID farm, LocalDate from, LocalDate to);
    java.util.List<MilkCollection> findAllByFarm_UuidAndCollectionDateBetween(UUID farm, LocalDate from, LocalDate to);
}
