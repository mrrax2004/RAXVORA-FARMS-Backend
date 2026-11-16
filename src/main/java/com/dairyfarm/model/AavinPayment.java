package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "buyer_payments")
public class AavinPayment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_uuid", nullable = false)
    private AavinSettlement settlement;
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "payment_reference", length = 160)
    private String paymentReference;
    @Column(length = 1000)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public AavinSettlement getSettlement() { return settlement; }
    public void setSettlement(AavinSettlement value) { settlement = value; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate value) { paymentDate = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser value) { recordedBy = value; }
}
