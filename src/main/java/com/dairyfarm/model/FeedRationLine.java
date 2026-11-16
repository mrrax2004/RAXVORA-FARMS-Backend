package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "feed_ration_lines")
public class FeedRationLine extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ration_uuid", nullable = false)
    private FeedRation ration;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_uuid", nullable = false)
    private InventoryItem item;
    @Column(name = "quantity_per_animal_day", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityPerAnimalDay;
    public FeedRation getRation() { return ration; }
    public void setRation(FeedRation value) { ration = value; }
    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem value) { item = value; }
    public BigDecimal getQuantityPerAnimalDay() { return quantityPerAnimalDay; }
    public void setQuantityPerAnimalDay(BigDecimal value) { quantityPerAnimalDay = value; }
}
