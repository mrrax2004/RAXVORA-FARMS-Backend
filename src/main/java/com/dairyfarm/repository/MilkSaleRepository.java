package com.dairyfarm.repository;

import com.dairyfarm.model.MilkSale;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MilkSaleRepository extends JpaRepository<MilkSale, UUID> {
    Page<MilkSale> findAllByFarm_UuidAndSaleDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    Page<MilkSale> findAllByFarm_UuidAndCustomer_UuidAndSaleDateBetween(UUID farm, UUID customer, LocalDate from, LocalDate to, Pageable pageable);
    java.util.Optional<MilkSale> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    java.util.Optional<MilkSale> findByInvoiceNumberAndFarm_Uuid(String invoice, UUID farm);
    @Query("select coalesce(sum(s.totalAmount),0) from MilkSale s where s.farm.uuid=:farm and s.saleDate between :from and :to")
    java.math.BigDecimal revenue(@Param("farm") UUID farm, @Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("select coalesce(sum(s.quantity),0) from MilkSale s where s.farm.uuid=:farm and s.saleDate=:date")
    java.math.BigDecimal soldQuantity(@Param("farm") UUID farm, @Param("date") LocalDate date);
    java.util.List<MilkSale> findAllByFarm_UuidAndSaleDateBetweenOrderBySaleDateAsc(UUID farm, LocalDate from, LocalDate to);
    java.util.List<MilkSale> findAllByFarm_UuidOrderBySaleDateAsc(UUID farm);
}
