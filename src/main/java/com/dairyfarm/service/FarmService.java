package com.dairyfarm.service;

import com.dairyfarm.dto.FarmDtos;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Farm;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.model.Breed;
import com.dairyfarm.repository.BreedRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmService {
    private final FarmRepository farms;
    private final BreedRepository breeds;
    public FarmService(FarmRepository farms, BreedRepository breeds) { this.farms = farms; this.breeds = breeds; }
    @Transactional
    public FarmDtos.Response create(FarmDtos.Request request) {
        Farm farm = new Farm();
        apply(farm, request);
        Farm saved = farms.save(farm);
        for (String name : List.of("Jersey", "Holstein Friesian (HF)", "Gir", "Sahiwal", "Kangayam", "Murrah", "Crossbred", "Other")) {
            Breed breed = new Breed(); breed.setFarm(saved); breed.setName(name); breeds.save(breed);
        }
        return response(saved);
    }
    @Transactional(readOnly = true)
    public List<FarmDtos.Response> list() { return farms.findAll().stream().map(FarmService::response).toList(); }
    @Transactional(readOnly = true)
    public FarmDtos.Response get(UUID uuid) { return response(find(uuid)); }
    @Transactional
    public FarmDtos.Response update(UUID uuid, FarmDtos.Request request) {
        Farm farm = find(uuid); apply(farm, request); return response(farms.save(farm));
    }
    private Farm find(UUID uuid) { return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found.")); }
    private static void apply(Farm farm, FarmDtos.Request r) {
        farm.setName(r.name().trim()); farm.setOwnerName(r.ownerName().trim()); farm.setPhone(r.phone());
        farm.setEmail(r.email()); farm.setAddress(r.address()); farm.setCity(r.city()); farm.setDistrict(r.district());
        farm.setState(r.state()); farm.setPinCode(r.pinCode());
    }
    public static FarmDtos.Response response(Farm f) {
        return new FarmDtos.Response(f.getUuid(), f.getName(), f.getOwnerName(), f.getPhone(), f.getEmail(),
                f.getAddress(), f.getCity(), f.getDistrict(), f.getState(), f.getPinCode(), f.getCreatedAt(), f.getUpdatedAt());
    }
}
