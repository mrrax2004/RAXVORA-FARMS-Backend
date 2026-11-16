package com.dairyfarm.service;

import com.dairyfarm.dto.BreedDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Breed;
import com.dairyfarm.model.Farm;
import com.dairyfarm.repository.BreedRepository;
import com.dairyfarm.repository.FarmRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreedService {
    private final BreedRepository breeds;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    public BreedService(BreedRepository breeds, FarmRepository farms, CurrentUserService currentUser) {
        this.breeds = breeds; this.farms = farms; this.currentUser = currentUser;
    }
    @Transactional
    public BreedDtos.Response create(BreedDtos.Request request) {
        UUID farmUuid = currentUser.farmUuid();
        if (breeds.existsByFarm_UuidAndNameIgnoreCase(farmUuid, request.name().trim()))
            throw new ConflictException("A breed with this name already exists in this farm.");
        Breed breed = new Breed(); breed.setFarm(farm(farmUuid)); apply(breed, request);
        return response(breeds.save(breed));
    }
    @Transactional(readOnly = true)
    public List<BreedDtos.Response> list(String search) {
        String term = search == null ? "" : search.toLowerCase();
        return breeds.findAllByFarm_Uuid(currentUser.farmUuid()).stream()
                .filter(b -> b.getName().toLowerCase().contains(term)).map(BreedService::response).toList();
    }
    @Transactional(readOnly = true)
    public BreedDtos.Response get(UUID uuid) { return response(find(uuid)); }
    @Transactional
    public BreedDtos.Response update(UUID uuid, BreedDtos.Request request) {
        Breed breed = find(uuid);
        boolean duplicate = breeds.existsByFarm_UuidAndNameIgnoreCase(breed.getFarm().getUuid(), request.name().trim())
                && !breed.getName().equalsIgnoreCase(request.name().trim());
        if (duplicate) throw new ConflictException("A breed with this name already exists in this farm.");
        apply(breed, request); return response(breeds.save(breed));
    }
    @Transactional
    public BreedDtos.Response status(UUID uuid, boolean active) {
        Breed breed = find(uuid); breed.setActive(active); return response(breeds.save(breed));
    }
    private Breed find(UUID uuid) {
        return breeds.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Breed not found."));
    }
    private Farm farm(UUID uuid) { return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found.")); }
    private static void apply(Breed b, BreedDtos.Request r) { b.setName(r.name().trim()); b.setDescription(r.description()); }
    public static BreedDtos.Response response(Breed b) {
        return new BreedDtos.Response(b.getUuid(), b.getFarm().getUuid(), b.getName(), b.getDescription(),
                b.isActive(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
