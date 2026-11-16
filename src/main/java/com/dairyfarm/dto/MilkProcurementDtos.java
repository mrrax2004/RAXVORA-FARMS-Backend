package com.dairyfarm.dto;

import com.dairyfarm.model.AavinAdjustmentType;
import com.dairyfarm.model.AavinSession;
import com.dairyfarm.model.AavinSettlementStatus;
import com.dairyfarm.model.AavinVerificationStatus;
import com.dairyfarm.model.MilkBuyerStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MilkProcurementDtos {
    private MilkProcurementDtos() {}

    public record BuyerRequest(@NotBlank @Size(max=160) String companyName,
            @NotBlank @Size(max=160) String centreName, @NotBlank @Size(max=60) String centreCode,
            @Size(max=120) String contactPerson, @Size(max=40) String phoneNumber,
            @Min(1) @jakarta.validation.constraints.Max(90) Integer paymentCycleDays, @NotNull MilkBuyerStatus status) {}
    public record BuyerResponse(UUID uuid, String type, String companyName, String centreName, String centreCode,
            String contactPerson, String phoneNumber, int paymentCycleDays, MilkBuyerStatus status) {}

    public record RateSlabRequest(@NotNull @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal fatPercentage,
            @NotNull @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal snfPercentage,
            @NotNull @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal ratePerLitre) {}
    public record RateChartRequest(@NotNull UUID buyerUuid, @NotBlank @Size(max=160) String name,
            @NotNull LocalDate effectiveFrom, LocalDate effectiveUntil, @Size(max=1500) String notes,
            @NotEmpty List<@Valid RateSlabRequest> slabs) {}
    public record RateChartPeriodRequest(@NotNull LocalDate effectiveUntil, @NotBlank @Size(max=1000) String reason) {}
    public record RateChartAuditResponse(UUID uuid, LocalDate previousEffectiveUntil, LocalDate newEffectiveUntil,
            String reason, String changedBy, Instant createdAt) {}
    public record RateSlabResponse(UUID uuid, BigDecimal fatPercentage, BigDecimal snfPercentage, BigDecimal ratePerLitre) {}
    public record RateChartResponse(UUID uuid, UUID buyerUuid, String centreName, String name,
            LocalDate effectiveFrom, LocalDate effectiveUntil, String notes, String createdBy,
            Instant createdAt, List<RateSlabResponse> slabs) {}

    public record SupplyRequest(@NotNull UUID buyerUuid, @NotNull LocalDate supplyDate, @NotNull AavinSession session,
            @NotNull @Positive @Digits(integer=8,fraction=2) BigDecimal quantity,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal farmFat,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal farmSnf,
            @PositiveOrZero @Digits(integer=4,fraction=3) BigDecimal density,
            @Size(max=100) String sampleReference, @Size(max=100) String receiptNumber, @Size(max=1500) String notes,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal buyerFat,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal buyerSnf,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal acceptedQuantity,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal rejectedQuantity,
            @Size(max=1000) String rejectionReason) {
        public SupplyRequest(UUID buyerUuid, LocalDate supplyDate, AavinSession session, BigDecimal quantity,
                BigDecimal farmFat, BigDecimal farmSnf, BigDecimal density, String sampleReference,
                String receiptNumber, String notes) {
            this(buyerUuid, supplyDate, session, quantity, farmFat, farmSnf, density, sampleReference,
                    receiptNumber, notes, null, null, null, null, null);
        }
    }
    public record SupplyVerificationRequest(@NotNull AavinVerificationStatus status,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal aavinFat,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal aavinSnf,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal aavinRate,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal acceptedQuantity,
            @Size(max=100) String receiptNumber, @NotBlank @Size(max=1000) String reason) {}
    public record SupplyRateOverrideRequest(@NotNull @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal ratePerLitre,
            @NotBlank @Size(max=1000) String reason) {}
    public record SupplyResponse(UUID uuid, UUID buyerUuid, String companyName, String centreName,
            LocalDate supplyDate, AavinSession session, BigDecimal quantity, BigDecimal farmFat, BigDecimal farmSnf,
            BigDecimal density, String sampleReference, String receiptNumber, BigDecimal aavinFat, BigDecimal aavinSnf,
            BigDecimal fatDifference, BigDecimal snfDifference, BigDecimal acceptedQuantity,
            BigDecimal rejectedQuantity, String rejectionReason, BigDecimal matchedRate,
            BigDecimal manualRate, BigDecimal aavinRate, BigDecimal applicableRate, BigDecimal estimatedValue,
            BigDecimal finalValue, boolean valueIsEstimate, UUID rateChartUuid, String rateChartName,
            AavinVerificationStatus status, String notes, String verificationNotes, UUID settlementUuid,
            Instant createdAt) {}
    public record SupplyAuditResponse(UUID uuid, String reason, BigDecimal previousRate, BigDecimal newRate,
            String previousStatus, String newStatus, String changedBy, Instant createdAt) {}

    public record SettlementRequest(@NotNull UUID buyerUuid, @NotBlank @Size(max=60) String settlementNumber,
            @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) {}
    public record AdjustmentRequest(@NotNull AavinAdjustmentType type,
            @NotNull @Digits(integer=12,fraction=2) BigDecimal amount,
            @NotBlank @Size(max=1000) String description) {}
    public record AdjustmentResponse(UUID uuid, AavinAdjustmentType type, BigDecimal amount,
            String description, String recordedBy, Instant createdAt) {}
    public record SettlementStatusRequest(@NotNull AavinSettlementStatus status, @NotBlank @Size(max=1000) String reason) {}
    public record PaymentRequest(@NotNull LocalDate paymentDate, @NotNull @Positive @Digits(integer=12,fraction=2) BigDecimal amount,
            @Size(max=160) String paymentReference, @Size(max=1000) String notes) {}
    public record PaymentResponse(UUID uuid, LocalDate paymentDate, BigDecimal amount,
            String paymentReference, String notes, String recordedBy, Instant createdAt) {}
    public record SettlementItemResponse(UUID supplyUuid, LocalDate supplyDate, AavinSession session,
            BigDecimal quantity, BigDecimal acceptedQuantity, BigDecimal appliedRate, BigDecimal milkValue,
            String receiptNumber) {}
    public record SettlementResponse(UUID uuid, UUID buyerUuid, String centreName, String settlementNumber,
            LocalDate periodStart, LocalDate periodEnd, BigDecimal totalQuantity, BigDecimal totalAcceptedQuantity,
            BigDecimal grossMilkValue, BigDecimal deductions, BigDecimal bonuses, BigDecimal adjustments,
            BigDecimal netPayable, BigDecimal amountReceived, BigDecimal remainingBalance,
            AavinSettlementStatus status, String statusReason, List<SettlementItemResponse> supplies,
            List<AdjustmentResponse> adjustmentItems, List<PaymentResponse> payments, Instant createdAt) {}
    public record SettlementStateRequest(@NotNull Boolean disputed, @Size(max=1000) String reason) {}

    public record DashboardResponse(LocalDate date, BigDecimal todaySupply, BigDecimal averageFat,
            BigDecimal averageSnf, BigDecimal estimatedValue, BigDecimal currentCycleAmount,
            LocalDate nextExpectedSettlement, BigDecimal pendingPayment, LocalDate lastPaymentDate) {}
    public record ReportRow(LocalDate date, String category, String centre, String session,
            BigDecimal quantity, BigDecimal fat, BigDecimal snf, BigDecimal rate, BigDecimal amount,
            String status, String reference) {}
}
