package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "farms")
public class Farm extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String district;
    private String state;
    @Column(name = "pin_code")
    private String pinCode;
    @Column(name = "default_milk_rate", nullable = false, precision = 10, scale = 2,
            columnDefinition = "DECIMAL(10,2) NOT NULL DEFAULT 0.00")
    private BigDecimal defaultMilkRate = BigDecimal.ZERO;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public BigDecimal getDefaultMilkRate() { return defaultMilkRate; }
    public void setDefaultMilkRate(BigDecimal defaultMilkRate) { this.defaultMilkRate = defaultMilkRate; }
}
