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
@Table(name = "milk_buyer_companies",
        uniqueConstraints = @UniqueConstraint(name = "uq_milk_buyer_company_code", columnNames = {"farm_uuid", "company_code"}),
        indexes = @Index(name = "idx_milk_buyer_company_farm_status", columnList = "farm_uuid,status"))
public class MilkBuyerCompany extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;
    @Column(name = "company_code", nullable = false, length = 60)
    private String companyCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 32)
    private MilkBuyerCompanyType companyType;
    @Column(length = 40)
    private String phoneNumber;
    @Column(length = 120)
    private String contactPerson;
    @Column(length = 500)
    private String address;
    @Column(name = "gst_number", length = 30)
    private String gstNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MilkBuyerStatus status = MilkBuyerStatus.ACTIVE;
    @Column(length = 1500)
    private String notes;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String value) { companyName = value; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String value) { companyCode = value; }
    public MilkBuyerCompanyType getCompanyType() { return companyType; }
    public void setCompanyType(MilkBuyerCompanyType value) { companyType = value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String value) { contactPerson = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String value) { gstNumber = value; }
    public MilkBuyerStatus getStatus() { return status; }
    public void setStatus(MilkBuyerStatus value) { status = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
