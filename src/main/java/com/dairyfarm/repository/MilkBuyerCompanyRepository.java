package com.dairyfarm.repository;

import com.dairyfarm.model.MilkBuyerCompany;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkBuyerCompanyRepository extends JpaRepository<MilkBuyerCompany, UUID> {
    List<MilkBuyerCompany> findAllByFarm_UuidOrderByCompanyNameAsc(UUID farmUuid);
    Optional<MilkBuyerCompany> findByUuidAndFarm_Uuid(UUID uuid, UUID farmUuid);
    boolean existsByFarm_UuidAndCompanyCodeIgnoreCase(UUID farmUuid, String companyCode);
    boolean existsByFarm_UuidAndCompanyCodeIgnoreCaseAndUuidNot(UUID farmUuid, String companyCode, UUID uuid);
}
