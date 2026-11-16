package com.dairyfarm.dto;

import com.dairyfarm.model.BuyerPaymentCycle;
import com.dairyfarm.model.BuyerPricingMethod;
import com.dairyfarm.model.MilkBuyerCompanyType;
import com.dairyfarm.model.MilkBuyerStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ProcurementDtos {
    private ProcurementDtos() {}

    public record CompanyRequest(@NotBlank @Size(max=160) String companyName,
            @NotBlank @Size(max=60) String companyCode, @NotNull MilkBuyerCompanyType companyType,
            @Size(max=120) String contactPerson, @Size(max=40) String phoneNumber,
            @Size(max=500) String address, @Size(max=30) String gstNumber,
            @NotNull MilkBuyerStatus status, @Size(max=1500) String notes) {}
    public record CompanyResponse(UUID uuid, String companyName, String companyCode, MilkBuyerCompanyType companyType,
            String contactPerson, String phoneNumber, String address, String gstNumber, MilkBuyerStatus status,
            String notes, Instant createdAt) {}
    public record CentreRequest(@NotNull UUID companyUuid, @NotBlank @Size(max=160) String centreName,
            @NotBlank @Size(max=60) String centreCode, @Size(max=120) String contactPerson,
            @Size(max=40) String phoneNumber, @NotNull MilkBuyerStatus status) {}
    public record CentreResponse(UUID uuid, UUID legacyBuyerUuid, UUID companyUuid, String companyName,
            String centreName, String centreCode, String contactPerson, String phoneNumber,
            MilkBuyerStatus status, Instant createdAt) {}
    public record PricingRequest(@NotNull BuyerPricingMethod pricingMethod, @NotNull LocalDate effectiveFrom,
            LocalDate effectiveUntil, @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal baseFatPercentage,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal baseRate,
            @PositiveOrZero @Digits(integer=8,fraction=4) BigDecimal fatRatePerPercentage,
            @PositiveOrZero @Digits(integer=8,fraction=4) BigDecimal snfRatePerPercentage,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal fixedRatePerLitre,
            @Size(max=1500) String notes) {}
    public record PricingResponse(UUID uuid, UUID companyUuid, BuyerPricingMethod pricingMethod,
            LocalDate effectiveFrom, LocalDate effectiveUntil, BigDecimal baseFatPercentage,
            BigDecimal baseRate, BigDecimal fatRatePerPercentage, BigDecimal snfRatePerPercentage,
            BigDecimal fixedRatePerLitre, String notes, Instant createdAt) {}
    public record PaymentScheduleRequest(@NotNull BuyerPaymentCycle paymentCycle,
            @Min(1) @Max(366) Integer customPeriodDays, @Size(max=160) String periodStartRule,
            @Size(max=160) String periodEndRule, @Min(1) @Max(31) Integer expectedPaymentDay,
            @Min(0) @Max(90) int gracePeriodDays, @Size(max=1500) String notes) {}
    public record PaymentScheduleResponse(UUID uuid, UUID companyUuid, BuyerPaymentCycle paymentCycle,
            Integer customPeriodDays, String periodStartRule, String periodEndRule,
            Integer expectedPaymentDay, int gracePeriodDays, String notes, Instant updatedAt) {}
}
