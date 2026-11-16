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
import java.util.UUID;

@Entity
@Table(name = "mobile_sync_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_mobile_sync_client_record",
                columnNames = {"farm_uuid", "device_uuid", "client_record_uuid"}),
        indexes = @Index(name = "idx_mobile_sync_farm_date", columnList = "farm_uuid,created_at"))
public class MobileSyncRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "device_uuid", nullable = false)
    private UUID deviceUuid;
    @Column(name = "client_record_uuid", nullable = false)
    private UUID clientRecordUuid;
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 24)
    private MobileSyncType type;
    @Column(name = "server_record_uuid", nullable = false)
    private UUID serverRecordUuid;
    @Column(name = "client_estimated_rate", precision = 10, scale = 2)
    private BigDecimal clientEstimatedRate;
    @Column(name = "client_estimated_amount", precision = 12, scale = 2)
    private BigDecimal clientEstimatedAmount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "synced_by_uuid", nullable = false)
    private AppUser syncedBy;

    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public UUID getDeviceUuid() { return deviceUuid; }
    public void setDeviceUuid(UUID value) { deviceUuid = value; }
    public UUID getClientRecordUuid() { return clientRecordUuid; }
    public void setClientRecordUuid(UUID value) { clientRecordUuid = value; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String value) { payloadHash = value; }
    public MobileSyncType getType() { return type; }
    public void setType(MobileSyncType value) { type = value; }
    public UUID getServerRecordUuid() { return serverRecordUuid; }
    public void setServerRecordUuid(UUID value) { serverRecordUuid = value; }
    public BigDecimal getClientEstimatedRate() { return clientEstimatedRate; }
    public void setClientEstimatedRate(BigDecimal value) { clientEstimatedRate = value; }
    public BigDecimal getClientEstimatedAmount() { return clientEstimatedAmount; }
    public void setClientEstimatedAmount(BigDecimal value) { clientEstimatedAmount = value; }
    public AppUser getSyncedBy() { return syncedBy; }
    public void setSyncedBy(AppUser value) { syncedBy = value; }
}
