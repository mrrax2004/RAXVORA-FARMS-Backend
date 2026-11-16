package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "in_app_notifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_user_reminder", columnNames = {"user_uuid", "reminder_uuid"}))
public class InAppNotification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_uuid", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_uuid", nullable = false)
    private Reminder reminder;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm v) { farm = v; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser v) { user = v; }
    public Reminder getReminder() { return reminder; }
    public void setReminder(Reminder v) { reminder = v; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { read = v; }
}
