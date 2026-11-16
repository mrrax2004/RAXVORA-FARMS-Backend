package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pregnancy_exams")
public class PregnancyExam extends FarmAnimalRecord {
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 24)
    private PregnancyTestType testType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PregnancyResult result;
    @Column(name = "veterinarian_name", length = 160)
    private String veterinarianName;
    @Column(length = 1500)
    private String notes;
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate v) { examDate = v; }
    public PregnancyTestType getTestType() { return testType; }
    public void setTestType(PregnancyTestType v) { testType = v; }
    public PregnancyResult getResult() { return result; }
    public void setResult(PregnancyResult v) { result = v; }
    public String getVeterinarianName() { return veterinarianName; }
    public void setVeterinarianName(String v) { veterinarianName = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate v) { expectedDeliveryDate = v; }
}
