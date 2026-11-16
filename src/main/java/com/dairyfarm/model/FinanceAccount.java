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

@Entity
@Table(name = "finance_accounts", uniqueConstraints = @UniqueConstraint(
        name = "uq_finance_account_farm_name", columnNames = {"farm_uuid", "account_name"}),
        indexes = @Index(name = "idx_finance_account_farm", columnList = "farm_uuid,status"))
public class FinanceAccount extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "account_name", nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private FinanceAccountType type;
    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance;
    @Column(nullable = false)
    private boolean status = true;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public FinanceAccountType getType() { return type; }
    public void setType(FinanceAccountType value) { type = value; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal value) { openingBalance = value; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean value) { status = value; }
}
