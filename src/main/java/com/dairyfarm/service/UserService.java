package com.dairyfarm.service;

import com.dairyfarm.dto.AuthDtos;
import com.dairyfarm.dto.UserDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.AppUser;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.Role;
import com.dairyfarm.model.UserRole;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.RoleRepository;
import com.dairyfarm.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final FarmRepository farms;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;
    public UserService(UserRepository users, FarmRepository farms, RoleRepository roles, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUser) {
        this.users = users; this.farms = farms; this.roles = roles;
        this.passwordEncoder = passwordEncoder; this.currentUser = currentUser;
    }
    @Transactional
    public AuthDtos.UserResponse create(UserDtos.Request request) {
        if (request.password() == null || request.password().isBlank())
            throw new IllegalArgumentException("Password is required for a new user.");
        validatePasswordBytes(request.password());
        if (users.existsByUsername(request.username().trim()))
            throw new ConflictException("This username is already in use.");
        AppUser actor = currentUser.get();
        UUID farmUuid = request.farmUuid() == null ? (actor.getFarm() == null ? null : actor.getFarm().getUuid()) : request.farmUuid();
        requireFarm(farmUuid);
        if (!currentUser.isAdmin(actor) && !farmUuid.equals(actor.getFarm().getUuid()))
            throw new AccessDeniedException("You can only add users to your own farm.");
        checkRoleAssignment(request.roles(), actor);
        AppUser user = new AppUser();
        user.setUsername(request.username().trim()); user.setFullName(request.fullName().trim());
        user.setPassword(passwordEncoder.encode(request.password())); user.setFarm(findFarm(farmUuid));
        user.setRoles(resolveRoles(request.roles()));
        return AuthService.toResponse(users.save(user));
    }
    @Transactional(readOnly = true)
    public List<AuthDtos.UserResponse> list(UUID farmUuid) {
        AppUser actor = currentUser.get();
        List<AppUser> found;
        if (currentUser.isAdmin(actor)) found = farmUuid == null ? users.findAll() : users.findAllByFarm_Uuid(farmUuid);
        else {
            UUID ownFarm = actor.getFarm() == null ? null : actor.getFarm().getUuid();
            if (ownFarm == null) return List.of();
            if (farmUuid != null && !farmUuid.equals(ownFarm)) throw new AccessDeniedException("Farm access denied.");
            found = users.findAllByFarm_Uuid(ownFarm);
        }
        return found.stream().map(AuthService::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public AuthDtos.UserResponse get(UUID uuid) { return AuthService.toResponse(find(uuid)); }
    @Transactional
    public AuthDtos.UserResponse update(UUID uuid, UserDtos.Request request) {
        AppUser user = find(uuid);
        AppUser actor = currentUser.get();
        if (!user.getUsername().equals(request.username().trim()) && users.existsByUsername(request.username().trim()))
            throw new ConflictException("This username is already in use.");
        UUID farmUuid = request.farmUuid() == null ? user.getFarm().getUuid() : request.farmUuid();
        requireFarm(farmUuid);
        if (!currentUser.isAdmin(actor) && !farmUuid.equals(actor.getFarm().getUuid()))
            throw new AccessDeniedException("You can only assign users to your own farm.");
        checkRoleAssignment(request.roles(), actor);
        user.setUsername(request.username().trim()); user.setFullName(request.fullName().trim());
        user.setFarm(findFarm(farmUuid)); user.setRoles(resolveRoles(request.roles()));
        if (request.password() != null && !request.password().isBlank()) {
            validatePasswordBytes(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return AuthService.toResponse(users.save(user));
    }
    @Transactional
    public AuthDtos.UserResponse status(UUID uuid, boolean active) {
        AppUser user = find(uuid);
        if (user.getUuid().equals(currentUser.get().getUuid()) && !active)
            throw new ConflictException("You cannot deactivate your own account.");
        user.setActive(active); return AuthService.toResponse(users.save(user));
    }
    @Transactional
    public void resetPassword(UUID uuid, String password) {
        validatePasswordBytes(password);
        AppUser user = find(uuid); user.setPassword(passwordEncoder.encode(password)); users.save(user);
    }
    private void validatePasswordBytes(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes for BCrypt.");
    }
    private AppUser find(UUID uuid) {
        AppUser user = users.findById(uuid).orElseThrow(() -> new NotFoundException("User not found."));
        AppUser actor = currentUser.get();
        if (!currentUser.isAdmin(actor) && (actor.getFarm() == null || user.getFarm() == null
                || !actor.getFarm().getUuid().equals(user.getFarm().getUuid())))
            throw new AccessDeniedException("User access denied.");
        return user;
    }
    private void requireFarm(UUID farmUuid) {
        if (farmUuid == null) throw new IllegalArgumentException("A farm is required for every user.");
    }
    private Farm findFarm(UUID uuid) { return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found.")); }
    private Set<Role> resolveRoles(Set<UserRole> names) {
        return names.stream().map(name -> roles.findByName(name)
                .orElseThrow(() -> new NotFoundException("Role not found: " + name))).collect(Collectors.toSet());
    }
    private void checkRoleAssignment(Set<UserRole> requested, AppUser actor) {
        if (!currentUser.isAdmin(actor) && requested.stream().anyMatch(role -> role == UserRole.ADMIN || role == UserRole.ACCOUNTANT))
            throw new AccessDeniedException("Only administrators can assign administrator or accountant roles.");
    }
}
