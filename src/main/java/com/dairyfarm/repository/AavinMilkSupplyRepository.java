package com.dairyfarm.repository;

import com.dairyfarm.model.AavinMilkSupply;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AavinMilkSupplyRepository extends JpaRepository<AavinMilkSupply, UUID> {
    Page<AavinMilkSupply> findAllByFarm_UuidAndSupplyDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    Page<AavinMilkSupply> findAllByFarm_UuidAndBuyer_UuidAndSupplyDateBetween(UUID farm, UUID buyer, LocalDate from, LocalDate to, Pageable pageable);
    List<AavinMilkSupply> findAllByFarm_UuidAndSupplyDateBetweenOrderBySupplyDateAscSessionAsc(UUID farm, LocalDate from, LocalDate to);
    Optional<AavinMilkSupply> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsByFarm_UuidAndBuyer_UuidAndReceiptNumber(UUID farm, UUID buyer, String receiptNumber);
    boolean existsByFarm_UuidAndBuyer_UuidAndReceiptNumberAndUuidNot(UUID farm, UUID buyer, String receiptNumber, UUID uuid);
    List<AavinMilkSupply> findAllByFarm_UuidAndSupplyDate(UUID farm, LocalDate date);
    List<AavinMilkSupply> findAllByFarm_Uuid(UUID farm);
}
