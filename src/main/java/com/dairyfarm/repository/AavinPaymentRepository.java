package com.dairyfarm.repository;

import com.dairyfarm.model.AavinPayment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AavinPaymentRepository extends JpaRepository<AavinPayment, UUID> {
    List<AavinPayment> findAllByFarm_UuidAndSettlement_UuidOrderByPaymentDateAscCreatedAtAsc(UUID farm, UUID settlement);
    List<AavinPayment> findAllByFarm_UuidAndPaymentDateBetweenOrderByPaymentDateAscCreatedAtAsc(
            UUID farm, LocalDate from, LocalDate to);
    @Query("select coalesce(sum(p.amount),0) from AavinPayment p where p.farm.uuid=:farm and p.settlement.uuid=:settlement")
    java.math.BigDecimal totalForSettlement(@Param("farm") UUID farm, @Param("settlement") UUID settlement);
    @Query("select coalesce(sum(s.netPayable),0) from AavinSettlement s where s.farm.uuid=:farm and s.status<>com.dairyfarm.model.AavinSettlementStatus.PAID")
    java.math.BigDecimal pendingPayable(@Param("farm") UUID farm);
    java.util.Optional<AavinPayment> findFirstByFarm_UuidOrderByPaymentDateDescCreatedAtDesc(UUID farm);
}
