package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "breeding_records")
public class BreedingRecord extends FarmAnimalRecord {
    @Column(name = "breeding_date", nullable = false)
    private LocalDate breedingDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "breeding_method", nullable = false, length = 12)
    private BreedingMethod method;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bull_animal_uuid")
    private Animal bull;
    @Column(name = "semen_identification", length = 160)
    private String semenIdentification;
    @Column(name = "technician_name", length = 160)
    private String technicianName;
    @Column(precision = 12, scale = 2)
    private BigDecimal cost;
    @Column(length = 1500)
    private String notes;
    public LocalDate getBreedingDate() { return breedingDate; }
    public void setBreedingDate(LocalDate v) { breedingDate = v; }
    public BreedingMethod getMethod() { return method; }
    public void setMethod(BreedingMethod v) { method = v; }
    public Animal getBull() { return bull; }
    public void setBull(Animal v) { bull = v; }
    public String getSemenIdentification() { return semenIdentification; }
    public void setSemenIdentification(String v) { semenIdentification = v; }
    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String v) { technicianName = v; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal v) { cost = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
