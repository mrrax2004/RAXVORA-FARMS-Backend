package com.dairyfarm.service;

import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.AppUser;
import com.dairyfarm.repository.UserRepository;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }
    public AppUser get() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found."));
    }
    public boolean isAdmin(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == com.dairyfarm.model.UserRole.ADMIN);
    }
    public UUID farmUuid() {
        AppUser user = get();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String selectedFarm = request.getHeader("X-Farm-UUID");
            if (selectedFarm != null && !selectedFarm.isBlank()) {
                UUID uuid;
                try { uuid = UUID.fromString(selectedFarm); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("X-Farm-UUID must be a valid UUID."); }
                requireFarmAccess(uuid);
                return uuid;
            }
        }
        if (user.getFarm() == null) throw new IllegalStateException("Select or assign a farm before accessing farm data.");
        return user.getFarm().getUuid();
    }
    public void requireFarmAccess(UUID farmUuid) {
        AppUser user = get();
        if (!isAdmin(user) && (user.getFarm() == null || !user.getFarm().getUuid().equals(farmUuid))) {
            throw new org.springframework.security.access.AccessDeniedException("Access to this farm is not permitted.");
        }
    }
}
