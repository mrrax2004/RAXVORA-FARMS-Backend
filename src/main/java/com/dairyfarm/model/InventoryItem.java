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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "farm_inventory_items", uniqueConstraints = @UniqueConstraint(name = "uq_inventory_farm_name_batch",
        columnNames = {"farm_uuid", "item_name", "batch_number"}),
        indexes = {@Index(name = "idx_inventory_farm_type_status", columnList = "farm_uuid,item_type,status"),
                @Index(name = "idx_inventory_expiry", columnList = "farm_uuid,expiry_date")})
public class InventoryItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "item_name", nullable = false, length = 160)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 12)
    private InventoryItemType type;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;
    @Column(name = "current_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;
    @Column(name = "minimum_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal minimumStock = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_uuid")
    private Supplier supplier;
    @Column(name = "batch_number", length = 80)
    private String batchNumber;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "storage_notes", length = 1000)
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private InventoryItemStatus status = InventoryItemStatus.ACTIVE;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public InventoryItemType getType() { return type; }
    public void setType(InventoryItemType value) { type = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal value) { purchasePrice = value; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal value) { currentStock = value; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal value) { minimumStock = value; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier value) { supplier = value; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String value) { batchNumber = value; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate value) { expiryDate = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public InventoryItemStatus getStatus() { return status; }
    public void setStatus(InventoryItemStatus value) { status = value; }
}
