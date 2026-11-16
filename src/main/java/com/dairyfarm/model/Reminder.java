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
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders", indexes = {
        @Index(name = "idx_reminder_farm_due_status", columnList = "farm_uuid,due_at,status"),
        @Index(name = "idx_reminder_animal_due", columnList = "animal_uuid,due_at")
}, uniqueConstraints = @UniqueConstraint(name = "uq_reminder_source", columnNames = {"farm_uuid", "source_key"}))
public class Reminder extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_uuid", nullable = false)
    private Animal animal;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReminderCategory category;
    @Column(length = 1500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReminderStatus status = ReminderStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReminderPriority priority = ReminderPriority.NORMAL;
    @Column(name = "source_key", length = 100)
    private String sourceKey;
    @Column(name = "automatic", nullable = false)
    private boolean automatic;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm v) { farm = v; }
    public Animal getAnimal() { return animal; }
    public void setAnimal(Animal v) { animal = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime v) { dueAt = v; }
    public ReminderCategory getCategory() { return category; }
    public void setCategory(ReminderCategory v) { category = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus v) { status = v; }
    public ReminderPriority getPriority() { return priority; }
    public void setPriority(ReminderPriority v) { priority = v; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String v) { sourceKey = v; }
    public boolean isAutomatic() { return automatic; }
    public void setAutomatic(boolean v) { automatic = v; }
}
