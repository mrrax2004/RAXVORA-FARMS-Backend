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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "milk_production", uniqueConstraints = @UniqueConstraint(
        name = "uq_milk_production_session", columnNames = {"farm_uuid", "animal_uuid", "production_date", "session"}),
        indexes = @Index(name = "idx_milk_production_farm_date", columnList = "farm_uuid,production_date"))
public class MilkProduction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "animal_uuid", nullable = false)
    private Animal animal;
    @Column(name = "production_date", nullable = false) private LocalDate productionDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private MilkingSession session;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal quantity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by_uuid", nullable = false)
    private AppUser recordedBy;
    @Column(length = 1500) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public Animal getAnimal() { return animal; } public void setAnimal(Animal v) { animal = v; }
    public LocalDate getProductionDate() { return productionDate; } public void setProductionDate(LocalDate v) { productionDate = v; }
    public MilkingSession getSession() { return session; } public void setSession(MilkingSession v) { session = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
    public AppUser getRecordedBy() { return recordedBy; } public void setRecordedBy(AppUser v) { recordedBy = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
