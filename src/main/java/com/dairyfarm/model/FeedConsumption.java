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
@Table(name = "feed_consumptions")
public class FeedConsumption extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_uuid", nullable = false)
    private InventoryItem item;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_uuid")
    private Animal animal;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_group", length = 24)
    private RationTargetGroup targetGroup;
    @Column(name = "consumption_date", nullable = false)
    private LocalDate date;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;
    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;
    @Column(length = 1000)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem value) { item = value; }
    public Animal getAnimal() { return animal; }
    public void setAnimal(Animal value) { animal = value; }
    public RationTargetGroup getTargetGroup() { return targetGroup; }
    public void setTargetGroup(RationTargetGroup value) { targetGroup = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal value) { unitCost = value; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal value) { totalCost = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser value) { recordedBy = value; }
}
