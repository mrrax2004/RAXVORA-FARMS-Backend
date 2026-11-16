package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "buyer_rate_chart_audits")
public class AavinRateChartAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chart_uuid", nullable = false)
    private AavinRateChart chart;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_uuid", nullable = false)
    private AppUser changedBy;
    @Column(name = "change_reason", nullable = false, length = 1000)
    private String reason;
    @Column(name = "previous_effective_until")
    private LocalDate previousEffectiveUntil;
    @Column(name = "new_effective_until", nullable = false)
    private LocalDate newEffectiveUntil;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public AavinRateChart getChart() { return chart; }
    public void setChart(AavinRateChart value) { chart = value; }
    public AppUser getChangedBy() { return changedBy; }
    public void setChangedBy(AppUser value) { changedBy = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public LocalDate getPreviousEffectiveUntil() { return previousEffectiveUntil; }
    public void setPreviousEffectiveUntil(LocalDate value) { previousEffectiveUntil = value; }
    public LocalDate getNewEffectiveUntil() { return newEffectiveUntil; }
    public void setNewEffectiveUntil(LocalDate value) { newEffectiveUntil = value; }
}
