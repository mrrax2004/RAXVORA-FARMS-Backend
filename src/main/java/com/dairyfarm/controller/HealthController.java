package com.dairyfarm.controller;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.service.HealthAttachmentService;
import com.dairyfarm.service.HealthService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class HealthController {
    private final HealthService health;
    private final HealthAttachmentService attachments;
    public HealthController(HealthService health, HealthAttachmentService attachments) {
        this.health = health; this.attachments = attachments;
    }

    @PostMapping("/health/records")
    public PhaseTwoDtos.HealthResponse createHealth(@Valid @RequestBody PhaseTwoDtos.HealthRequest request) {
        return health.create(request);
    }
    @GetMapping("/health/records")
    public Page<PhaseTwoDtos.HealthResponse> listHealth(@RequestParam(required = false) UUID animalUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return health.list(animalUuid, search, page, size);
    }
    @GetMapping("/health/records/{uuid}")
    public PhaseTwoDtos.HealthResponse getHealth(@PathVariable UUID uuid) { return health.get(uuid); }
    @PutMapping("/health/records/{uuid}")
    public PhaseTwoDtos.HealthResponse updateHealth(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.HealthRequest request) { return health.update(uuid, request); }
    @PostMapping("/health/records/{uuid}/attachments")
    public PhaseTwoDtos.AttachmentResponse uploadAttachment(@PathVariable UUID uuid, @RequestPart("file") MultipartFile file)
            throws IOException { return attachments.upload(uuid, file); }
    @GetMapping("/health/records/{uuid}/attachments/{attachmentUuid}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID uuid, @PathVariable UUID attachmentUuid)
            throws IOException { return attachments.download(uuid, attachmentUuid); }

    @PostMapping("/vaccinations")
    public PhaseTwoDtos.VaccinationResponse createVaccination(@Valid @RequestBody PhaseTwoDtos.VaccinationRequest request) {
        return health.createVaccination(request);
    }
    @GetMapping("/vaccinations")
    public Page<PhaseTwoDtos.VaccinationResponse> listVaccinations(@RequestParam(required = false) UUID animalUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return health.listVaccinations(animalUuid, search, page, size);
    }
    @GetMapping("/vaccinations/{uuid}")
    public PhaseTwoDtos.VaccinationResponse getVaccination(@PathVariable UUID uuid) { return health.getVaccination(uuid); }
    @PutMapping("/vaccinations/{uuid}")
    public PhaseTwoDtos.VaccinationResponse updateVaccination(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.VaccinationRequest request) { return health.updateVaccination(uuid, request); }
}
