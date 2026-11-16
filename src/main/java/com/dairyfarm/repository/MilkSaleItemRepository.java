package com.dairyfarm.repository;
import com.dairyfarm.model.MilkSaleItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilkSaleItemRepository extends JpaRepository<MilkSaleItem, UUID> { }
