package com.dairyfarm.dto;

import com.dairyfarm.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, UserResponse user) {}
    public record UserResponse(UUID uuid, String username, String fullName, boolean active,
                               UUID farmUuid, Set<UserRole> roles) {}
}
