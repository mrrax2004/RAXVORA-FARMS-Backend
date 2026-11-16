package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "buyer_rate_charts", indexes = @Index(name = "idx_aavin_chart_period",
        columnList = "farm_uuid,buyer_uuid,effective_from,effective_until"))
public class AavinRateChart extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_uuid", nullable = false)
    private MilkBuyer buyer;
    @Column(name = "chart_name", nullable = false, length = 160)
    private String name;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;
    @Column(length = 1500)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    private AppUser createdBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyer getBuyer() { return buyer; }
    public void setBuyer(MilkBuyer value) { buyer = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate value) { effectiveFrom = value; }
    public LocalDate getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDate value) { effectiveUntil = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser value) { createdBy = value; }
}
