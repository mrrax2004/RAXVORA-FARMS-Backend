package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(name = "health_attachments", indexes = @Index(name = "idx_health_attachment_record", columnList = "health_record_uuid"))
public class HealthAttachment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "health_record_uuid", nullable = false)
    private HealthRecord healthRecord;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(name = "stored_name", nullable = false, unique = true, length = 80)
    private String storedName;
    @Column(name = "media_type", nullable = false, length = 100)
    private String mediaType;
    @Column(name = "file_size", nullable = false)
    private long fileSize;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm v) { farm = v; }
    public HealthRecord getHealthRecord() { return healthRecord; }
    public void setHealthRecord(HealthRecord v) { healthRecord = v; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { originalName = v; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String v) { storedName = v; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String v) { mediaType = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { fileSize = v; }
}
