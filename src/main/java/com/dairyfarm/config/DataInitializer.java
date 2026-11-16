package com.dairyfarm.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dairyfarm.model.AppUser;
import com.dairyfarm.model.Breed;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.Role;
import com.dairyfarm.model.UserRole;
import com.dairyfarm.repository.BreedRepository;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.RoleRepository;
import com.dairyfarm.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final RoleRepository roles;
    private final UserRepository users;
    private final FarmRepository farms;
    private final BreedRepository breeds;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    @Value("${ADMIN_USERNAME:}")
    private String adminUsername;
    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;
    @Value("${ADMIN_FARM_NAME:My Dairy Farm}")
    private String farmName;
    @Value("${ADMIN_OWNER_NAME:Farm Owner}")
    private String ownerName;
    public DataInitializer(RoleRepository roles, UserRepository users, FarmRepository farms,
                           BreedRepository breeds, PasswordEncoder passwordEncoder, Environment environment) {
        this.roles = roles; this.users = users; this.farms = farms; this.breeds = breeds;
        this.passwordEncoder = passwordEncoder; this.environment = environment;
    }
    @Override
    @Transactional
    public void run(String... args) {
        String normalizedAdminUsername = adminUsername == null ? "" : adminUsername.trim();
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (production && (normalizedAdminUsername.isBlank() || adminPassword == null || adminPassword.isBlank())) {
            throw new IllegalStateException("Production requires ADMIN_USERNAME and ADMIN_PASSWORD to provision the initial administrator.");
        }
        for (UserRole role : UserRole.values()) roles.findByName(role).orElseGet(() -> roles.save(new Role(role)));
        if (normalizedAdminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("No admin account was provisioned. Set ADMIN_USERNAME and ADMIN_PASSWORD to create one.");
            return;
        }
        boolean demoCredentials = environment.acceptsProfiles(Profiles.of("demo"))
                && normalizedAdminUsername.equals("admin") && adminPassword.equals("admin");
        if (!demoCredentials && (adminPassword.length() < 10
                || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72))
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 10 characters and at most 72 UTF-8 bytes.");
        if (demoCredentials) log.warn("Using the demo-only admin/admin credentials. Do not use the demo profile in production.");
        if (users.existsByUsername(normalizedAdminUsername)) return;
        Farm farm = farms.findFirstByNameIgnoreCase(farmName).orElseGet(() -> {
            Farm created = new Farm(); created.setName(farmName); created.setOwnerName(ownerName);
            return farms.save(created);
        });
        if (breeds.findAllByFarm_Uuid(farm.getUuid()).isEmpty()) {
            for (String name : new String[]{"Jersey", "Holstein Friesian (HF)", "Gir", "Sahiwal", "Kangayam", "Murrah", "Crossbred", "Other"}) {
                Breed breed = new Breed(); breed.setFarm(farm); breed.setName(name); breeds.save(breed);
            }
        }
        AppUser admin = new AppUser();
        admin.setUsername(normalizedAdminUsername); admin.setFullName(ownerName); admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFarm(farm); admin.setRoles(Set.of(roles.findByName(UserRole.ADMIN).orElseThrow()));
        users.save(admin);
        log.info("Administrator account '{}' created for farm '{}'.", adminUsername, farmName);
    }
}
