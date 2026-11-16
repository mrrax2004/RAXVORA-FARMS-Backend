package com.dairyfarm.repository;

import com.dairyfarm.model.Supplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findAllByFarm_UuidOrderByNameAsc(UUID farm);
    Optional<Supplier> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndNameIgnoreCase(UUID farm, String name);
    boolean existsByFarm_UuidAndNameIgnoreCaseAndUuidNot(UUID farm, String name, UUID uuid);
}
