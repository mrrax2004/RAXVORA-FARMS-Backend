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
@Table(name = "milk_deliveries", uniqueConstraints = @UniqueConstraint(name = "uq_delivery_subscription_date",
        columnNames = {"subscription_uuid", "delivery_date"}), indexes = @Index(name = "idx_delivery_farm_date",
        columnList = "farm_uuid,delivery_date,status"))
public class MilkDelivery extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_uuid", nullable = false) private MilkSubscription subscription;
    @Column(name = "delivery_date", nullable = false) private LocalDate deliveryDate;
    @Column(name = "scheduled_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal scheduledQuantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_rate_uuid") private CustomerMilkRate customerMilkRate;
    @Column(name = "delivered_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal deliveredQuantity = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private MilkDeliveryStatus status = MilkDeliveryStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delivered_by_uuid") private AppUser deliveredBy;
    @Column(length = 500) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public MilkSubscription getSubscription() { return subscription; } public void setSubscription(MilkSubscription v) { subscription = v; }
    public LocalDate getDeliveryDate() { return deliveryDate; } public void setDeliveryDate(LocalDate v) { deliveryDate = v; }
    public BigDecimal getScheduledQuantity() { return scheduledQuantity; } public void setScheduledQuantity(BigDecimal v) { scheduledQuantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal v) { totalAmount = v; }
    public CustomerMilkRate getCustomerMilkRate() { return customerMilkRate; } public void setCustomerMilkRate(CustomerMilkRate v) { customerMilkRate = v; }
    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; } public void setDeliveredQuantity(BigDecimal v) { deliveredQuantity = v; }
    public MilkDeliveryStatus getStatus() { return status; } public void setStatus(MilkDeliveryStatus v) { status = v; }
    public AppUser getDeliveredBy() { return deliveredBy; } public void setDeliveredBy(AppUser v) { deliveredBy = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
