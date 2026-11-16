package com.dairyfarm.dto;

import com.dairyfarm.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}
    public record Request(@NotBlank @Size(max = 60) String username, @Size(min = 10, max = 72) String password,
                          @NotBlank @Size(max = 120) String fullName, UUID farmUuid,
                          @NotEmpty Set<UserRole> roles) {}
    public record StatusRequest(boolean active) {}
    public record ResetPasswordRequest(@NotBlank @Size(min = 10, max = 72) String password) {}
}
