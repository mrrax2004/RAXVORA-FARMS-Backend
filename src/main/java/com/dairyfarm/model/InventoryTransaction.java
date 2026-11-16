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
import java.util.UUID;

@Entity
@Table(name = "farm_inventory_transactions")
public class InventoryTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_uuid", nullable = false)
    private InventoryItem item;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 24)
    private InventoryTransactionType type;
    @Column(name = "quantity_change", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityChange;
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Column(name = "reference_uuid")
    private UUID referenceUuid;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_uuid")
    private Animal animal;
    @Column(length = 1000)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem value) { item = value; }
    public InventoryTransactionType getType() { return type; }
    public void setType(InventoryTransactionType value) { type = value; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public void setQuantityChange(BigDecimal value) { quantityChange = value; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal value) { unitCost = value; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate value) { transactionDate = value; }
    public UUID getReferenceUuid() { return referenceUuid; }
    public void setReferenceUuid(UUID value) { referenceUuid = value; }
    public Animal getAnimal() { return animal; }
    public void setAnimal(Animal value) { animal = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser value) { recordedBy = value; }
}
