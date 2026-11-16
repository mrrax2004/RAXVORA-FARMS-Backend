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
@Table(name = "buyer_settlement_items", uniqueConstraints = @UniqueConstraint(name = "uq_aavin_settlement_supply",
        columnNames = {"supply_uuid"}))
public class AavinSettlementItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_uuid", nullable = false)
    private AavinSettlement settlement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_uuid", nullable = false)
    private AavinMilkSupply supply;
    @Column(name = "accepted_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal acceptedQuantity;
    @Column(name = "applied_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedRate;
    @Column(name = "milk_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal milkValue;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public AavinSettlement getSettlement() { return settlement; }
    public void setSettlement(AavinSettlement value) { settlement = value; }
    public AavinMilkSupply getSupply() { return supply; }
    public void setSupply(AavinMilkSupply value) { supply = value; }
    public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(BigDecimal value) { acceptedQuantity = value; }
    public BigDecimal getAppliedRate() { return appliedRate; }
    public void setAppliedRate(BigDecimal value) { appliedRate = value; }
    public BigDecimal getMilkValue() { return milkValue; }
    public void setMilkValue(BigDecimal value) { milkValue = value; }
}
