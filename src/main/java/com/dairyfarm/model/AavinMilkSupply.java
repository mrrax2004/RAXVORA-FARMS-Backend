package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "milk_supplies", uniqueConstraints = @UniqueConstraint(name = "uq_aavin_receipt",
        columnNames = {"farm_uuid", "buyer_uuid", "receipt_number"}),
        indexes = @Index(name = "idx_aavin_supply_farm_date", columnList = "farm_uuid,supply_date,session"))
public class AavinMilkSupply extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_uuid", nullable = false)
    private MilkBuyer buyer;
    @Column(name = "supply_date", nullable = false)
    private LocalDate supplyDate;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AavinSession session;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
    @Column(name = "rejected_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;
    @Column(length = 1500)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_uuid")
    private AavinSettlement settlement;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyer getBuyer() { return buyer; }
    public void setBuyer(MilkBuyer value) { buyer = value; }
    public LocalDate getSupplyDate() { return supplyDate; }
    public void setSupplyDate(LocalDate value) { supplyDate = value; }
    public AavinSession getSession() { return session; }
    public void setSession(AavinSession value) { session = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(BigDecimal value) { rejectedQuantity = value; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String value) { rejectionReason = value; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String value) { receiptNumber = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser value) { recordedBy = value; }
    public AavinSettlement getSettlement() { return settlement; }
    public void setSettlement(AavinSettlement value) { settlement = value; }
}
