package com.dairyfarm.repository;

import com.dairyfarm.model.BuyerPaymentSchedule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerPaymentScheduleRepository extends JpaRepository<BuyerPaymentSchedule, UUID> {
    Optional<BuyerPaymentSchedule> findByCompany_UuidAndFarm_Uuid(UUID companyUuid, UUID farmUuid);
}
