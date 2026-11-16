package com.dairyfarm.repository;

import com.dairyfarm.model.AavinSettlementItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinSettlementItemRepository extends JpaRepository<AavinSettlementItem, UUID> {
    List<AavinSettlementItem> findAllByFarm_UuidAndSettlement_UuidOrderBySupply_SupplyDateAsc(UUID farm, UUID settlement);
    boolean existsByFarm_UuidAndSupply_Uuid(UUID farm, UUID supply);
}
