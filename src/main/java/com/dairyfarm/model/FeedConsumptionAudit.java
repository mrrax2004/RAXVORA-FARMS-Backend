package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "feed_consumption_audits")
public class FeedConsumptionAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumption_uuid", nullable = false)
    private FeedConsumption consumption;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edited_by_uuid", nullable = false)
    private AppUser editedBy;
    @Column(name = "previous_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal previousQuantity;
    @Column(name = "new_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal newQuantity;
    @Column(name = "correction_reason", nullable = false, length = 1000)
    private String reason;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public FeedConsumption getConsumption() { return consumption; }
    public void setConsumption(FeedConsumption value) { consumption = value; }
    public AppUser getEditedBy() { return editedBy; }
    public void setEditedBy(AppUser value) { editedBy = value; }
    public BigDecimal getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(BigDecimal value) { previousQuantity = value; }
    public BigDecimal getNewQuantity() { return newQuantity; }
    public void setNewQuantity(BigDecimal value) { newQuantity = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
}
