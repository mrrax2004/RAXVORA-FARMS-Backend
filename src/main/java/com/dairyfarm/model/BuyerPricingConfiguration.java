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
@Table(name = "buyer_pricing_configurations",
        indexes = @Index(name = "idx_buyer_pricing_effective", columnList = "farm_uuid,company_uuid,effective_from,effective_until"))
public class BuyerPricingConfiguration extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_uuid", nullable = false)
    private MilkBuyerCompany company;
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_method", nullable = false, length = 32)
    private BuyerPricingMethod pricingMethod;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;
    @Column(name = "base_fat_percentage", precision = 5, scale = 2)
    private BigDecimal baseFatPercentage;
    @Column(name = "base_rate", precision = 10, scale = 2)
    private BigDecimal baseRate;
    @Column(name = "fat_rate_per_percentage", precision = 10, scale = 4)
    private BigDecimal fatRatePerPercentage;
    @Column(name = "snf_rate_per_percentage", precision = 10, scale = 4)
    private BigDecimal snfRatePerPercentage;
    @Column(name = "fixed_rate_per_litre", precision = 10, scale = 2)
    private BigDecimal fixedRatePerLitre;
    @Column(length = 1500)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    private AppUser createdBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyerCompany getCompany() { return company; }
    public void setCompany(MilkBuyerCompany value) { company = value; }
    public BuyerPricingMethod getPricingMethod() { return pricingMethod; }
    public void setPricingMethod(BuyerPricingMethod value) { pricingMethod = value; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate value) { effectiveFrom = value; }
    public LocalDate getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDate value) { effectiveUntil = value; }
    public BigDecimal getBaseFatPercentage() { return baseFatPercentage; }
    public void setBaseFatPercentage(BigDecimal value) { baseFatPercentage = value; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal value) { baseRate = value; }
    public BigDecimal getFatRatePerPercentage() { return fatRatePerPercentage; }
    public void setFatRatePerPercentage(BigDecimal value) { fatRatePerPercentage = value; }
    public BigDecimal getSnfRatePerPercentage() { return snfRatePerPercentage; }
    public void setSnfRatePerPercentage(BigDecimal value) { snfRatePerPercentage = value; }
    public BigDecimal getFixedRatePerLitre() { return fixedRatePerLitre; }
    public void setFixedRatePerLitre(BigDecimal value) { fixedRatePerLitre = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser value) { createdBy = value; }
}
