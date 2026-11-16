package com.dairyfarm.repository;

import com.dairyfarm.model.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    List<AppUser> findAllByFarm_Uuid(UUID farmUuid);
    List<AppUser> findAllByFarm_UuidAndActiveTrue(UUID farmUuid);
}
