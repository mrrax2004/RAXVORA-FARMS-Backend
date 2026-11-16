package com.dairyfarm.controller;

import com.dairyfarm.dto.ProcurementDtos;
import com.dairyfarm.service.ProcurementDirectoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER','ACCOUNTANT')")
public class ProcurementDirectoryController {
    private final ProcurementDirectoryService directory;

    public ProcurementDirectoryController(ProcurementDirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping("/companies")
    public List<ProcurementDtos.CompanyResponse> companies() { return directory.companies(); }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.CompanyResponse createCompany(@Valid @RequestBody ProcurementDtos.CompanyRequest request) {
        return directory.createCompany(request);
    }

    @PutMapping("/companies/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.CompanyResponse updateCompany(@PathVariable UUID uuid,
            @Valid @RequestBody ProcurementDtos.CompanyRequest request) {
        return directory.updateCompany(uuid, request);
    }

    @GetMapping("/centres")
    public List<ProcurementDtos.CentreResponse> centres(@RequestParam(required = false) UUID companyUuid) {
        return directory.centres(companyUuid);
    }

    @PostMapping("/centres")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.CentreResponse createCentre(@Valid @RequestBody ProcurementDtos.CentreRequest request) {
        return directory.createCentre(request);
    }

    @PutMapping("/centres/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.CentreResponse updateCentre(@PathVariable UUID uuid,
            @Valid @RequestBody ProcurementDtos.CentreRequest request) {
        return directory.updateCentre(uuid, request);
    }

    @GetMapping("/pricing/{companyUuid}")
    public List<ProcurementDtos.PricingResponse> pricing(@PathVariable UUID companyUuid) {
        return directory.pricing(companyUuid);
    }

    @PostMapping("/pricing/{companyUuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.PricingResponse addPricing(@PathVariable UUID companyUuid,
            @Valid @RequestBody ProcurementDtos.PricingRequest request) {
        return directory.addPricing(companyUuid, request);
    }

    @GetMapping("/payment-schedule/{companyUuid}")
    public ProcurementDtos.PaymentScheduleResponse schedule(@PathVariable UUID companyUuid) {
        return directory.schedule(companyUuid);
    }

    @PutMapping("/payment-schedule/{companyUuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProcurementDtos.PaymentScheduleResponse schedule(@PathVariable UUID companyUuid,
            @Valid @RequestBody ProcurementDtos.PaymentScheduleRequest request) {
        return directory.saveSchedule(companyUuid, request);
    }
}
