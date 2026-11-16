package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "buyer_rate_slabs", uniqueConstraints = @UniqueConstraint(name = "uq_aavin_slab_fat_snf",
        columnNames = {"chart_uuid", "fat_percentage", "snf_percentage"}))
public class AavinRateSlab extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chart_uuid", nullable = false)
    private AavinRateChart chart;
    @Column(name = "fat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal fatPercentage;
    @Column(name = "snf_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal snfPercentage;
    @Column(name = "rate_per_litre", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerLitre;

    public AavinRateChart getChart() { return chart; }
    public void setChart(AavinRateChart value) { chart = value; }
    public BigDecimal getFatPercentage() { return fatPercentage; }
    public void setFatPercentage(BigDecimal value) { fatPercentage = value; }
    public BigDecimal getSnfPercentage() { return snfPercentage; }
    public void setSnfPercentage(BigDecimal value) { snfPercentage = value; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public void setRatePerLitre(BigDecimal value) { ratePerLitre = value; }
}
