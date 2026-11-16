package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "buyer_settlements", uniqueConstraints = @UniqueConstraint(name = "uq_aavin_settlement_number",
        columnNames = {"farm_uuid", "settlement_number"}))
public class AavinSettlement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_uuid", nullable = false)
    private MilkBuyer buyer;
    @Column(name = "settlement_number", nullable = false, length = 60)
    private String settlementNumber;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "total_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalQuantity;
    @Column(name = "total_accepted_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAcceptedQuantity;
    @Column(name = "gross_milk_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossMilkValue;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deductions;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal bonuses;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal adjustments;
    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AavinSettlementStatus status = AavinSettlementStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    private AppUser createdBy;
    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyer getBuyer() { return buyer; }
    public void setBuyer(MilkBuyer value) { buyer = value; }
    public String getSettlementNumber() { return settlementNumber; }
    public void setSettlementNumber(String value) { settlementNumber = value; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal value) { totalQuantity = value; }
    public BigDecimal getTotalAcceptedQuantity() { return totalAcceptedQuantity; }
    public void setTotalAcceptedQuantity(BigDecimal value) { totalAcceptedQuantity = value; }
    public BigDecimal getGrossMilkValue() { return grossMilkValue; }
    public void setGrossMilkValue(BigDecimal value) { grossMilkValue = value; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal value) { deductions = value; }
    public BigDecimal getBonuses() { return bonuses; }
    public void setBonuses(BigDecimal value) { bonuses = value; }
    public BigDecimal getAdjustments() { return adjustments; }
    public void setAdjustments(BigDecimal value) { adjustments = value; }
    public BigDecimal getNetPayable() { return netPayable; }
    public void setNetPayable(BigDecimal value) { netPayable = value; }
    public AavinSettlementStatus getStatus() { return status; }
    public void setStatus(AavinSettlementStatus value) { status = value; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser value) { createdBy = value; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String value) { statusReason = value; }
}
