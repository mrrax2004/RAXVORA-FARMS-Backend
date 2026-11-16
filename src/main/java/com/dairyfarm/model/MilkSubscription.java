package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "milk_subscriptions")
public class MilkSubscription extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_uuid", nullable = false) private Customer customer;
    @Column(name = "daily_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal dailyQuantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private MilkingSession session;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_uuid", nullable = false) private AppUser createdBy;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public Customer getCustomer() { return customer; } public void setCustomer(Customer v) { customer = v; }
    public BigDecimal getDailyQuantity() { return dailyQuantity; } public void setDailyQuantity(BigDecimal v) { dailyQuantity = v; }
    public MilkingSession getSession() { return session; } public void setSession(MilkingSession v) { session = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public SubscriptionStatus getStatus() { return status; } public void setStatus(SubscriptionStatus v) { status = v; }
    public AppUser getCreatedBy() { return createdBy; } public void setCreatedBy(AppUser v) { createdBy = v; }
}
