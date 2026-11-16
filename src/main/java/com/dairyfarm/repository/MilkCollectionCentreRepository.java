package com.dairyfarm.repository;

import com.dairyfarm.model.MilkCollectionCentre;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkCollectionCentreRepository extends JpaRepository<MilkCollectionCentre, UUID> {
    List<MilkCollectionCentre> findAllByFarm_UuidAndCompany_UuidOrderByCentreNameAsc(UUID farmUuid, UUID companyUuid);
    List<MilkCollectionCentre> findAllByFarm_UuidOrderByCompany_CompanyNameAscCentreNameAsc(UUID farmUuid);
    Optional<MilkCollectionCentre> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    Optional<MilkCollectionCentre> findByLegacyBuyer_UuidAndFarm_Uuid(UUID legacyBuyerUuid, UUID farmUuid);
    boolean existsByFarm_UuidAndCentreCodeIgnoreCase(UUID farmUuid, String centreCode);
    boolean existsByFarm_UuidAndCentreCodeIgnoreCaseAndUuidNot(UUID farmUuid, String centreCode, UUID uuid);
}
