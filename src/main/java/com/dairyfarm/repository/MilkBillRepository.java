package com.dairyfarm.repository;

import com.dairyfarm.model.MilkBill;
import com.dairyfarm.model.MilkBillingFrequency;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkBillRepository extends JpaRepository<MilkBill, UUID> {
    java.util.List<MilkBill> findAllByFarm_Uuid(UUID farm);
    Optional<MilkBill> findByFarm_UuidAndCustomer_UuidAndBillingFrequencyAndPeriodStartAndPeriodEnd(
            UUID farm, UUID customer, MilkBillingFrequency frequency, LocalDate start, LocalDate end);
    Optional<MilkBill> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    Page<MilkBill> findAllByFarm_UuidAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    Page<MilkBill> findAllByFarm_UuidAndCustomer_UuidAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            UUID farm, UUID customer, LocalDate from, LocalDate to, Pageable pageable);
}
