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
import java.time.LocalDate;

@Entity
@Table(name = "milk_customers", indexes = @Index(name = "idx_customer_farm_name", columnList = "farm_uuid,customer_name"))
public class Customer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "customer_name", nullable = false, length = 160) private String name;
    @Column(name = "mobile_number", length = 40) private String mobileNumber;
    @Column(length = 1000) private String address;
    @Enumerated(EnumType.STRING) @Column(name = "customer_type", nullable = false, length = 20) private CustomerType type;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private CustomerStatus status = CustomerStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(name = "billing_frequency", nullable = false, length = 24)
    private MilkBillingFrequency billingFrequency = MilkBillingFrequency.DAILY;
    @Column(length = 1500) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getMobileNumber() { return mobileNumber; } public void setMobileNumber(String v) { mobileNumber = v; }
    public String getAddress() { return address; } public void setAddress(String v) { address = v; }
    public CustomerType getType() { return type; } public void setType(CustomerType v) { type = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public CustomerStatus getStatus() { return status; } public void setStatus(CustomerStatus v) { status = v; }
    public MilkBillingFrequency getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(MilkBillingFrequency v) { billingFrequency = v == null ? MilkBillingFrequency.DAILY : v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
