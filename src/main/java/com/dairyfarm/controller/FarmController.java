package com.dairyfarm.controller;

import com.dairyfarm.dto.FarmDtos;
import com.dairyfarm.service.CurrentUserService;
import com.dairyfarm.service.FarmService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farms")
public class FarmController {
    private final FarmService farms;
    private final CurrentUserService currentUser;
    public FarmController(FarmService farms, CurrentUserService currentUser) { this.farms = farms; this.currentUser = currentUser; }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public FarmDtos.Response create(@Valid @RequestBody FarmDtos.Request request) { return farms.create(request); }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<FarmDtos.Response> list() { return farms.list(); }
    @GetMapping("/{uuid}")
    public FarmDtos.Response get(@PathVariable UUID uuid) { currentUser.requireFarmAccess(uuid); return farms.get(uuid); }
    @PutMapping("/{uuid}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public FarmDtos.Response update(@PathVariable UUID uuid, @Valid @RequestBody FarmDtos.Request request) {
        currentUser.requireFarmAccess(uuid); return farms.update(uuid, request);
    }
}
