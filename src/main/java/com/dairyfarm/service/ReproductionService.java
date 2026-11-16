package com.dairyfarm.service;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Animal;
import com.dairyfarm.model.AnimalStatus;
import com.dairyfarm.model.AnimalType;
import com.dairyfarm.model.BreedingMethod;
import com.dairyfarm.model.BreedingRecord;
import com.dairyfarm.model.CalfBirth;
import com.dairyfarm.model.CalfHealthStatus;
import com.dairyfarm.model.CalvingRecord;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.Gender;
import com.dairyfarm.model.HeatDetection;
import com.dairyfarm.model.ObservationPeriod;
import com.dairyfarm.model.PregnancyExam;
import com.dairyfarm.model.PregnancyResult;
import com.dairyfarm.model.PregnancyStatus;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderPriority;
import com.dairyfarm.model.ReminderStatus;
import com.dairyfarm.repository.AnimalRepository;
import com.dairyfarm.repository.BreedingRepository;
import com.dairyfarm.repository.CalfBirthRepository;
import com.dairyfarm.repository.CalvingRepository;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.HeatDetectionRepository;
import com.dairyfarm.repository.PregnancyExamRepository;
import com.dairyfarm.repository.ReminderRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReproductionService {
    private final HeatDetectionRepository heats;
    private final BreedingRepository breeding;
    private final PregnancyExamRepository pregnancyExams;
    private final CalvingRepository calvings;
    private final CalfBirthRepository calfBirths;
    private final ReminderRepository reminderRepository;
    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final ReminderService reminders;
    private final CurrentUserService currentUser;
    private final Clock clock;
    private final int gestationDays;
    private final int heatCycleDays;
    private final int pregnancyTestDays;

    public ReproductionService(HeatDetectionRepository heats, BreedingRepository breeding,
                               PregnancyExamRepository pregnancyExams, CalvingRepository calvings,
                               CalfBirthRepository calfBirths, ReminderRepository reminderRepository,
                               AnimalRepository animals, FarmRepository farms, ReminderService reminders,
                               CurrentUserService currentUser, Clock clock,
                               @Value("${app.reproduction.gestation-days}") int gestationDays,
                               @Value("${app.reproduction.heat-cycle-days}") int heatCycleDays,
                               @Value("${app.reproduction.pregnancy-test-days}") int pregnancyTestDays) {
        this.heats = heats; this.breeding = breeding; this.pregnancyExams = pregnancyExams; this.calvings = calvings;
        this.calfBirths = calfBirths; this.reminderRepository = reminderRepository; this.animals = animals;
        this.farms = farms; this.reminders = reminders; this.currentUser = currentUser; this.clock = clock;
        if (gestationDays < 1 || heatCycleDays < 1 || pregnancyTestDays < 1)
            throw new IllegalArgumentException("Reproduction period settings must be positive.");
        this.gestationDays = gestationDays; this.heatCycleDays = heatCycleDays; this.pregnancyTestDays = pregnancyTestDays;
    }

    @Transactional
    public PhaseTwoDtos.HeatResponse createHeat(PhaseTwoDtos.HeatRequest request) {
        HeatDetection heat = new HeatDetection();
        heat.setFarm(farm(currentUser.farmUuid()));
        heat.setAnimal(female(request.animalUuid()));
        apply(heat, request);
        HeatDetection saved = heats.save(heat);
        reminders.syncAutomatic(saved.getFarm(), saved.getAnimal(), "heat:" + saved.getUuid(),
                "Heat check: " + animalName(saved.getAnimal()), saved.getDetectionDate().plusDays(heatCycleDays),
                ReminderCategory.HEAT_CHECK, "Observe the animal for the next heat cycle.", ReminderPriority.NORMAL);
        return heatResponse(saved);
    }

    @Transactional
    public PhaseTwoDtos.HeatResponse updateHeat(UUID uuid, PhaseTwoDtos.HeatRequest request) {
        HeatDetection heat = heats.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Heat record not found."));
        heat.setAnimal(female(request.animalUuid()));
        apply(heat, request);
        HeatDetection saved = heats.save(heat);
        reminders.syncAutomatic(saved.getFarm(), saved.getAnimal(), "heat:" + saved.getUuid(),
                "Heat check: " + animalName(saved.getAnimal()), saved.getDetectionDate().plusDays(heatCycleDays),
                ReminderCategory.HEAT_CHECK, "Observe the animal for the next heat cycle.", ReminderPriority.NORMAL);
        return heatResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.HeatResponse> listHeat(UUID animalUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Specification<HeatDetection> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (animalUuid != null) p.add(cb.equal(root.get("animal").get("uuid"), animalUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("symptoms")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return heats.findAll(spec, page(page, size, "detectionDate")).map(ReproductionService::heatResponse);
    }
    @Transactional(readOnly = true)
    public PhaseTwoDtos.HeatResponse getHeat(UUID uuid) {
        return heatResponse(heats.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Heat record not found.")));
    }

    @Transactional
    public PhaseTwoDtos.BreedingResponse createBreeding(PhaseTwoDtos.BreedingRequest request) {
        BreedingRecord record = new BreedingRecord();
        record.setFarm(farm(currentUser.farmUuid()));
        record.setAnimal(female(request.animalUuid()));
        apply(record, request);
        BreedingRecord saved = breeding.save(record);
        syncPregnancyTestReminder(saved);
        return breedingResponse(saved);
    }
    @Transactional
    public PhaseTwoDtos.BreedingResponse updateBreeding(UUID uuid, PhaseTwoDtos.BreedingRequest request) {
        BreedingRecord record = breeding.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Breeding record not found."));
        record.setAnimal(female(request.animalUuid()));
        apply(record, request);
        BreedingRecord saved = breeding.save(record);
        syncPregnancyTestReminder(saved);
        return breedingResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.BreedingResponse> listBreeding(UUID animalUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Specification<BreedingRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (animalUuid != null) p.add(cb.equal(root.get("animal").get("uuid"), animalUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("semenIdentification")), pattern),
                        cb.like(cb.lower(root.get("technicianName")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return breeding.findAll(spec, page(page, size, "breedingDate")).map(ReproductionService::breedingResponse);
    }
    @Transactional(readOnly = true)
    public PhaseTwoDtos.BreedingResponse getBreeding(UUID uuid) {
        return breedingResponse(breeding.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Breeding record not found.")));
    }

    @Transactional
    public PhaseTwoDtos.PregnancyResponse createPregnancyExam(PhaseTwoDtos.PregnancyRequest request) {
        PregnancyExam exam = new PregnancyExam();
        exam.setFarm(farm(currentUser.farmUuid()));
        exam.setAnimal(female(request.animalUuid()));
        apply(exam, request);
        PregnancyExam saved = pregnancyExams.save(exam);
        updatePregnancyStatus(saved.getAnimal());
        syncPregnancyReminder(saved.getAnimal());
        if (saved.getResult() != PregnancyResult.INCONCLUSIVE) {
            reminderRepository.findAllByFarm_UuidAndAnimal_UuidAndCategoryAndStatus(saved.getFarm().getUuid(),
                    saved.getAnimal().getUuid(), ReminderCategory.PREGNANCY_TEST, ReminderStatus.PENDING)
                    .forEach(r -> r.setStatus(ReminderStatus.CANCELLED));
        }
        return pregnancyResponse(saved);
    }
    @Transactional
    public PhaseTwoDtos.PregnancyResponse updatePregnancyExam(UUID uuid, PhaseTwoDtos.PregnancyRequest request) {
        PregnancyExam exam = pregnancyExams.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Pregnancy examination not found."));
        Animal oldAnimal = exam.getAnimal();
        exam.setAnimal(female(request.animalUuid()));
        apply(exam, request);
        PregnancyExam saved = pregnancyExams.save(exam);
        if (!oldAnimal.getUuid().equals(saved.getAnimal().getUuid())) {
            updatePregnancyStatus(oldAnimal);
            syncPregnancyReminder(oldAnimal);
        }
        updatePregnancyStatus(saved.getAnimal());
        syncPregnancyReminder(saved.getAnimal());
        if (saved.getResult() != PregnancyResult.INCONCLUSIVE) {
            reminderRepository.findAllByFarm_UuidAndAnimal_UuidAndCategoryAndStatus(saved.getFarm().getUuid(),
                    saved.getAnimal().getUuid(), ReminderCategory.PREGNANCY_TEST, ReminderStatus.PENDING)
                    .forEach(r -> r.setStatus(ReminderStatus.CANCELLED));
        }
        return pregnancyResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.PregnancyResponse> listPregnancyExams(UUID animalUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Specification<PregnancyExam> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (animalUuid != null) p.add(cb.equal(root.get("animal").get("uuid"), animalUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("veterinarianName")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return pregnancyExams.findAll(spec, page(page, size, "examDate")).map(ReproductionService::pregnancyResponse);
    }
    @Transactional(readOnly = true)
    public PhaseTwoDtos.PregnancyResponse getPregnancyExam(UUID uuid) {
        return pregnancyResponse(pregnancyExams.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Pregnancy examination not found.")));
    }

    @Transactional
    public PhaseTwoDtos.CalvingResponse createCalving(PhaseTwoDtos.CalvingRequest request) {
        validateCalving(request);
        UUID farmUuid = currentUser.farmUuid();
        Animal mother = female(request.motherUuid());
        CalvingRecord record = new CalvingRecord();
        record.setFarm(farm(farmUuid));
        record.setMother(mother);
        apply(record, request);
        CalvingRecord saved = calvings.saveAndFlush(record);
        syncCalves(saved, request.calves(), List.of());
        updatePregnancyStatus(mother);
        syncPregnancyReminder(mother);
        return calvingResponse(saved);
    }

    @Transactional
    public PhaseTwoDtos.CalvingResponse updateCalving(UUID uuid, PhaseTwoDtos.CalvingRequest request) {
        validateCalving(request);
        UUID farmUuid = currentUser.farmUuid();
        CalvingRecord record = calvings.findByUuidAndFarm_Uuid(uuid, farmUuid)
                .orElseThrow(() -> new NotFoundException("Calving record not found."));
        if (!record.getMother().getUuid().equals(request.motherUuid()))
            throw new ConflictException("The mother animal cannot be changed on an existing delivery.");
        apply(record, request);
        CalvingRecord saved = calvings.saveAndFlush(record);
        List<CalfBirth> existing = calfBirths.findAllByCalving_UuidAndVoidedFalseOrderByCreatedAtAsc(saved.getUuid());
        syncCalves(saved, request.calves(), existing);
        updatePregnancyStatus(saved.getMother());
        syncPregnancyReminder(saved.getMother());
        return calvingResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.CalvingResponse> listCalvings(UUID motherUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Specification<CalvingRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (motherUuid != null) p.add(cb.equal(root.get("mother").get("uuid"), motherUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("mother").get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("veterinarianName")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return calvings.findAll(spec, page(page, size, "actualDeliveryDate")).map(this::calvingResponse);
    }
    @Transactional(readOnly = true)
    public PhaseTwoDtos.CalvingResponse getCalving(UUID uuid) {
        return calvingResponse(calvings.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Calving record not found.")));
    }

    public long animalsInHeat(UUID farmUuid) {
        LocalDate today = LocalDate.now(clock);
        return heats.countDistinctAnimalsInHeat(farmUuid, today.minusDays(heatCycleDays - 1L), today);
    }
    public long pregnantAnimals(UUID farmUuid) {
        return animals.countByFarm_UuidAndPregnancyStatusAndArchivedFalse(farmUuid, PregnancyStatus.PREGNANT);
    }
    public long expectedDeliveriesThisMonth(UUID farmUuid) {
        LocalDate today = LocalDate.now(clock);
        return animals.countByFarm_UuidAndPregnancyStatusAndExpectedDeliveryDateBetweenAndArchivedFalse(
                farmUuid, PregnancyStatus.PREGNANT, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    private void validateCalving(PhaseTwoDtos.CalvingRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.actualDeliveryDate().isAfter(today)) throw new IllegalArgumentException("Actual delivery date cannot be in the future.");
        if (request.calves() == null || request.calves().size() != request.calfCount())
            throw new IllegalArgumentException("Calf count must match the number of calf birth details.");
    }
    private void syncCalves(CalvingRecord calving, List<PhaseTwoDtos.CalfRequest> requests, List<CalfBirth> existing) {
        Set<UUID> used = new HashSet<>();
        for (int i = 0; i < requests.size(); i++) {
            PhaseTwoDtos.CalfRequest request = requests.get(i);
            CalfBirth birth;
            if (request.birthUuid() != null) {
                birth = existing.stream().filter(b -> b.getUuid().equals(request.birthUuid())).findFirst()
                        .orElseThrow(() -> new NotFoundException("Calf birth does not belong to this delivery."));
            } else if (i < existing.size() && !used.contains(existing.get(i).getUuid())) {
                birth = existing.get(i);
            } else {
                birth = new CalfBirth();
                birth.setFarm(calving.getFarm());
                birth.setCalving(calving);
            }
            if (birth.getUuid() != null && !used.add(birth.getUuid()))
                throw new IllegalArgumentException("A calf birth cannot be listed more than once.");
            birth.setGender(request.gender());
            birth.setBirthWeight(request.birthWeight());
            birth.setHealthStatus(request.healthStatus());
            birth.setStillborn(request.stillborn());
            birth.setNotes(request.notes());
            birth.setVoided(false);
            syncCalfAnimal(birth, calving);
            calfBirths.save(birth);
        }
        for (CalfBirth birth : existing) {
            if (!used.contains(birth.getUuid())) {
                birth.setVoided(true);
                archiveCalf(birth.getCalf());
                calfBirths.save(birth);
            }
        }
    }
    private void syncCalfAnimal(CalfBirth birth, CalvingRecord calving) {
        boolean live = !birth.isStillborn() && birth.getHealthStatus() != CalfHealthStatus.DECEASED;
        Animal calf = birth.getCalf();
        if (calf == null && live) {
            calf = new Animal();
            calf.setFarm(calving.getFarm());
            calf.setTagNumber("CALF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            calf.setName("Calf of " + calving.getMother().getTagNumber());
            calf.setType(AnimalType.CALF);
            calf.setBreed(calving.getMother().getBreed());
            calf.setMother(calving.getMother());
        }
        if (calf != null) {
            calf.setGender(birth.getGender());
            calf.setDateOfBirth(calving.getActualDeliveryDate());
            calf.setArchived(!live);
            calf.setStatus(live ? AnimalStatus.ACTIVE : AnimalStatus.DEAD);
            animals.save(calf);
            birth.setCalf(calf);
        }
    }
    private void archiveCalf(Animal calf) {
        if (calf != null) {
            calf.setArchived(true);
            calf.setStatus(AnimalStatus.DEAD);
            animals.save(calf);
        }
    }
    private void apply(CalvingRecord record, PhaseTwoDtos.CalvingRequest request) {
        record.setActualDeliveryDate(request.actualDeliveryDate());
        record.setDeliveryType(request.deliveryType());
        record.setCalfCount(request.calfCount());
        record.setVeterinarianName(request.veterinarianName());
        record.setNotes(request.notes());
    }
    private void apply(HeatDetection h, PhaseTwoDtos.HeatRequest r) {
        validateHistoricalDate(r.detectionDate(), "Heat detection date");
        h.setDetectionDate(r.detectionDate()); h.setSymptoms(r.symptoms().trim());
        h.setObservationPeriod(r.observationPeriod()); h.setObservedBy(r.observedBy()); h.setNotes(r.notes());
    }
    private void apply(BreedingRecord b, PhaseTwoDtos.BreedingRequest r) {
        validateHistoricalDate(r.breedingDate(), "Breeding date");
        if (r.method() == BreedingMethod.AI && r.bullUuid() == null
                && (r.semenIdentification() == null || r.semenIdentification().isBlank()))
            throw new IllegalArgumentException("Artificial insemination requires a bull or semen identification.");
        b.setBreedingDate(r.breedingDate()); b.setMethod(r.method());
        b.setBull(r.bullUuid() == null ? null : bull(r.bullUuid()));
        b.setSemenIdentification(r.semenIdentification()); b.setTechnicianName(r.technicianName());
        b.setCost(r.cost()); b.setNotes(r.notes());
    }
    private void apply(PregnancyExam e, PhaseTwoDtos.PregnancyRequest r) {
        validateHistoricalDate(r.examDate(), "Pregnancy examination date");
        e.setExamDate(r.examDate()); e.setTestType(r.testType()); e.setResult(r.result());
        e.setVeterinarianName(r.veterinarianName()); e.setNotes(r.notes());
        e.setExpectedDeliveryDate(r.result() == PregnancyResult.PREGNANT ? r.examDate().plusDays(gestationDays) : null);
    }
    private void updatePregnancyStatus(Animal animal) {
        var latestExam = pregnancyExams.findFirstByFarm_UuidAndAnimal_UuidAndResultNotOrderByExamDateDescCreatedAtDesc(
                animal.getFarm().getUuid(), animal.getUuid(), PregnancyResult.INCONCLUSIVE);
        var latestCalving = calvings.findFirstByFarm_UuidAndMother_UuidOrderByActualDeliveryDateDescCreatedAtDesc(
                animal.getFarm().getUuid(), animal.getUuid());
        if (latestExam.isPresent() && (latestCalving.isEmpty()
                || latestExam.get().getExamDate().isAfter(latestCalving.get().getActualDeliveryDate()))) {
            PregnancyExam exam = latestExam.get();
            boolean pregnant = exam.getResult() == PregnancyResult.PREGNANT;
            animal.setPregnancyStatus(pregnant ? PregnancyStatus.PREGNANT : PregnancyStatus.NOT_PREGNANT);
            animal.setExpectedDeliveryDate(pregnant ? exam.getExpectedDeliveryDate() : null);
        } else {
            animal.setPregnancyStatus(PregnancyStatus.NOT_PREGNANT);
            animal.setExpectedDeliveryDate(null);
        }
        animals.save(animal);
    }
    private void syncPregnancyReminder(Animal animal) {
        LocalDate dueDate = animal.getPregnancyStatus() == PregnancyStatus.PREGNANT
                ? animal.getExpectedDeliveryDate() : null;
        reminders.syncAutomatic(animal.getFarm(), animal, "pregnancy:" + animal.getUuid(),
                "Expected delivery: " + animalName(animal), dueDate,
                ReminderCategory.EXPECTED_DELIVERY, "Estimated delivery based on a positive pregnancy examination.",
                ReminderPriority.HIGH);
    }
    private void syncPregnancyTestReminder(BreedingRecord record) {
        LocalDate due = record.getBreedingDate().plusDays(pregnancyTestDays);
        reminders.syncAutomatic(record.getFarm(), record.getAnimal(), "breeding:" + record.getUuid(),
                "Pregnancy test: " + animalName(record.getAnimal()), due, ReminderCategory.PREGNANCY_TEST,
                "Pregnancy examination is due after breeding.", ReminderPriority.NORMAL);
    }
    private void validateHistoricalDate(LocalDate date, String name) {
        if (date.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException(name + " cannot be in the future.");
    }
    private Animal female(UUID uuid) {
        Animal animal = animal(uuid);
        if (animal.getGender() != Gender.FEMALE) throw new IllegalArgumentException("This record must be linked to a female animal.");
        return animal;
    }
    private Animal bull(UUID uuid) {
        Animal animal = animal(uuid);
        if (animal.getGender() != Gender.MALE) throw new IllegalArgumentException("The selected bull must be a male animal.");
        return animal;
    }
    private Animal animal(UUID uuid) {
        return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Animal not found in this farm."));
    }
    private Farm farm(UUID uuid) {
        return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found."));
    }
    private Pageable page(int page, int size, String field) {
        return PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, field).and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }
    private static String animalName(Animal a) { return a.getName() == null ? a.getTagNumber() : a.getName(); }
    private static PhaseTwoDtos.HeatResponse heatResponse(HeatDetection h) {
        return new PhaseTwoDtos.HeatResponse(h.getUuid(), h.getFarm().getUuid(), h.getAnimal().getUuid(),
                h.getAnimal().getTagNumber(), h.getAnimal().getName(), h.getDetectionDate(), h.getSymptoms(),
                h.getObservationPeriod(), h.getObservedBy(), h.getNotes(), h.getCreatedAt(), h.getUpdatedAt());
    }
    private static PhaseTwoDtos.BreedingResponse breedingResponse(BreedingRecord b) {
        return new PhaseTwoDtos.BreedingResponse(b.getUuid(), b.getFarm().getUuid(), b.getAnimal().getUuid(),
                b.getAnimal().getTagNumber(), b.getAnimal().getName(), b.getBreedingDate(), b.getMethod(),
                b.getBull() == null ? null : b.getBull().getUuid(), b.getBull() == null ? null : b.getBull().getTagNumber(),
                b.getSemenIdentification(), b.getTechnicianName(), b.getCost(), b.getNotes(), b.getCreatedAt(), b.getUpdatedAt());
    }
    private static PhaseTwoDtos.PregnancyResponse pregnancyResponse(PregnancyExam e) {
        return new PhaseTwoDtos.PregnancyResponse(e.getUuid(), e.getFarm().getUuid(), e.getAnimal().getUuid(),
                e.getAnimal().getTagNumber(), e.getAnimal().getName(), e.getExamDate(), e.getTestType(), e.getResult(),
                e.getVeterinarianName(), e.getNotes(), e.getExpectedDeliveryDate(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private PhaseTwoDtos.CalvingResponse calvingResponse(CalvingRecord c) {
        List<PhaseTwoDtos.CalfResponse> calves = calfBirths.findAllByCalving_UuidAndVoidedFalseOrderByCreatedAtAsc(c.getUuid())
                .stream().map(b -> new PhaseTwoDtos.CalfResponse(b.getUuid(), b.getCalf() == null ? null : b.getCalf().getUuid(),
                        b.getCalf() == null ? null : b.getCalf().getTagNumber(), b.getGender(), b.getBirthWeight(),
                        b.getHealthStatus(), b.isStillborn(), b.getNotes())).toList();
        return new PhaseTwoDtos.CalvingResponse(c.getUuid(), c.getFarm().getUuid(), c.getMother().getUuid(),
                c.getMother().getTagNumber(), c.getMother().getName(), c.getActualDeliveryDate(), c.getDeliveryType(),
                c.getCalfCount(), c.getVeterinarianName(), c.getNotes(), calves, c.getCreatedAt(), c.getUpdatedAt());
    }
}
