package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "calf_births")
public class CalfBirth extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calving_uuid", nullable = false)
    private CalvingRecord calving;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 12)
    private Gender gender;
    @Column(name = "birth_weight", precision = 7, scale = 2)
    private BigDecimal birthWeight;
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 16)
    private CalfHealthStatus healthStatus;
    @Column(name = "stillborn", nullable = false)
    private boolean stillborn;
    @Column(length = 1000)
    private String notes;
    @Column(nullable = false)
    private boolean voided;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calf_animal_uuid", unique = true)
    private Animal calf;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm v) { farm = v; }
    public CalvingRecord getCalving() { return calving; }
    public void setCalving(CalvingRecord v) { calving = v; }
    public Gender getGender() { return gender; }
    public void setGender(Gender v) { gender = v; }
    public BigDecimal getBirthWeight() { return birthWeight; }
    public void setBirthWeight(BigDecimal v) { birthWeight = v; }
    public CalfHealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(CalfHealthStatus v) { healthStatus = v; }
    public boolean isStillborn() { return stillborn; }
    public void setStillborn(boolean v) { stillborn = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean v) { voided = v; }
    public Animal getCalf() { return calf; }
    public void setCalf(Animal v) { calf = v; }
}
