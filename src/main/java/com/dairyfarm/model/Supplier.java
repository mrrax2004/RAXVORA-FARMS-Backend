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
@Table(name = "farm_suppliers", uniqueConstraints = @UniqueConstraint(name = "uq_supplier_farm_name",
        columnNames = {"farm_uuid", "supplier_name"}),
        indexes = @Index(name = "idx_supplier_farm_status", columnList = "farm_uuid,status"))
public class Supplier extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "supplier_name", nullable = false, length = 160)
    private String name;
    @Column(name = "phone_number", length = 40)
    private String phone;
    @Column(length = 800)
    private String address;
    @Column(name = "gst_number", length = 30)
    private String gstNumber;
    @Column(name = "contact_person", length = 120)
    private String contactPerson;
    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false, length = 20)
    private SupplierType type;
    @Column(length = 1200)
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SupplierStatus status = SupplierStatus.ACTIVE;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String value) { gstNumber = value; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String value) { contactPerson = value; }
    public SupplierType getType() { return type; }
    public void setType(SupplierType value) { type = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public SupplierStatus getStatus() { return status; }
    public void setStatus(SupplierStatus value) { status = value; }
}
