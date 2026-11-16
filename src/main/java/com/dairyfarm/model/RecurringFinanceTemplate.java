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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_finance_templates",
        indexes = @Index(name = "idx_recurring_finance_due", columnList = "farm_uuid,status,next_date"))
public class RecurringFinanceTemplate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(nullable = false, length = 160)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_uuid", nullable = false)
    private FinanceAccount account;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 12)
    private FinanceType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FinanceCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private FinanceFrequency frequency;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "next_date", nullable = false)
    private LocalDate nextDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private FinanceTemplateStatus status = FinanceTemplateStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    private AppUser createdBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public FinanceAccount getAccount() { return account; }
    public void setAccount(FinanceAccount value) { account = value; }
    public FinanceType getType() { return type; }
    public void setType(FinanceType value) { type = value; }
    public FinanceCategory getCategory() { return category; }
    public void setCategory(FinanceCategory value) { category = value; }
    public FinanceFrequency getFrequency() { return frequency; }
    public void setFrequency(FinanceFrequency value) { frequency = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public LocalDate getNextDate() { return nextDate; }
    public void setNextDate(LocalDate value) { nextDate = value; }
    public FinanceTemplateStatus getStatus() { return status; }
    public void setStatus(FinanceTemplateStatus value) { status = value; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser value) { createdBy = value; }
}
