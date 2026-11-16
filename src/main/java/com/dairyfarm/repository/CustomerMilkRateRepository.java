package com.dairyfarm.repository;

import com.dairyfarm.model.CustomerMilkRate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerMilkRateRepository extends JpaRepository<CustomerMilkRate, UUID> {
    List<CustomerMilkRate> findAllByFarm_UuidAndCustomer_UuidOrderByEffectiveFromDescCreatedAtDesc(UUID farm, UUID customer);
    Optional<CustomerMilkRate> findByUuidAndFarm_UuidAndCustomer_Uuid(UUID uuid, UUID farm, UUID customer);
    List<CustomerMilkRate> findAllByFarm_UuidAndCustomer_UuidAndStatusAndEffectiveFromLessThanEqual(
            UUID farm, UUID customer, com.dairyfarm.model.CustomerMilkRateStatus status, java.time.LocalDate date);
}
