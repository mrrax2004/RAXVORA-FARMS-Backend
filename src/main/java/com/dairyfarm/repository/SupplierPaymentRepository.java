package com.dairyfarm.repository;

import com.dairyfarm.model.SupplierPayment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, UUID> {
    List<SupplierPayment> findAllByFarm_UuidAndPurchase_UuidOrderByPaymentDateAscCreatedAtAsc(UUID farm, UUID purchase);
}
