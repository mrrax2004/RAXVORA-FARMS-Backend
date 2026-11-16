package com.dairyfarm.repository;
import com.dairyfarm.model.MilkDelivery;
import com.dairyfarm.model.MilkDeliveryStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilkDeliveryRepository extends JpaRepository<MilkDelivery, UUID> {
    Page<MilkDelivery> findAllByFarm_UuidAndDeliveryDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    java.util.Optional<MilkDelivery> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    boolean existsBySubscription_UuidAndDeliveryDate(UUID subscription, LocalDate date);
    boolean existsBySubscription_Uuid(UUID subscription);
    long countByFarm_UuidAndDeliveryDateAndStatus(UUID farm, LocalDate date, MilkDeliveryStatus status);
    java.util.List<MilkDelivery> findAllByFarm_UuidAndSubscription_Customer_UuidAndDeliveryDateBetweenAndStatusOrderByDeliveryDateAsc(
            UUID farm, UUID customer, LocalDate from, LocalDate to, MilkDeliveryStatus status);
}
