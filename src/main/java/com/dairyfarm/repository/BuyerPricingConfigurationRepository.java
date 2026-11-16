package com.dairyfarm.repository;

import com.dairyfarm.model.BuyerPricingConfiguration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerPricingConfigurationRepository extends JpaRepository<BuyerPricingConfiguration, UUID> {
    List<BuyerPricingConfiguration> findAllByFarm_UuidAndCompany_UuidOrderByEffectiveFromDesc(UUID farmUuid, UUID companyUuid);
    List<BuyerPricingConfiguration> findAllByFarm_UuidAndCompany_UuidAndEffectiveFromLessThanEqual(
            UUID farmUuid, UUID companyUuid, LocalDate date);
    Optional<BuyerPricingConfiguration> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
}
