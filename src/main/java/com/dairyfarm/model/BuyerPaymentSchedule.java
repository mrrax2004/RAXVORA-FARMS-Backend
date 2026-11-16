package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "buyer_payment_schedules",
        uniqueConstraints = @UniqueConstraint(name = "uq_buyer_payment_schedule_company", columnNames = {"farm_uuid", "company_uuid"}))
public class BuyerPaymentSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_uuid", nullable = false)
    private MilkBuyerCompany company;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_cycle", nullable = false, length = 24)
    private BuyerPaymentCycle paymentCycle;
    @Column(name = "custom_period_days")
    private Integer customPeriodDays;
    @Column(name = "period_start_rule", length = 160)
    private String periodStartRule;
    @Column(name = "period_end_rule", length = 160)
    private String periodEndRule;
    @Column(name = "expected_payment_day")
    private Integer expectedPaymentDay;
    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays;
    @Column(length = 1500)
    private String notes;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyerCompany getCompany() { return company; }
    public void setCompany(MilkBuyerCompany value) { company = value; }
    public BuyerPaymentCycle getPaymentCycle() { return paymentCycle; }
    public void setPaymentCycle(BuyerPaymentCycle value) { paymentCycle = value; }
    public Integer getCustomPeriodDays() { return customPeriodDays; }
    public void setCustomPeriodDays(Integer value) { customPeriodDays = value; }
    public String getPeriodStartRule() { return periodStartRule; }
    public void setPeriodStartRule(String value) { periodStartRule = value; }
    public String getPeriodEndRule() { return periodEndRule; }
    public void setPeriodEndRule(String value) { periodEndRule = value; }
    public Integer getExpectedPaymentDay() { return expectedPaymentDay; }
    public void setExpectedPaymentDay(Integer value) { expectedPaymentDay = value; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int value) { gracePeriodDays = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
