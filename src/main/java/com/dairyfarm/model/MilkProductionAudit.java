package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "milk_production_audits")
public class MilkProductionAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "production_uuid", nullable = false)
    private MilkProduction production;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "edited_by_uuid", nullable = false)
    private AppUser editedBy;
    @Column(name = "previous_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal previousQuantity;
    @Column(name = "new_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal newQuantity;
    @Column(name = "correction_reason", nullable = false, length = 1000) private String correctionReason;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public MilkProduction getProduction() { return production; } public void setProduction(MilkProduction v) { production = v; }
    public AppUser getEditedBy() { return editedBy; } public void setEditedBy(AppUser v) { editedBy = v; }
    public BigDecimal getPreviousQuantity() { return previousQuantity; } public void setPreviousQuantity(BigDecimal v) { previousQuantity = v; }
    public BigDecimal getNewQuantity() { return newQuantity; } public void setNewQuantity(BigDecimal v) { newQuantity = v; }
    public String getCorrectionReason() { return correctionReason; } public void setCorrectionReason(String v) { correctionReason = v; }
}
