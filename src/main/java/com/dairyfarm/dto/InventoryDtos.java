package com.dairyfarm.dto;

import com.dairyfarm.model.InventoryItemStatus;
import com.dairyfarm.model.InventoryItemType;
import com.dairyfarm.model.InventoryTransactionType;
import com.dairyfarm.model.PaymentMethod;
import com.dairyfarm.model.RationTargetGroup;
import com.dairyfarm.model.SupplierPaymentStatus;
import com.dairyfarm.model.SupplierStatus;
import com.dairyfarm.model.SupplierType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {}
    public record SupplierRequest(@NotBlank @Size(max=160) String name, @Size(max=40) String phone,
            @Size(max=800) String address, @Size(max=30) String gstNumber, @Size(max=120) String contactPerson,
            @NotNull SupplierType type, @Size(max=1200) String notes, @NotNull SupplierStatus status) {}
    public record SupplierResponse(UUID uuid, String name, String phone, String address, String gstNumber,
            String contactPerson, SupplierType type, String notes, SupplierStatus status,
            BigDecimal purchases, BigDecimal paid, BigDecimal outstanding) {}
    public record ItemRequest(@NotBlank @Size(max=160) String name, @NotNull InventoryItemType type,
            @NotBlank @Size(max=40) String category, @NotBlank @Size(max=20) String unit,
            @NotNull @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal purchasePrice,
            @NotNull @PositiveOrZero @Digits(integer=11,fraction=3) BigDecimal minimumStock,
            UUID supplierUuid, @Size(max=80) String batchNumber, LocalDate expiryDate,
            @Size(max=1000) String notes, @NotNull InventoryItemStatus status) {}
    public record ItemResponse(UUID uuid, String name, InventoryItemType type, String category, String unit,
            BigDecimal purchasePrice, BigDecimal currentStock, BigDecimal minimumStock, UUID supplierUuid,
            String supplierName, String batchNumber, LocalDate expiryDate, String notes,
            InventoryItemStatus status, boolean lowStock, boolean expired) {}
    public record PurchaseRequest(@NotNull UUID supplierUuid, @NotNull UUID itemUuid, @NotNull LocalDate purchaseDate,
            @NotNull @Positive @Digits(integer=11,fraction=3) BigDecimal quantity,
            @NotNull @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal unitPrice,
            @NotBlank @Size(max=100) String invoiceNumber,
            @NotNull @PositiveOrZero @Digits(integer=12,fraction=2) BigDecimal initialPayment,
            @Size(max=20) String paymentMethod, @Size(max=1200) String notes) {}
    public record PurchaseResponse(UUID uuid, UUID supplierUuid, String supplierName, UUID itemUuid,
            String itemName, String unit, LocalDate purchaseDate, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal totalAmount, String invoiceNumber, SupplierPaymentStatus paymentStatus,
            BigDecimal amountPaid, BigDecimal outstanding, String paymentMethod, String notes,
            Instant createdAt) {}
    public record SupplierPaymentRequest(@NotNull LocalDate paymentDate, @NotNull @Positive
            @Digits(integer=12,fraction=2) BigDecimal amount, @NotNull PaymentMethod paymentMethod,
            @Size(max=120) String referenceNumber) {}
    public record SupplierPaymentResponse(UUID uuid, LocalDate paymentDate, BigDecimal amount,
            String paymentMethod, String referenceNumber, String recordedBy, Instant createdAt) {}
    public record ConsumptionRequest(@NotNull UUID itemUuid, UUID animalUuid, RationTargetGroup targetGroup,
            @NotNull LocalDate date, @NotNull @Positive @Digits(integer=11,fraction=3) BigDecimal quantity,
            @Size(max=1000) String notes) {}
    public record ConsumptionCorrectionRequest(@NotNull @Positive @Digits(integer=11,fraction=3) BigDecimal quantity,
            @NotBlank @Size(max=1000) String reason) {}
    public record ConsumptionResponse(UUID uuid, UUID itemUuid, String itemName, String unit, UUID animalUuid,
            String animalTag, RationTargetGroup targetGroup, LocalDate date, BigDecimal quantity,
            BigDecimal unitCost, BigDecimal totalCost, String notes, Instant createdAt) {}
    public record ConsumptionAuditResponse(UUID uuid, BigDecimal previousQuantity, BigDecimal newQuantity,
            String reason, String editedBy, Instant createdAt) {}
    public record RationLineRequest(@NotNull UUID itemUuid, @NotNull @Positive
            @Digits(integer=10,fraction=3) BigDecimal quantityPerAnimalDay) {}
    public record RationRequest(@NotBlank @Size(max=160) String name, RationTargetGroup targetGroup,
            UUID animalUuid, @NotNull LocalDate effectiveFrom, LocalDate effectiveUntil,
            @NotEmpty List<@Valid RationLineRequest> lines) {}
    public record RationLineResponse(UUID itemUuid, String itemName, String unit,
            BigDecimal quantityPerAnimalDay, BigDecimal dailyCostPerAnimal) {}
    public record RationResponse(UUID uuid, String name, RationTargetGroup targetGroup,
            UUID animalUuid, String animalTag, LocalDate effectiveFrom, LocalDate effectiveUntil,
            boolean active, int applicableAnimals, BigDecimal dailyQuantityPerAnimal,
            BigDecimal dailyCostPerAnimal, BigDecimal monthlyQuantityPerAnimal,
            BigDecimal monthlyCostPerAnimal, List<RationLineResponse> lines) {}
    public record InventoryTransactionResponse(UUID uuid, UUID itemUuid, String itemName,
            InventoryTransactionType type, BigDecimal quantityChange, String unit, BigDecimal unitCost,
            LocalDate date, UUID animalUuid, String animalTag, UUID referenceUuid, String reason,
            String recordedBy, Instant createdAt) {}
    public record AlertResponse(UUID itemUuid, String itemName, InventoryItemType type,
            BigDecimal currentStock, BigDecimal minimumStock, String unit, String alertType, LocalDate expiryDate) {}
    public record ReportRow(LocalDate date, String type, String item, String category, String animal,
            String group, BigDecimal quantity, String unit, BigDecimal unitCost, BigDecimal totalCost,
            String reference) {}
    public record SummaryResponse(BigDecimal feedValue, BigDecimal medicineValue, long activeSuppliers,
            long lowStockItems, long expiredItems, BigDecimal supplierOutstanding) {}
    public record AnimalMedicineRequest(UUID medicineItemUuid,
            @Positive @Digits(integer=11,fraction=3) BigDecimal medicineQuantity) {}
    public record StockAdjustmentRequest(@NotNull @Positive @Digits(integer=11,fraction=3) BigDecimal quantity,
            @NotNull InventoryTransactionType type, @NotBlank @Size(max=1000) String reason) {}
}
