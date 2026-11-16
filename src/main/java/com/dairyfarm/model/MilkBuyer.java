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

@Entity
@Table(name = "milk_buyers", uniqueConstraints = @UniqueConstraint(name = "uq_buyer_farm_center",
        columnNames = {"farm_uuid", "centre_code"}),
        indexes = @Index(name = "idx_buyer_farm_type_status", columnList = "farm_uuid,buyer_type,status"))
public class MilkBuyer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Enumerated(EnumType.STRING)
    @Column(name = "buyer_type", nullable = false, length = 16)
    private MilkBuyerType type = MilkBuyerType.AAVIN;
    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;
    @Column(name = "collection_centre_name", nullable = false, length = 160)
    private String centreName;
    @Column(name = "centre_code", nullable = false, length = 60)
    private String centreCode;
    @Column(name = "contact_person", length = 120)
    private String contactPerson;
    @Column(name = "phone_number", length = 40)
    private String phoneNumber;
    @Column(name = "payment_cycle_days", nullable = false)
    private int paymentCycleDays = 15;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MilkBuyerStatus status = MilkBuyerStatus.ACTIVE;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyerType getType() { return type; }
    public void setType(MilkBuyerType value) { type = value; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String value) { companyName = value; }
    public String getCentreName() { return centreName; }
    public void setCentreName(String value) { centreName = value; }
    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String value) { centreCode = value; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String value) { contactPerson = value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public int getPaymentCycleDays() { return paymentCycleDays; }
    public void setPaymentCycleDays(int value) { paymentCycleDays = value; }
    public MilkBuyerStatus getStatus() { return status; }
    public void setStatus(MilkBuyerStatus value) { status = value; }
}
