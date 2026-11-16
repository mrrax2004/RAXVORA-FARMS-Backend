package com.dairyfarm.repository;

import com.dairyfarm.model.InventoryItem;
import com.dairyfarm.model.InventoryItemType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findAllByFarm_UuidOrderByNameAsc(UUID farm);
    List<InventoryItem> findAllByFarm_UuidAndTypeOrderByNameAsc(UUID farm, InventoryItemType type);
    Optional<InventoryItem> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndNameIgnoreCaseAndBatchNumberIgnoreCase(UUID farm, String name, String batch);
    boolean existsByFarm_UuidAndNameIgnoreCaseAndBatchNumberIsNull(UUID farm, String name);
    List<InventoryItem> findAllByFarm_UuidAndExpiryDateIsNotNullAndExpiryDateLessThanEqual(UUID farm, LocalDate date);
}
