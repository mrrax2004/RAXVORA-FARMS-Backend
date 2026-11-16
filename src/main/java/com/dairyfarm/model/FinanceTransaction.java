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
@Table(name = "finance_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uq_finance_recurring_occurrence",
                columnNames = {"recurring_template_uuid", "occurrence_date"}),
        indexes = @Index(name = "idx_finance_tx_farm_date", columnList = "farm_uuid,transaction_date"))
public class FinanceTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_uuid", nullable = false)
    private FinanceAccount account;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 12)
    private FinanceType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FinanceCategory category;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 200)
    private String description;
    @Column(length = 1000)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_template_uuid")
    private RecurringFinanceTemplate recurringTemplate;
    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public FinanceAccount getAccount() { return account; }
    public void setAccount(FinanceAccount value) { account = value; }
    public FinanceType getType() { return type; }
    public void setType(FinanceType value) { type = value; }
    public FinanceCategory getCategory() { return category; }
    public void setCategory(FinanceCategory value) { category = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public RecurringFinanceTemplate getRecurringTemplate() { return recurringTemplate; }
    public void setRecurringTemplate(RecurringFinanceTemplate value) { recurringTemplate = value; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate value) { occurrenceDate = value; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser value) { recordedBy = value; }
}
