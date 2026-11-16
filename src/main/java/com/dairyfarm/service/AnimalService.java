package com.dairyfarm.service;

import com.dairyfarm.dto.AnimalDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Animal;
import com.dairyfarm.model.AnimalStatus;
import com.dairyfarm.model.AnimalType;
import com.dairyfarm.model.Breed;
import com.dairyfarm.model.Farm;
import com.dairyfarm.repository.AnimalRepository;
import com.dairyfarm.repository.BreedRepository;
import com.dairyfarm.repository.FarmRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalService {
    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final BreedRepository breeds;
    private final CurrentUserService currentUser;
    public AnimalService(AnimalRepository animals, FarmRepository farms, BreedRepository breeds, CurrentUserService currentUser) {
        this.animals = animals; this.farms = farms; this.breeds = breeds; this.currentUser = currentUser;
    }
    @Transactional
    public AnimalDtos.Response create(AnimalDtos.Request request) {
        UUID farmUuid = currentUser.farmUuid();
        if (animals.existsByFarm_UuidAndTagNumberIgnoreCaseAndArchivedFalse(farmUuid, request.tagNumber().trim()))
            throw new ConflictException("An animal with this tag number already exists in this farm.");
        Animal animal = new Animal(); animal.setFarm(farm(farmUuid)); apply(animal, request, farmUuid);
        return response(animals.save(animal));
    }
    @Transactional(readOnly = true)
    public Page<AnimalDtos.Response> list(String search, AnimalType type, com.dairyfarm.model.Gender gender,
                                           UUID breedUuid, AnimalStatus status, Pageable pageable) {
        UUID farmUuid = currentUser.farmUuid();
        Specification<Animal> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            predicates.add(cb.isFalse(root.get("archived")));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
            }
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (gender != null) predicates.add(cb.equal(root.get("gender"), gender));
            if (breedUuid != null) predicates.add(cb.equal(root.get("breed").get("uuid"), breedUuid));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return animals.findAll(spec, pageable).map(AnimalService::response);
    }
    @Transactional(readOnly = true)
    public AnimalDtos.Response get(UUID uuid) { return response(find(uuid)); }
    @Transactional
    public AnimalDtos.Response update(UUID uuid, AnimalDtos.Request request) {
        Animal animal = find(uuid);
        UUID farmUuid = animal.getFarm().getUuid();
        if (animals.existsByFarm_UuidAndTagNumberIgnoreCaseAndArchivedFalseAndUuidNot(farmUuid,
                request.tagNumber().trim(), uuid))
            throw new ConflictException("An animal with this tag number already exists in this farm.");
        apply(animal, request, farmUuid);
        return response(animals.save(animal));
    }
    @Transactional
    public AnimalDtos.Response status(UUID uuid, AnimalStatus status) {
        Animal animal = find(uuid); animal.setStatus(status); return response(animals.save(animal));
    }
    @Transactional
    public void archive(UUID uuid) {
        Animal animal = find(uuid); animal.setArchived(true); animals.save(animal);
    }
    private Animal find(UUID uuid) {
        return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Animal not found."));
    }
    private Farm farm(UUID uuid) { return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found.")); }
    private void apply(Animal a, AnimalDtos.Request r, UUID farmUuid) {
        a.setTagNumber(r.tagNumber().trim()); a.setName(r.name()); a.setType(r.type()); a.setGender(r.gender());
        a.setDateOfBirth(r.dateOfBirth()); a.setColour(r.colour()); a.setPurchaseDate(r.purchaseDate());
        a.setPurchasePrice(r.purchasePrice()); a.setPurchaseSource(r.purchaseSource());
        a.setStatus(r.status() == null ? AnimalStatus.ACTIVE : r.status()); a.setPhotoUrl(r.photoUrl()); a.setNotes(r.notes());
        if (r.breedUuid() == null) a.setBreed(null);
        else a.setBreed(breeds.findByUuidAndFarm_Uuid(r.breedUuid(), farmUuid)
                .orElseThrow(() -> new NotFoundException("Breed not found in this farm.")));
    }
    public static AnimalDtos.Response response(Animal a) {
        return new AnimalDtos.Response(a.getUuid(), a.getFarm().getUuid(), a.getTagNumber(), a.getName(),
                a.getType(), a.getGender(), a.getBreed() == null ? null : a.getBreed().getUuid(),
                a.getBreed() == null ? null : a.getBreed().getName(), a.getDateOfBirth(), a.getColour(),
                a.getPurchaseDate(), a.getPurchasePrice(), a.getPurchaseSource(), a.getStatus(), a.getPhotoUrl(),
                a.getNotes(), a.getMother() == null ? null : a.getMother().getUuid(),
                a.getPregnancyStatus(), a.getExpectedDeliveryDate(), a.isLactating(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
