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

@Entity
@Table(name = "milk_stock_transactions")
public class MilkStockTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private MilkStockTransactionType type;
    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 2) private BigDecimal quantityChange;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "collection_uuid") private MilkCollection collection;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "production_uuid") private MilkProduction production;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_uuid") private MilkSale sale;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delivery_uuid") private MilkDelivery delivery;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supply_uuid") private AavinMilkSupply supply;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by_uuid", nullable = false) private AppUser recordedBy;
    @Column(name = "reference_note", length = 500) private String referenceNote;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public MilkStockTransactionType getType() { return type; } public void setType(MilkStockTransactionType v) { type = v; }
    public BigDecimal getQuantityChange() { return quantityChange; } public void setQuantityChange(BigDecimal v) { quantityChange = v; }
    public MilkCollection getCollection() { return collection; } public void setCollection(MilkCollection v) { collection = v; }
    public MilkProduction getProduction() { return production; } public void setProduction(MilkProduction v) { production = v; }
    public MilkSale getSale() { return sale; } public void setSale(MilkSale v) { sale = v; }
    public MilkDelivery getDelivery() { return delivery; } public void setDelivery(MilkDelivery v) { delivery = v; }
    public AavinMilkSupply getSupply() { return supply; } public void setSupply(AavinMilkSupply v) { supply = v; }
    public AppUser getRecordedBy() { return recordedBy; } public void setRecordedBy(AppUser v) { recordedBy = v; }
    public String getReferenceNote() { return referenceNote; } public void setReferenceNote(String v) { referenceNote = v; }
}
