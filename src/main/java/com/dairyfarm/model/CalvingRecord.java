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
import java.time.LocalDate;

@Entity
@Table(name = "calving_records",
        indexes = @Index(name = "idx_calving_farm_date", columnList = "farm_uuid,actual_delivery_date"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_uuid", "mother_animal_uuid", "actual_delivery_date"}))
public class CalvingRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mother_animal_uuid", nullable = false)
    private Animal mother;
    @Column(name = "actual_delivery_date", nullable = false)
    private LocalDate actualDeliveryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 16)
    private DeliveryType deliveryType;
    @Column(name = "calf_count", nullable = false)
    private int calfCount;
    @Column(name = "veterinarian_name", length = 160)
    private String veterinarianName;
    @Column(length = 2000)
    private String notes;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm v) { farm = v; }
    public Animal getMother() { return mother; }
    public void setMother(Animal v) { mother = v; }
    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate v) { actualDeliveryDate = v; }
    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType v) { deliveryType = v; }
    public int getCalfCount() { return calfCount; }
    public void setCalfCount(int v) { calfCount = v; }
    public String getVeterinarianName() { return veterinarianName; }
    public void setVeterinarianName(String v) { veterinarianName = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
