package com.dairyfarm.controller;

import com.dairyfarm.dto.AuthDtos;
import com.dairyfarm.dto.UserDtos;
import com.dairyfarm.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @PostMapping
    public AuthDtos.UserResponse create(@Valid @RequestBody UserDtos.Request request) { return users.create(request); }
    @GetMapping
    public List<AuthDtos.UserResponse> list(@RequestParam(required = false) UUID farmUuid) { return users.list(farmUuid); }
    @GetMapping("/{uuid}")
    public AuthDtos.UserResponse get(@PathVariable UUID uuid) { return users.get(uuid); }
    @PutMapping("/{uuid}")
    public AuthDtos.UserResponse update(@PathVariable UUID uuid, @Valid @RequestBody UserDtos.Request request) {
        return users.update(uuid, request);
    }
    @PatchMapping("/{uuid}/status")
    public AuthDtos.UserResponse status(@PathVariable UUID uuid, @Valid @RequestBody UserDtos.StatusRequest request) {
        return users.status(uuid, request.active());
    }
    @PatchMapping("/{uuid}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public void resetPassword(@PathVariable UUID uuid, @Valid @RequestBody UserDtos.ResetPasswordRequest request) {
        users.resetPassword(uuid, request.password());
    }
}
