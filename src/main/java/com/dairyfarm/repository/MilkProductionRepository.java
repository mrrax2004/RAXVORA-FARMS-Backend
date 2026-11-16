package com.dairyfarm.repository;

import com.dairyfarm.model.MilkProduction;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MilkProductionRepository extends JpaRepository<MilkProduction, UUID> {
    Page<MilkProduction> findAllByFarm_UuidAndProductionDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    Page<MilkProduction> findAllByFarm_UuidAndAnimal_UuidAndProductionDateBetween(UUID farm, UUID animal, LocalDate from, LocalDate to, Pageable pageable);
    @Query("select coalesce(sum(p.quantity),0) from MilkProduction p where p.farm.uuid=:farm and p.productionDate between :from and :to")
    java.math.BigDecimal sumQuantity(@Param("farm") UUID farm, @Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("select coalesce(sum(p.quantity),0) from MilkProduction p where p.farm.uuid=:farm and p.productionDate=:date and p.session=:session")
    java.math.BigDecimal sumByDateAndSession(@Param("farm") UUID farm, @Param("date") LocalDate date, @Param("session") com.dairyfarm.model.MilkingSession session);
    @Query("select p from MilkProduction p where p.farm.uuid=:farm and p.productionDate between :from and :to order by p.productionDate,p.session")
    java.util.List<MilkProduction> report(@Param("farm") UUID farm, @Param("from") LocalDate from, @Param("to") LocalDate to);
    java.util.List<MilkProduction> findAllByFarm_UuidAndAnimal_UuidAndProductionDateBetweenOrderByProductionDateAscSessionAsc(UUID farm, UUID animal, LocalDate from, LocalDate to);
    java.util.List<MilkProduction> findAllByFarm_UuidAndProductionDateBetweenOrderByProductionDateAscSessionAsc(UUID farm, LocalDate from, LocalDate to);
}
