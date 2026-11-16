package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vaccinations")
public class VaccinationRecord extends FarmAnimalRecord {
    @Column(name = "vaccine_name", nullable = false, length = 160)
    private String vaccineName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_item_uuid")
    private InventoryItem medicineItem;
    @Column(name = "medicine_quantity", precision = 14, scale = 3)
    private BigDecimal medicineQuantity;
    @Column(name = "vaccination_date", nullable = false)
    private LocalDate vaccinationDate;
    @Column(name = "next_vaccination_date")
    private LocalDate nextVaccinationDate;
    @Column(length = 200)
    private String dose;
    @Column(name = "administered_by", length = 160)
    private String administeredBy;
    @Column(length = 1500)
    private String notes;
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String v) { vaccineName = v; }
    public InventoryItem getMedicineItem() { return medicineItem; }
    public void setMedicineItem(InventoryItem v) { medicineItem = v; }
    public BigDecimal getMedicineQuantity() { return medicineQuantity; }
    public void setMedicineQuantity(BigDecimal v) { medicineQuantity = v; }
    public LocalDate getVaccinationDate() { return vaccinationDate; }
    public void setVaccinationDate(LocalDate v) { vaccinationDate = v; }
    public LocalDate getNextVaccinationDate() { return nextVaccinationDate; }
    public void setNextVaccinationDate(LocalDate v) { nextVaccinationDate = v; }
    public String getDose() { return dose; }
    public void setDose(String v) { dose = v; }
    public String getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(String v) { administeredBy = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
