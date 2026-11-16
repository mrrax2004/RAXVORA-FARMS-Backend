package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "milk_collection_centres",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_collection_centre_code", columnNames = {"farm_uuid", "centre_code"}),
                @UniqueConstraint(name = "uq_collection_centre_legacy_buyer", columnNames = "legacy_buyer_uuid")
        },
        indexes = @Index(name = "idx_collection_centre_company", columnList = "farm_uuid,company_uuid,status"))
public class MilkCollectionCentre extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_uuid", nullable = false)
    private MilkBuyerCompany company;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legacy_buyer_uuid", nullable = false)
    private MilkBuyer legacyBuyer;
    @Column(name = "centre_name", nullable = false, length = 160)
    private String centreName;
    @Column(name = "centre_code", nullable = false, length = 60)
    private String centreCode;
    @Column(name = "contact_person", length = 120)
    private String contactPerson;
    @Column(name = "phone_number", length = 40)
    private String phoneNumber;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MilkBuyerStatus status = MilkBuyerStatus.ACTIVE;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public MilkBuyerCompany getCompany() { return company; }
    public void setCompany(MilkBuyerCompany value) { company = value; }
    public MilkBuyer getLegacyBuyer() { return legacyBuyer; }
    public void setLegacyBuyer(MilkBuyer value) { legacyBuyer = value; }
    public String getCentreName() { return centreName; }
    public void setCentreName(String value) { centreName = value; }
    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String value) { centreCode = value; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String value) { contactPerson = value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public MilkBuyerStatus getStatus() { return status; }
    public void setStatus(MilkBuyerStatus value) { status = value; }
}
