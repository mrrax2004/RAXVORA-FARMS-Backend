package com.dairyfarm.repository;

import com.dairyfarm.model.FeedPurchase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPurchaseRepository extends JpaRepository<FeedPurchase, UUID> {
    List<FeedPurchase> findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(UUID farm);
    List<FeedPurchase> findAllByFarm_UuidAndPurchaseDateBetweenOrderByPurchaseDateAsc(UUID farm, LocalDate from, LocalDate to);
    Optional<FeedPurchase> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndInvoiceNumberAndItem_Uuid(UUID farm, String invoice, UUID item);
}
