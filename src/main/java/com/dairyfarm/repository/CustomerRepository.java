package com.dairyfarm.repository;

import com.dairyfarm.model.Customer;
import com.dairyfarm.model.CustomerStatus;
import com.dairyfarm.model.CustomerType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findAllByFarm_UuidAndNameContainingIgnoreCaseAndStatusAndType(UUID farm, String search, CustomerStatus status, CustomerType type, Pageable pageable);
    Page<Customer> findAllByFarm_UuidAndNameContainingIgnoreCaseAndStatus(UUID farm, String search, CustomerStatus status, Pageable pageable);
    Page<Customer> findAllByFarm_UuidAndNameContainingIgnoreCaseAndType(UUID farm, String search, CustomerType type, Pageable pageable);
    Page<Customer> findAllByFarm_UuidAndNameContainingIgnoreCase(UUID farm, String search, Pageable pageable);
    java.util.Optional<Customer> findByUuidAndFarm_Uuid(UUID uuid, UUID farm);
    long countByFarm_UuidAndStatus(UUID farm, CustomerStatus status);
    java.util.List<Customer> findAllByFarm_Uuid(UUID farm);
}
