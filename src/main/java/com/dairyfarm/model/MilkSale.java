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
import java.time.LocalTime;

@Entity
@Table(name = "milk_sales", uniqueConstraints = @UniqueConstraint(name = "uq_milk_invoice", columnNames = {"farm_uuid", "invoice_number"}),
        indexes = @Index(name = "idx_sale_farm_date", columnList = "farm_uuid,sale_date"))
public class MilkSale extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @Column(name = "invoice_number", nullable = false, length = 60) private String invoiceNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_uuid", nullable = false) private Customer customer;
    @Column(name = "sale_date", nullable = false) private LocalDate saleDate;
    @Column(name = "sale_time") private LocalTime saleTime;
    @Column(name = "milk_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(name = "original_unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal originalUnitPrice = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_rate_uuid") private CustomerMilkRate customerMilkRate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rate_override_user_uuid") private AppUser rateOverrideBy;
    @Column(name = "rate_override_reason", length = 1000) private String rateOverrideReason;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal discount;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 12) private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 20) private PaymentMethod paymentMethod;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_uuid", nullable = false) private AppUser createdBy;
    @Column(length = 1500) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String v) { invoiceNumber = v; }
    public Customer getCustomer() { return customer; } public void setCustomer(Customer v) { customer = v; }
    public LocalDate getSaleDate() { return saleDate; } public void setSaleDate(LocalDate v) { saleDate = v; }
    public LocalTime getSaleTime() { return saleTime; } public void setSaleTime(LocalTime v) { saleTime = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice; } public void setOriginalUnitPrice(BigDecimal v) { originalUnitPrice = v; }
    public CustomerMilkRate getCustomerMilkRate() { return customerMilkRate; } public void setCustomerMilkRate(CustomerMilkRate v) { customerMilkRate = v; }
    public AppUser getRateOverrideBy() { return rateOverrideBy; } public void setRateOverrideBy(AppUser v) { rateOverrideBy = v; }
    public String getRateOverrideReason() { return rateOverrideReason; } public void setRateOverrideReason(String v) { rateOverrideReason = v; }
    public BigDecimal getDiscount() { return discount; } public void setDiscount(BigDecimal v) { discount = v; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal v) { totalAmount = v; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(PaymentStatus v) { paymentStatus = v; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(PaymentMethod v) { paymentMethod = v; }
    public AppUser getCreatedBy() { return createdBy; } public void setCreatedBy(AppUser v) { createdBy = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
