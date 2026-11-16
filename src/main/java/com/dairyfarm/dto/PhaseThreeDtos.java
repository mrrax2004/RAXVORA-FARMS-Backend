package com.dairyfarm.dto;

import com.dairyfarm.model.CustomerStatus;
import com.dairyfarm.model.CustomerMilkRateStatus;
import com.dairyfarm.model.MilkBillingFrequency;
import com.dairyfarm.model.CustomerType;
import com.dairyfarm.model.MilkDeliveryStatus;
import com.dairyfarm.model.MilkStockTransactionType;
import com.dairyfarm.model.MilkingSession;
import com.dairyfarm.model.PaymentMethod;
import com.dairyfarm.model.PaymentStatus;
import com.dairyfarm.model.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PhaseThreeDtos {
    private PhaseThreeDtos() {}
    public record ProductionRequest(@NotNull UUID animalUuid, @NotNull LocalDate productionDate,
            @NotNull MilkingSession session, @NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal quantity,
            @Size(max=1500) String notes) {}
    public record BatchProductionItem(@NotNull UUID animalUuid,
            @NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal quantity,
            @Size(max=1500) String notes) {}
    public record BatchProductionRequest(@NotNull LocalDate productionDate, @NotNull MilkingSession session,
            @jakarta.validation.constraints.NotEmpty java.util.List<@jakarta.validation.Valid BatchProductionItem> items) {}
    public record ProductionCorrection(@NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal quantity,
            @NotBlank @Size(max=1000) String correctionReason) {}
    public record ProductionResponse(UUID uuid, UUID animalUuid, String animalTag, String animalName,
            LocalDate productionDate, MilkingSession session, BigDecimal quantity, String recordedBy,
            String notes, Instant createdAt, Instant updatedAt) {}
    public record ProductionAuditResponse(UUID uuid, BigDecimal previousQuantity, BigDecimal newQuantity,
            String correctionReason, String editedBy, Instant createdAt) {}
    public record DailyProduction(LocalDate date, BigDecimal morning, BigDecimal afternoon, BigDecimal evening, BigDecimal total) {}

    public record CollectionResponse(UUID uuid, LocalDate collectionDate, MilkingSession session,
            BigDecimal totalQuantity, BigDecimal acceptedQuantity, BigDecimal rejectedQuantity, String rejectionReason,
            String qualityStatus, String storageLocation, String recordedBy, Instant createdAt) {}
    public record StockAdjustment(@NotNull @Positive @Digits(integer=8, fraction=2) BigDecimal quantity,
            @NotBlank @Size(max=500) String note) {}
    public record StockResponse(BigDecimal availableQuantity) {}
    public record StockTransactionResponse(UUID uuid, MilkStockTransactionType type, BigDecimal quantityChange,
            UUID collectionUuid, UUID productionUuid, UUID saleUuid, UUID deliveryUuid, UUID supplyUuid,
            String referenceNote, String recordedBy, Instant createdAt) {}

    public record CustomerRequest(@NotBlank @Size(max=160) String name, @Size(max=40) String mobileNumber,
            @Size(max=1000) String address, @NotNull CustomerType type, @NotNull LocalDate startDate,
            @NotNull CustomerStatus status, @Size(max=1500) String notes, MilkBillingFrequency billingFrequency) {}
    public record CustomerResponse(UUID uuid, String name, String mobileNumber, String address, CustomerType type,
            LocalDate startDate, CustomerStatus status, BigDecimal outstandingBalance, String notes,
            Instant createdAt, Instant updatedAt, BigDecimal currentMilkRate, LocalDate rateEffectiveFrom,
            LocalDate rateEffectiveUntil, MilkBillingFrequency billingFrequency) {}

    public record CustomerMilkRateRequest(@NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal ratePerLitre,
            @NotNull LocalDate effectiveFrom, LocalDate effectiveUntil, @NotNull CustomerMilkRateStatus status,
            @Size(max=1500) String notes) {}
    public record CustomerMilkRateStatusRequest(@NotNull CustomerMilkRateStatus status) {}
    public record CustomerMilkRateResponse(UUID uuid, UUID customerUuid, String customerName, BigDecimal ratePerLitre,
            LocalDate effectiveFrom, LocalDate effectiveUntil, CustomerMilkRateStatus status, String notes,
            String createdBy, Instant createdAt, Instant updatedAt) {}
    public record MilkRateQuoteResponse(UUID customerUuid, LocalDate date, BigDecimal ratePerLitre,
            String source, UUID customerRateUuid) {}
    public record DefaultMilkRateRequest(@NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal ratePerLitre) {}
    public record DefaultMilkRateResponse(BigDecimal ratePerLitre) {}

    public record SaleRequest(@NotBlank @Size(max=60) String invoiceNumber, @NotNull UUID customerUuid,
            @NotNull LocalDate saleDate, @NotNull @Positive @Digits(integer=8, fraction=2) BigDecimal quantity,
            @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal unitPrice,
            @NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal discount,
            @NotNull PaymentMethod paymentMethod, @PositiveOrZero @Digits(integer=10, fraction=2) BigDecimal initialPayment,
            @Size(max=1500) String notes, @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal overrideRate,
            @Size(max=1000) String overrideReason, java.time.LocalTime saleTime) {
        public SaleRequest(String invoiceNumber, UUID customerUuid, LocalDate saleDate, BigDecimal quantity,
                BigDecimal unitPrice, BigDecimal discount, PaymentMethod paymentMethod, BigDecimal initialPayment,
                String notes, BigDecimal overrideRate, String overrideReason) {
            this(invoiceNumber, customerUuid, saleDate, quantity, unitPrice, discount, paymentMethod,
                    initialPayment, notes, overrideRate, overrideReason, null);
        }
    }
    public record SaleResponse(UUID uuid, String invoiceNumber, UUID customerUuid, String customerName,
            LocalDate saleDate, java.time.LocalTime saleTime, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal totalAmount,
            BigDecimal paidAmount, BigDecimal outstandingBalance, PaymentStatus paymentStatus, PaymentMethod paymentMethod,
            String notes, Instant createdAt, BigDecimal originalUnitPrice, String rateOverrideReason,
            String rateOverrideBy) {}

    public record SubscriptionRequest(@NotNull UUID customerUuid, @NotNull @Positive @Digits(integer=8, fraction=2) BigDecimal dailyQuantity,
            @NotNull MilkingSession session, @NotNull LocalDate startDate, LocalDate endDate,
            @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal unitPrice) {}
    public record SubscriptionStatusRequest(@NotNull SubscriptionStatus status) {}
    public record SubscriptionResponse(UUID uuid, UUID customerUuid, String customerName, BigDecimal dailyQuantity,
            MilkingSession session, LocalDate startDate, LocalDate endDate, BigDecimal unitPrice,
            SubscriptionStatus status, Instant createdAt) {}
    public record GenerateDeliveriesRequest(LocalDate date) {}
    public record DeliveryUpdate(@NotNull MilkDeliveryStatus status,
            @NotNull @PositiveOrZero @Digits(integer=8, fraction=2) BigDecimal deliveredQuantity,
            @Size(max=500) String notes) {}
    public record DeliveryResponse(UUID uuid, UUID subscriptionUuid, UUID customerUuid, String customerName,
            LocalDate deliveryDate, BigDecimal scheduledQuantity, BigDecimal deliveredQuantity,
            MilkDeliveryStatus status, String notes, BigDecimal unitPrice, BigDecimal totalAmount) {}
    public record PaymentRequest(@NotNull UUID customerUuid, UUID saleUuid, @NotNull LocalDate paymentDate,
            @NotNull @Positive @Digits(integer=10, fraction=2) BigDecimal amount, @NotNull PaymentMethod method,
            @Size(max=160) String transactionReference, @Size(max=1000) String notes, UUID billUuid) {}
    public record PaymentResponse(UUID uuid, UUID customerUuid, String customerName, UUID saleUuid,
            String invoiceNumber, LocalDate paymentDate, BigDecimal amount, PaymentMethod method,
            String transactionReference, String receivedBy, Instant createdAt, UUID billUuid) {}
    public record BillAdjustmentRequest(@NotNull @Digits(integer=10, fraction=2) BigDecimal amount,
            @NotBlank @Size(max=500) String reason) {}
    public record BillAdjustmentResponse(UUID uuid, BigDecimal amount, String reason,
            String adjustedBy, Instant createdAt) {}
    public record GenerateMilkBillRequest(@NotNull UUID customerUuid, LocalDate periodDate,
            LocalDate periodStart, LocalDate periodEnd) {}
    public record MilkBillItemResponse(UUID uuid, UUID deliveryUuid, LocalDate deliveryDate, UUID subscriptionUuid,
            MilkingSession session, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {}
    public record MilkBillResponse(UUID uuid, String invoiceNumber, UUID customerUuid, String customerName,
            MilkBillingFrequency billingFrequency, LocalDate periodStart, LocalDate periodEnd, BigDecimal totalQuantity,
            BigDecimal totalAmount, BigDecimal previousOutstanding, BigDecimal adjustmentAmount, BigDecimal amountDue,
            BigDecimal paidAmount, BigDecimal outstandingBalance, PaymentStatus paymentStatus,
            String customerMobileNumber, String generatedBy, Instant createdAt, java.util.List<MilkBillItemResponse> items,
            java.util.List<BillAdjustmentResponse> adjustments) {}
    public record DashboardResponse(LocalDate date, BigDecimal todayProduction, BigDecimal morningProduction,
            BigDecimal afternoonProduction, BigDecimal eveningProduction, BigDecimal availableStock,
            BigDecimal todaySales, BigDecimal todayRevenue, long activeCustomers, long pendingDeliveries,
            BigDecimal outstandingPayments, java.util.List<DailyProduction> productionTrend,
            java.util.List<DailySales> salesTrend) {}
    public record DailySales(LocalDate date, BigDecimal quantity, BigDecimal revenue) {}
    public record ReportRow(LocalDate date, String category, String label, BigDecimal quantity,
            BigDecimal amount, String details) {}
}
