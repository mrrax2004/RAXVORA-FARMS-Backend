package com.dairyfarm.repository;

import com.dairyfarm.model.MilkStockTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MilkStockTransactionRepository extends JpaRepository<MilkStockTransaction, UUID> {
    @Query("select coalesce(sum(t.quantityChange),0) from MilkStockTransaction t where t.farm.uuid=:farm")
    BigDecimal stock(@Param("farm") UUID farm);
    Page<MilkStockTransaction> findAllByFarm_UuidOrderByCreatedAtDesc(UUID farm, Pageable pageable);
    @Query("select t from MilkStockTransaction t where t.farm.uuid=:farm and t.createdAt>=:from and t.createdAt<:to order by t.createdAt desc")
    Page<MilkStockTransaction> byCreatedRange(@Param("farm") UUID farm,@Param("from") java.time.Instant from,
                                              @Param("to") java.time.Instant to,Pageable pageable);
    @Query("select t from MilkStockTransaction t where t.farm.uuid=:farm and t.type=:type and t.createdAt>=:from and t.createdAt<:to")
    java.util.List<MilkStockTransaction> byTypeAndCreatedRange(@Param("farm") UUID farm,
            @Param("type") com.dairyfarm.model.MilkStockTransactionType type,@Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);
    boolean existsByFarm_UuidAndType(UUID farm, com.dairyfarm.model.MilkStockTransactionType type);
    boolean existsByProduction_UuidAndType(UUID production, com.dairyfarm.model.MilkStockTransactionType type);
    @Query("select coalesce(sum(t.quantityChange),0) from MilkStockTransaction t where t.farm.uuid=:farm and t.supply.uuid=:supply")
    BigDecimal sumByFarmAndSupply(@Param("farm") UUID farm, @Param("supply") UUID supply);
    @Query("select coalesce(sum(t.quantityChange),0) from MilkStockTransaction t where t.farm.uuid=:farm and t.type=:type and t.createdAt >= :start and t.createdAt < :end")
    BigDecimal sumByTypeAndCreatedRange(@Param("farm") UUID farm, @Param("type") com.dairyfarm.model.MilkStockTransactionType type,
                                        @Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
}
