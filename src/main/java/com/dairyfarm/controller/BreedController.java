package com.dairyfarm.controller;

import com.dairyfarm.dto.BreedDtos;
import com.dairyfarm.service.BreedService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/breeds")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER')")
public class BreedController {
    private final BreedService breeds;
    public BreedController(BreedService breeds) { this.breeds = breeds; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BreedDtos.Response create(@Valid @RequestBody BreedDtos.Request request) { return breeds.create(request); }
    @GetMapping
    public List<BreedDtos.Response> list(@RequestParam(required = false) String search) { return breeds.list(search); }
    @GetMapping("/{uuid}")
    public BreedDtos.Response get(@PathVariable UUID uuid) { return breeds.get(uuid); }
    @PutMapping("/{uuid}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BreedDtos.Response update(@PathVariable UUID uuid, @Valid @RequestBody BreedDtos.Request request) {
        return breeds.update(uuid, request);
    }
    @PatchMapping("/{uuid}/status") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BreedDtos.Response status(@PathVariable UUID uuid, @Valid @RequestBody BreedDtos.StatusRequest request) {
        return breeds.status(uuid, request.active());
    }
}
