package com.dairyfarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class BreedDtos {
    private BreedDtos() {}
    public record Request(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
    public record Response(UUID uuid, UUID farmUuid, String name, String description, boolean active,
                           Instant createdAt, Instant updatedAt) {}
    public record StatusRequest(boolean active) {}
}
