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
@Table(name = "health_records")
public class HealthRecord extends FarmAnimalRecord {
    @Column(name = "disease_name", length = 160)
    private String diseaseName;
    @Column(length = 1500)
    private String symptoms;
    @Column(length = 1500)
    private String diagnosis;
    @Column(name = "treatment_details", length = 2000)
    private String treatmentDetails;
    @Column(name = "medicine_name", length = 160)
    private String medicineName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_item_uuid")
    private InventoryItem medicineItem;
    @Column(name = "medicine_quantity", precision = 14, scale = 3)
    private BigDecimal medicineQuantity;
    @Column(length = 200)
    private String dosage;
    @Column(length = 200)
    private String frequency;
    @Column(name = "treatment_start_date")
    private LocalDate treatmentStartDate;
    @Column(name = "treatment_end_date")
    private LocalDate treatmentEndDate;
    @Column(name = "veterinarian_name", length = 160)
    private String veterinarianName;
    @Column(name = "veterinarian_contact", length = 80)
    private String veterinarianContact;
    @Column(name = "medical_expense", precision = 12, scale = 2)
    private BigDecimal medicalExpense;
    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
    @Column(length = 2000)
    private String notes;
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String v) { diseaseName = v; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String v) { symptoms = v; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String v) { diagnosis = v; }
    public String getTreatmentDetails() { return treatmentDetails; }
    public void setTreatmentDetails(String v) { treatmentDetails = v; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String v) { medicineName = v; }
    public InventoryItem getMedicineItem() { return medicineItem; }
    public void setMedicineItem(InventoryItem v) { medicineItem = v; }
    public BigDecimal getMedicineQuantity() { return medicineQuantity; }
    public void setMedicineQuantity(BigDecimal v) { medicineQuantity = v; }
    public String getDosage() { return dosage; }
    public void setDosage(String v) { dosage = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { frequency = v; }
    public LocalDate getTreatmentStartDate() { return treatmentStartDate; }
    public void setTreatmentStartDate(LocalDate v) { treatmentStartDate = v; }
    public LocalDate getTreatmentEndDate() { return treatmentEndDate; }
    public void setTreatmentEndDate(LocalDate v) { treatmentEndDate = v; }
    public String getVeterinarianName() { return veterinarianName; }
    public void setVeterinarianName(String v) { veterinarianName = v; }
    public String getVeterinarianContact() { return veterinarianContact; }
    public void setVeterinarianContact(String v) { veterinarianContact = v; }
    public BigDecimal getMedicalExpense() { return medicalExpense; }
    public void setMedicalExpense(BigDecimal v) { medicalExpense = v; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate v) { followUpDate = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
