package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "milk_sale_items")
public class MilkSaleItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sale_uuid", nullable = false) private MilkSale sale;
    @Column(name = "milk_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal discount;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    public MilkSale getSale() { return sale; } public void setSale(MilkSale v) { sale = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public BigDecimal getDiscount() { return discount; } public void setDiscount(BigDecimal v) { discount = v; }
    public BigDecimal getLineTotal() { return lineTotal; } public void setLineTotal(BigDecimal v) { lineTotal = v; }
}
