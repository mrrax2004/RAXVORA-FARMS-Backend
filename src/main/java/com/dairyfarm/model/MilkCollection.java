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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "milk_collections", indexes = @Index(name = "idx_collection_farm_date", columnList = "farm_uuid,collection_date"))
public class MilkCollection extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @Column(name = "collection_date", nullable = false) private LocalDate collectionDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private MilkingSession session;
    @Column(name = "total_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal totalQuantity;
    @Column(name = "accepted_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal acceptedQuantity;
    @Column(name = "rejected_quantity", nullable = false, precision = 10, scale = 2) private BigDecimal rejectedQuantity;
    @Column(name = "rejection_reason", length = 1000) private String rejectionReason;
    @Column(name = "quality_status", length = 20) private String qualityStatus;
    @Column(name = "storage_location", length = 160) private String storageLocation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by_uuid", nullable = false) private AppUser recordedBy;
    @Column(length = 1500) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public LocalDate getCollectionDate() { return collectionDate; } public void setCollectionDate(LocalDate v) { collectionDate = v; }
    public MilkingSession getSession() { return session; } public void setSession(MilkingSession v) { session = v; }
    public BigDecimal getTotalQuantity() { return totalQuantity; } public void setTotalQuantity(BigDecimal v) { totalQuantity = v; }
    public BigDecimal getAcceptedQuantity() { return acceptedQuantity; } public void setAcceptedQuantity(BigDecimal v) { acceptedQuantity = v; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; } public void setRejectedQuantity(BigDecimal v) { rejectedQuantity = v; }
    public String getRejectionReason() { return rejectionReason; } public void setRejectionReason(String v) { rejectionReason = v; }
    public String getQualityStatus() { return qualityStatus; } public void setQualityStatus(String v) { qualityStatus = v; }
    public String getStorageLocation() { return storageLocation; } public void setStorageLocation(String v) { storageLocation = v; }
    public AppUser getRecordedBy() { return recordedBy; } public void setRecordedBy(AppUser v) { recordedBy = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
