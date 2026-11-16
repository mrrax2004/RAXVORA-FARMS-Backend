package com.dairyfarm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class FarmDtos {
    private FarmDtos() {}
    public record Request(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 120) String ownerName,
                          String phone, @Email String email, String address, String city, String district,
                          String state, String pinCode) {}
    public record Response(UUID uuid, String name, String ownerName, String phone, String email,
                           String address, String city, String district, String state, String pinCode,
                           Instant createdAt, Instant updatedAt) {}
}
