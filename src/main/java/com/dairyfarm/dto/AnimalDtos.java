package com.dairyfarm.dto;

import com.dairyfarm.model.AnimalStatus;
import com.dairyfarm.model.AnimalType;
import com.dairyfarm.model.Gender;
import com.dairyfarm.model.PregnancyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AnimalDtos {
    private AnimalDtos() {}
    public record Request(@NotBlank @Size(max = 60) String tagNumber, @Size(max = 120) String name,
                          @NotNull AnimalType type, @NotNull Gender gender, UUID breedUuid,
                          @PastOrPresent LocalDate dateOfBirth, @Size(max = 80) String colour,
                          @PastOrPresent LocalDate purchaseDate,
                          @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice,
                          String purchaseSource, AnimalStatus status,
                          @Size(max = 500) String photoUrl, @Size(max = 2000) String notes) {}
    public record StatusRequest(@NotNull AnimalStatus status) {}
    public record Response(UUID uuid, UUID farmUuid, String tagNumber, String name, AnimalType type,
                           Gender gender, UUID breedUuid, String breedName, LocalDate dateOfBirth, String colour,
                           LocalDate purchaseDate, BigDecimal purchasePrice, String purchaseSource,
                           AnimalStatus status, String photoUrl, String notes, UUID motherUuid,
                           PregnancyStatus pregnancyStatus, LocalDate expectedDeliveryDate, boolean lactating,
                           Instant createdAt, Instant updatedAt) {}
}
