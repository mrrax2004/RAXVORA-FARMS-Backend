package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "heat_detections")
public class HeatDetection extends FarmAnimalRecord {
    @Column(name = "detection_date", nullable = false)
    private LocalDate detectionDate;
    @Column(name = "symptoms", nullable = false, length = 1500)
    private String symptoms;
    @Enumerated(EnumType.STRING)
    @Column(name = "observation_period", nullable = false, length = 16)
    private ObservationPeriod observationPeriod;
    @Column(name = "observed_by", length = 160)
    private String observedBy;
    @Column(length = 1500)
    private String notes;
    public LocalDate getDetectionDate() { return detectionDate; }
    public void setDetectionDate(LocalDate v) { detectionDate = v; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String v) { symptoms = v; }
    public ObservationPeriod getObservationPeriod() { return observationPeriod; }
    public void setObservationPeriod(ObservationPeriod v) { observationPeriod = v; }
    public String getObservedBy() { return observedBy; }
    public void setObservedBy(String v) { observedBy = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
