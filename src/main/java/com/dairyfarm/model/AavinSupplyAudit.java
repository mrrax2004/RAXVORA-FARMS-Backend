package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "milk_supply_audits")
public class AavinSupplyAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_uuid", nullable = false)
    private AavinMilkSupply supply;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_uuid", nullable = false)
    private AppUser changedBy;
    @Column(name = "change_reason", nullable = false, length = 1000)
    private String reason;
    @Column(name = "previous_rate", precision = 10, scale = 2)
    private BigDecimal previousRate;
    @Column(name = "new_rate", precision = 10, scale = 2)
    private BigDecimal newRate;
    @Column(name = "previous_aavin_fat", precision = 5, scale = 2)
    private BigDecimal previousAavinFat;
    @Column(name = "new_aavin_fat", precision = 5, scale = 2)
    private BigDecimal newAavinFat;
    @Column(name = "previous_aavin_snf", precision = 5, scale = 2)
    private BigDecimal previousAavinSnf;
    @Column(name = "new_aavin_snf", precision = 5, scale = 2)
    private BigDecimal newAavinSnf;
    @Column(name = "previous_accepted_quantity", precision = 10, scale = 2)
    private BigDecimal previousAcceptedQuantity;
    @Column(name = "new_accepted_quantity", precision = 10, scale = 2)
    private BigDecimal newAcceptedQuantity;
    @Column(name = "previous_status", length = 24)
    private String previousStatus;
    @Column(name = "new_status", length = 24)
    private String newStatus;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public AavinMilkSupply getSupply() { return supply; }
    public void setSupply(AavinMilkSupply value) { supply = value; }
    public AppUser getChangedBy() { return changedBy; }
    public void setChangedBy(AppUser value) { changedBy = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public BigDecimal getPreviousRate() { return previousRate; }
    public void setPreviousRate(BigDecimal value) { previousRate = value; }
    public BigDecimal getNewRate() { return newRate; }
    public void setNewRate(BigDecimal value) { newRate = value; }
    public BigDecimal getPreviousAavinFat() { return previousAavinFat; }
    public void setPreviousAavinFat(BigDecimal value) { previousAavinFat = value; }
    public BigDecimal getNewAavinFat() { return newAavinFat; }
    public void setNewAavinFat(BigDecimal value) { newAavinFat = value; }
    public BigDecimal getPreviousAavinSnf() { return previousAavinSnf; }
    public void setPreviousAavinSnf(BigDecimal value) { previousAavinSnf = value; }
    public BigDecimal getNewAavinSnf() { return newAavinSnf; }
    public void setNewAavinSnf(BigDecimal value) { newAavinSnf = value; }
    public BigDecimal getPreviousAcceptedQuantity() { return previousAcceptedQuantity; }
    public void setPreviousAcceptedQuantity(BigDecimal value) { previousAcceptedQuantity = value; }
    public BigDecimal getNewAcceptedQuantity() { return newAcceptedQuantity; }
    public void setNewAcceptedQuantity(BigDecimal value) { newAcceptedQuantity = value; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String value) { previousStatus = value; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String value) { newStatus = value; }
}
