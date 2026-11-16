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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_milk_rates", indexes = {
        @Index(name = "idx_customer_rate_period", columnList = "farm_uuid,customer_uuid,status,effective_from,effective_until")
})
public class CustomerMilkRate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_uuid", nullable = false)
    private Customer customer;
    @Column(name = "rate_per_litre", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerLitre;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustomerMilkRateStatus status = CustomerMilkRateStatus.ACTIVE;
    @Column(length = 1500)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    private AppUser createdBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer value) { customer = value; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public void setRatePerLitre(BigDecimal value) { ratePerLitre = value; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate value) { effectiveFrom = value; }
    public LocalDate getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDate value) { effectiveUntil = value; }
    public CustomerMilkRateStatus getStatus() { return status; }
    public void setStatus(CustomerMilkRateStatus value) { status = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser value) { createdBy = value; }
}
