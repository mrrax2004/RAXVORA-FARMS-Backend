package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
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
@Table(name = "milk_bills",
        uniqueConstraints = @UniqueConstraint(name = "uq_milk_bill_farm_invoice", columnNames = {"farm_uuid", "invoice_number"}),
        indexes = @Index(name = "idx_milk_bill_farm_customer_period",
                columnList = "farm_uuid,customer_uuid,period_start,period_end"))
public class MilkBill extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_uuid", nullable = false)
    private Customer customer;
    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency", nullable = false, length = 24)
    private MilkBillingFrequency billingFrequency;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "total_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuantity;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "previous_outstanding", nullable = false, precision = 12, scale = 2)
    private BigDecimal previousOutstanding = BigDecimal.ZERO;
    @Column(name = "adjustment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by_uuid", nullable = false)
    private AppUser generatedBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer value) { customer = value; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String value) { invoiceNumber = value; }
    public MilkBillingFrequency getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(MilkBillingFrequency value) { billingFrequency = value; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal value) { totalQuantity = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { totalAmount = value; }
    public BigDecimal getPreviousOutstanding() { return previousOutstanding; }
    public void setPreviousOutstanding(BigDecimal value) { previousOutstanding = value; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public void setAdjustmentAmount(BigDecimal value) { adjustmentAmount = value; }
    public AppUser getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(AppUser value) { generatedBy = value; }
}
