package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "milk_supply_quality_tests")
public class AavinMilkTest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_uuid", nullable = false, unique = true)
    private AavinMilkSupply supply;
    @Column(name = "farm_fat", precision = 5, scale = 2)
    private BigDecimal farmFat;
    @Column(name = "farm_snf", precision = 5, scale = 2)
    private BigDecimal farmSnf;
    @Column(name = "density", precision = 7, scale = 3)
    private BigDecimal density;
    @Column(name = "sample_reference", length = 100)
    private String sampleReference;
    @Column(name = "aavin_fat", precision = 5, scale = 2)
    private BigDecimal aavinFat;
    @Column(name = "aavin_snf", precision = 5, scale = 2)
    private BigDecimal aavinSnf;
    @Column(name = "aavin_rate", precision = 10, scale = 2)
    private BigDecimal aavinRate;
    @Column(name = "accepted_quantity", precision = 10, scale = 2)
    private BigDecimal acceptedQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_chart_uuid")
    private AavinRateChart rateChart;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_slab_uuid")
    private AavinRateSlab rateSlab;
    @Column(name = "matched_rate", precision = 10, scale = 2)
    private BigDecimal matchedRate;
    @Column(name = "manual_rate", precision = 10, scale = 2)
    private BigDecimal manualRate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AavinVerificationStatus status;
    @Column(name = "verification_notes", length = 1500)
    private String verificationNotes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verified_by_uuid", nullable = false)
    private AppUser verifiedBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public AavinMilkSupply getSupply() { return supply; }
    public void setSupply(AavinMilkSupply value) { supply = value; }
    public BigDecimal getFarmFat() { return farmFat; }
    public void setFarmFat(BigDecimal value) { farmFat = value; }
    public BigDecimal getFarmSnf() { return farmSnf; }
    public void setFarmSnf(BigDecimal value) { farmSnf = value; }
    public BigDecimal getDensity() { return density; }
    public void setDensity(BigDecimal value) { density = value; }
    public String getSampleReference() { return sampleReference; }
    public void setSampleReference(String value) { sampleReference = value; }
    public BigDecimal getAavinFat() { return aavinFat; }
    public void setAavinFat(BigDecimal value) { aavinFat = value; }
    public BigDecimal getAavinSnf() { return aavinSnf; }
    public void setAavinSnf(BigDecimal value) { aavinSnf = value; }
    public BigDecimal getAavinRate() { return aavinRate; }
    public void setAavinRate(BigDecimal value) { aavinRate = value; }
    public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(BigDecimal value) { acceptedQuantity = value; }
    public AavinRateChart getRateChart() { return rateChart; }
    public void setRateChart(AavinRateChart value) { rateChart = value; }
    public AavinRateSlab getRateSlab() { return rateSlab; }
    public void setRateSlab(AavinRateSlab value) { rateSlab = value; }
    public BigDecimal getMatchedRate() { return matchedRate; }
    public void setMatchedRate(BigDecimal value) { matchedRate = value; }
    public BigDecimal getManualRate() { return manualRate; }
    public void setManualRate(BigDecimal value) { manualRate = value; }
    public AavinVerificationStatus getStatus() { return status; }
    public void setStatus(AavinVerificationStatus value) { status = value; }
    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String value) { verificationNotes = value; }
    public AppUser getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(AppUser value) { verifiedBy = value; }
}
