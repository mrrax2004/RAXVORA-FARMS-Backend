package com.dairyfarm.dto;

import com.dairyfarm.model.MobileSyncType;
import com.dairyfarm.model.MilkingSession;
import com.dairyfarm.model.RationTargetGroup;
import com.dairyfarm.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MobileDtos {
    private MobileDtos() {}
    public record SyncRequest(@NotNull UUID deviceUuid, @NotEmpty @Size(max=100) List<@Valid SyncItem> records) {}
    public record SyncItem(@NotNull UUID clientRecordUuid, @NotNull MobileSyncType type,
            @NotNull LocalDate date, UUID animalUuid, UUID itemUuid, MilkingSession session,
            @Positive @Digits(integer=11,fraction=3) BigDecimal quantity, RationTargetGroup targetGroup,
            @Size(max=1500) String notes, UUID createdByUuid, UUID customerUuid, String invoiceNumber,
            String saleTime, PaymentMethod paymentMethod,
            @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal amountReceived,
            @Size(max=160) String transactionReference,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal clientEstimatedRate,
            @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal clientEstimatedAmount,
            UUID buyerUuid,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal farmFat,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal farmSnf,
            @PositiveOrZero @Digits(integer=4,fraction=3) BigDecimal density,
            @Size(max=100) String sampleReference, @Size(max=100) String receiptNumber,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal buyerFat,
            @PositiveOrZero @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal buyerSnf,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal acceptedQuantity,
            @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal rejectedQuantity,
            @Size(max=1000) String rejectionReason) {}
    public record SyncResult(UUID clientRecordUuid, UUID serverRecordUuid, String status, String message) {}
    public record SyncResponse(List<SyncResult> results) {}
    public record MobileAnimal(UUID uuid, String tagNumber, String name, String type, boolean lactating) {}
    public record MobileCustomer(UUID uuid, String name, String mobileNumber, String status, BigDecimal milkRate, String rateSource,
            BigDecimal outstandingBalance,
            LocalDate rateAsOf, LocalDate rateEffectiveFrom, LocalDate rateEffectiveUntil) {}
    public record MobileCompany(UUID uuid, String companyName, String companyCode, String status) {}
    public record MobileCentre(UUID uuid, UUID legacyBuyerUuid, UUID companyUuid, String companyName,
            String centreName, String centreCode, String status) {}
    public record MobilePricing(UUID uuid, UUID companyUuid, String pricingMethod, LocalDate effectiveFrom,
            LocalDate effectiveUntil, BigDecimal baseFatPercentage, BigDecimal baseRate,
            BigDecimal fatRatePerPercentage, BigDecimal snfRatePerPercentage, BigDecimal fixedRatePerLitre) {}
    public record MobileRateSlab(BigDecimal fatPercentage, BigDecimal snfPercentage, BigDecimal ratePerLitre) {}
    public record MobileRateChart(UUID uuid, UUID buyerUuid, LocalDate effectiveFrom, LocalDate effectiveUntil,
            java.util.List<MobileRateSlab> slabs) {}
    public record SnapshotResponse(UUID farmUuid, List<MobileAnimal> animals,
            List<InventoryDtos.ItemResponse> feedItems, LocalDate snapshotDate, BigDecimal defaultMilkRate,
            List<MobileCustomer> customers, List<MobileCompany> companies, List<MobileCentre> centres,
            List<MobilePricing> pricing, List<MobileRateChart> rateCharts) {}
    public record DashboardResponse(LocalDate date, BigDecimal todayProduction, BigDecimal todaySold,
            BigDecimal todayCompanySupply, BigDecimal availableStock, BigDecimal salesRevenue,
            BigDecimal companySupplyValue, BigDecimal cashCollected, BigDecimal creditSales,
            long pendingRateVerification, BigDecimal customerOutstanding) {}
}
