package com.dairyfarm.repository;
import com.dairyfarm.model.MilkSubscription;
import com.dairyfarm.model.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MilkSubscriptionRepository extends JpaRepository<MilkSubscription, UUID> {
    Page<MilkSubscription> findAllByFarm_Uuid(UUID farm, Pageable pageable);
    @Query("select s from MilkSubscription s where s.farm.uuid=:farm and s.status=:status and s.startDate<=:date and (s.endDate is null or s.endDate>=:date)")
    List<MilkSubscription> activeOnDate(@Param("farm") UUID farm, @Param("status") SubscriptionStatus status, @Param("date") LocalDate date);
    java.util.Optional<MilkSubscription> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
}
