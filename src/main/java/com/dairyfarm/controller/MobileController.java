package com.dairyfarm.controller;

import com.dairyfarm.dto.MobileDtos;
import com.dairyfarm.dto.PhaseThreeDtos;
import com.dairyfarm.service.MobileSyncService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class MobileController {
    private final MobileSyncService mobile;
    public MobileController(MobileSyncService mobile) { this.mobile=mobile; }
    @GetMapping("/snapshot")
    public MobileDtos.SnapshotResponse snapshot() { return mobile.snapshot(); }
    @GetMapping("/dashboard")
    public MobileDtos.DashboardResponse dashboard() { return mobile.dashboard(); }
    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.CustomerResponse createCustomer(@Valid @RequestBody PhaseThreeDtos.CustomerRequest request) {
        return mobile.createCustomer(request);
    }
    @PostMapping("/sync")
    public MobileDtos.SyncResponse sync(@Valid @RequestBody MobileDtos.SyncRequest request) {
        return mobile.sync(request);
    }
}
