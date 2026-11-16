package com.dairyfarm.dto;

import com.dairyfarm.model.BreedingMethod;
import com.dairyfarm.model.CalfHealthStatus;
import com.dairyfarm.model.DeliveryType;
import com.dairyfarm.model.Gender;
import com.dairyfarm.model.ObservationPeriod;
import com.dairyfarm.model.PregnancyResult;
import com.dairyfarm.model.PregnancyTestType;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderPriority;
import com.dairyfarm.model.ReminderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PhaseTwoDtos {
    private PhaseTwoDtos() {}

    public record HealthRequest(@NotNull UUID animalUuid, @Size(max = 160) String diseaseName,
            @Size(max = 1500) String symptoms, @Size(max = 1500) String diagnosis,
            @Size(max = 2000) String treatmentDetails, @Size(max = 160) String medicineName,
            UUID medicineItemUuid, @Positive @Digits(integer = 11, fraction = 3) BigDecimal medicineQuantity,
            @Size(max = 200) String dosage, @Size(max = 200) String frequency,
            LocalDate treatmentStartDate, LocalDate treatmentEndDate,
            @Size(max = 160) String veterinarianName, @Size(max = 80) String veterinarianContact,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal medicalExpense,
            LocalDate followUpDate, @Size(max = 2000) String notes) {}
    public record AttachmentResponse(UUID uuid, String originalName, String mediaType, long fileSize, String downloadUrl) {}
    public record HealthResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            String diseaseName, String symptoms, String diagnosis, String treatmentDetails, String medicineName,
            UUID medicineItemUuid, BigDecimal medicineQuantity,
            String dosage, String frequency, LocalDate treatmentStartDate, LocalDate treatmentEndDate,
            String veterinarianName, String veterinarianContact, BigDecimal medicalExpense, LocalDate followUpDate,
            String notes, List<AttachmentResponse> attachments, Instant createdAt, Instant updatedAt) {}

    public record VaccinationRequest(@NotNull UUID animalUuid, @NotBlank @Size(max = 160) String vaccineName,
            @NotNull LocalDate vaccinationDate, LocalDate nextVaccinationDate,
            @Size(max = 200) String dose, @Size(max = 160) String administeredBy, @Size(max = 1500) String notes,
            UUID medicineItemUuid, @Positive @Digits(integer = 11, fraction = 3) BigDecimal medicineQuantity) {}
    public record VaccinationResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            String vaccineName, LocalDate vaccinationDate, LocalDate nextVaccinationDate, String dose,
            String administeredBy, String notes, UUID medicineItemUuid, BigDecimal medicineQuantity,
            Instant createdAt, Instant updatedAt) {}

    public record HeatRequest(@NotNull UUID animalUuid, @NotNull LocalDate detectionDate,
            @NotBlank @Size(max = 1500) String symptoms, @NotNull ObservationPeriod observationPeriod,
            @Size(max = 160) String observedBy, @Size(max = 1500) String notes) {}
    public record HeatResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            LocalDate detectionDate, String symptoms, ObservationPeriod observationPeriod, String observedBy,
            String notes, Instant createdAt, Instant updatedAt) {}

    public record BreedingRequest(@NotNull UUID animalUuid, @NotNull LocalDate breedingDate,
            @NotNull BreedingMethod method, UUID bullUuid, @Size(max = 160) String semenIdentification,
            @Size(max = 160) String technicianName, @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal cost,
            @Size(max = 1500) String notes) {}
    public record BreedingResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            LocalDate breedingDate, BreedingMethod method, UUID bullUuid, String bullTag, String semenIdentification,
            String technicianName, BigDecimal cost, String notes, Instant createdAt, Instant updatedAt) {}

    public record PregnancyRequest(@NotNull UUID animalUuid, @NotNull LocalDate examDate,
            @NotNull PregnancyTestType testType, @NotNull PregnancyResult result,
            @Size(max = 160) String veterinarianName, @Size(max = 1500) String notes) {}
    public record PregnancyResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            LocalDate examDate, PregnancyTestType testType, PregnancyResult result, String veterinarianName,
            String notes, LocalDate expectedDeliveryDate, Instant createdAt, Instant updatedAt) {}

    public record CalfRequest(UUID birthUuid, @NotNull Gender gender,
            @Positive @Digits(integer = 5, fraction = 2) BigDecimal birthWeight,
            @NotNull CalfHealthStatus healthStatus, boolean stillborn, @Size(max = 1000) String notes) {}
    public record CalvingRequest(@NotNull UUID motherUuid, @NotNull LocalDate actualDeliveryDate,
            @NotNull DeliveryType deliveryType, @PositiveOrZero int calfCount,
            @Size(max = 160) String veterinarianName, @Size(max = 2000) String notes,
            @Valid List<@Valid CalfRequest> calves) {}
    public record CalfResponse(UUID birthUuid, UUID calfUuid, String calfTag, Gender gender, BigDecimal birthWeight,
            CalfHealthStatus healthStatus, boolean stillborn, String notes) {}
    public record CalvingResponse(UUID uuid, UUID farmUuid, UUID motherUuid, String motherTag, String motherName,
            LocalDate actualDeliveryDate, DeliveryType deliveryType, int calfCount, String veterinarianName,
            String notes, List<CalfResponse> calves, Instant createdAt, Instant updatedAt) {}

    public record ReminderRequest(@NotNull UUID animalUuid, @NotBlank @Size(max = 180) String title,
            @NotNull LocalDateTime dueAt, @NotNull ReminderCategory category,
            @Size(max = 1500) String description, @NotNull ReminderPriority priority) {}
    public record ReminderStatusRequest(@NotNull ReminderStatus status) {}
    public record ReminderResponse(UUID uuid, UUID farmUuid, UUID animalUuid, String animalTag, String animalName,
            String title, LocalDateTime dueAt, ReminderCategory category, String description, ReminderStatus status,
            ReminderPriority priority, boolean automatic, Instant createdAt, Instant updatedAt) {}
    public record NotificationResponse(UUID uuid, boolean read, Instant createdAt, ReminderResponse reminder) {}
    public record UnreadCount(long unreadCount) {}
}
