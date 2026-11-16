package com.dairyfarm.repository;

import com.dairyfarm.model.MilkBillItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkBillItemRepository extends JpaRepository<MilkBillItem, UUID> {
    List<MilkBillItem> findAllByBill_UuidOrderByDelivery_DeliveryDateAsc(UUID billUuid);
    boolean existsByDelivery_Uuid(UUID deliveryUuid);
}
