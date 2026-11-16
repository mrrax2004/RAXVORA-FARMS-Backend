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
@Table(name = "milk_bill_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_milk_bill_item_delivery", columnNames = "delivery_uuid"))
public class MilkBillItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_uuid", nullable = false)
    private MilkBill bill;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_uuid", nullable = false)
    private MilkDelivery delivery;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "line_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public MilkBill getBill() { return bill; }
    public void setBill(MilkBill value) { bill = value; }
    public MilkDelivery getDelivery() { return delivery; }
    public void setDelivery(MilkDelivery value) { delivery = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { unitPrice = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
}
