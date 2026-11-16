package com.dairyfarm.controller;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.service.ReproductionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/reproduction")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class ReproductionController {
    private final ReproductionService reproduction;
    public ReproductionController(ReproductionService reproduction) { this.reproduction = reproduction; }

    @PostMapping("/heat")
    public PhaseTwoDtos.HeatResponse createHeat(@Valid @RequestBody PhaseTwoDtos.HeatRequest request) {
        return reproduction.createHeat(request);
    }
    @GetMapping("/heat")
    public Page<PhaseTwoDtos.HeatResponse> listHeat(@RequestParam(required = false) UUID animalUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return reproduction.listHeat(animalUuid, search, page, size); }
    @GetMapping("/heat/{uuid}")
    public PhaseTwoDtos.HeatResponse getHeat(@PathVariable UUID uuid) { return reproduction.getHeat(uuid); }
    @PutMapping("/heat/{uuid}")
    public PhaseTwoDtos.HeatResponse updateHeat(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.HeatRequest request) { return reproduction.updateHeat(uuid, request); }

    @PostMapping("/breeding")
    public PhaseTwoDtos.BreedingResponse createBreeding(@Valid @RequestBody PhaseTwoDtos.BreedingRequest request) {
        return reproduction.createBreeding(request);
    }
    @GetMapping("/breeding")
    public Page<PhaseTwoDtos.BreedingResponse> listBreeding(@RequestParam(required = false) UUID animalUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reproduction.listBreeding(animalUuid, search, page, size);
    }
    @GetMapping("/breeding/{uuid}")
    public PhaseTwoDtos.BreedingResponse getBreeding(@PathVariable UUID uuid) { return reproduction.getBreeding(uuid); }
    @PutMapping("/breeding/{uuid}")
    public PhaseTwoDtos.BreedingResponse updateBreeding(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.BreedingRequest request) { return reproduction.updateBreeding(uuid, request); }

    @PostMapping("/pregnancies")
    public PhaseTwoDtos.PregnancyResponse createPregnancyExam(@Valid @RequestBody PhaseTwoDtos.PregnancyRequest request) {
        return reproduction.createPregnancyExam(request);
    }
    @GetMapping("/pregnancies")
    public Page<PhaseTwoDtos.PregnancyResponse> listPregnancyExams(@RequestParam(required = false) UUID animalUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reproduction.listPregnancyExams(animalUuid, search, page, size);
    }
    @GetMapping("/pregnancies/{uuid}")
    public PhaseTwoDtos.PregnancyResponse getPregnancyExam(@PathVariable UUID uuid) {
        return reproduction.getPregnancyExam(uuid);
    }
    @PutMapping("/pregnancies/{uuid}")
    public PhaseTwoDtos.PregnancyResponse updatePregnancyExam(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.PregnancyRequest request) {
        return reproduction.updatePregnancyExam(uuid, request);
    }

    @PostMapping("/calvings")
    public PhaseTwoDtos.CalvingResponse createCalving(@Valid @RequestBody PhaseTwoDtos.CalvingRequest request) {
        return reproduction.createCalving(request);
    }
    @GetMapping("/calvings")
    public Page<PhaseTwoDtos.CalvingResponse> listCalvings(@RequestParam(required = false) UUID motherUuid,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reproduction.listCalvings(motherUuid, search, page, size);
    }
    @GetMapping("/calvings/{uuid}")
    public PhaseTwoDtos.CalvingResponse getCalving(@PathVariable UUID uuid) { return reproduction.getCalving(uuid); }
    @PutMapping("/calvings/{uuid}")
    public PhaseTwoDtos.CalvingResponse updateCalving(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseTwoDtos.CalvingRequest request) { return reproduction.updateCalving(uuid, request); }
}
