package com.dairyfarm.controller;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderStatus;
import com.dairyfarm.service.ReminderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class ReminderController {
    private final ReminderService reminders;
    public ReminderController(ReminderService reminders) { this.reminders = reminders; }

    @PostMapping("/reminders")
    public PhaseTwoDtos.ReminderResponse create(@Valid @RequestBody PhaseTwoDtos.ReminderRequest request) {
        return reminders.create(request);
    }
    @GetMapping("/reminders")
    public Page<PhaseTwoDtos.ReminderResponse> list(@RequestParam(defaultValue = "all") String bucket,
            @RequestParam(required = false) ReminderStatus status, @RequestParam(required = false) ReminderCategory category,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reminders.list(bucket, status, category, search, page, size);
    }
    @PutMapping("/reminders/{uuid}")
    public PhaseTwoDtos.ReminderResponse update(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.ReminderRequest request) { return reminders.update(uuid, request); }
    @PatchMapping("/reminders/{uuid}/status")
    public PhaseTwoDtos.ReminderResponse updateStatus(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.ReminderStatusRequest request) {
        return reminders.updateStatus(uuid, request.status());
    }

    @GetMapping("/notifications")
    public Page<PhaseTwoDtos.NotificationResponse> notifications(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Boolean unreadOnly) {
        return reminders.notifications(page, size, unreadOnly);
    }
    @GetMapping("/notifications/unread-count")
    public PhaseTwoDtos.UnreadCount unreadCount() { return new PhaseTwoDtos.UnreadCount(reminders.unreadCount()); }
    @PatchMapping("/notifications/{uuid}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID uuid) {
        reminders.markRead(uuid); return ResponseEntity.noContent().build();
    }
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead() {
        reminders.markAllRead(); return ResponseEntity.noContent().build();
    }
}
