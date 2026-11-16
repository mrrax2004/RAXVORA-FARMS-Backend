package com.dairyfarm.controller;

import com.dairyfarm.dto.AnimalDtos;
import com.dairyfarm.model.AnimalStatus;
import com.dairyfarm.model.AnimalType;
import com.dairyfarm.model.Gender;
import com.dairyfarm.service.AnimalService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/animals")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class AnimalController {
    private final AnimalService animals;
    public AnimalController(AnimalService animals) { this.animals = animals; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AnimalDtos.Response create(@Valid @RequestBody AnimalDtos.Request request) { return animals.create(request); }
    @GetMapping
    public Page<AnimalDtos.Response> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) AnimalType type, @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) UUID breedUuid, @RequestParam(required = false) AnimalStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String direction) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        Sort sort = Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC,
                java.util.Set.of("createdAt", "updatedAt", "tagNumber", "name", "dateOfBirth", "status").contains(sortBy)
                        ? sortBy : "createdAt");
        return animals.list(search, type, gender, breedUuid, status, PageRequest.of(safePage, safeSize, sort));
    }
    @GetMapping("/{uuid}")
    public AnimalDtos.Response get(@PathVariable UUID uuid) { return animals.get(uuid); }
    @PutMapping("/{uuid}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AnimalDtos.Response update(@PathVariable UUID uuid, @Valid @RequestBody AnimalDtos.Request request) {
        return animals.update(uuid, request);
    }
    @PatchMapping("/{uuid}/status") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
    public AnimalDtos.Response status(@PathVariable UUID uuid, @Valid @RequestBody AnimalDtos.StatusRequest request) {
        return animals.status(uuid, request.status());
    }
    @DeleteMapping("/{uuid}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void archive(@PathVariable UUID uuid) { animals.archive(uuid); }
}
