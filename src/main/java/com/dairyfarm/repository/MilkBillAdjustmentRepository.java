package com.dairyfarm.repository;

import com.dairyfarm.model.MilkBillAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkBillAdjustmentRepository extends JpaRepository<MilkBillAdjustment, UUID> {
    List<MilkBillAdjustment> findAllByBill_UuidOrderByCreatedAtAsc(UUID billUuid);
}
