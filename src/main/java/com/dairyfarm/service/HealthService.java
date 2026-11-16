package com.dairyfarm.service;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Animal;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.HealthRecord;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderPriority;
import com.dairyfarm.model.VaccinationRecord;
import com.dairyfarm.repository.AnimalRepository;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.HealthRecordRepository;
import com.dairyfarm.repository.VaccinationRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthService {
    private final HealthRecordRepository healthRecords;
    private final VaccinationRepository vaccinations;
    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final HealthAttachmentService attachments;
    private final ReminderService reminders;
    private final InventoryService inventory;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public HealthService(HealthRecordRepository healthRecords, VaccinationRepository vaccinations,
                         AnimalRepository animals, FarmRepository farms, HealthAttachmentService attachments,
                         ReminderService reminders, InventoryService inventory, CurrentUserService currentUser, Clock clock) {
        this.healthRecords = healthRecords; this.vaccinations = vaccinations; this.animals = animals;
        this.farms = farms; this.attachments = attachments; this.reminders = reminders;
        this.inventory = inventory; this.currentUser = currentUser; this.clock = clock;
    }

    @Transactional
    public PhaseTwoDtos.HealthResponse create(PhaseTwoDtos.HealthRequest request) {
        UUID farmUuid = currentUser.farmUuid();
        HealthRecord record = new HealthRecord();
        record.setFarm(farm(farmUuid));
        record.setAnimal(animal(request.animalUuid()));
        apply(record, request);
        HealthRecord saved = healthRecords.save(record);
        inventory.replaceMedicineUsage("TREATMENT",saved.getUuid(),null,null,
                saved.getMedicineItem()==null?null:saved.getMedicineItem().getUuid(),saved.getMedicineQuantity(),
                saved.getAnimal().getUuid(),saved.getTreatmentStartDate()==null?LocalDate.now(clock):saved.getTreatmentStartDate());
        syncHealthReminders(saved);
        return response(saved);
    }

    @Transactional
    public PhaseTwoDtos.HealthResponse update(UUID uuid, PhaseTwoDtos.HealthRequest request) {
        HealthRecord record = healthRecords.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Medical record not found."));
        UUID previousMedicine=record.getMedicineItem()==null?null:record.getMedicineItem().getUuid();
        BigDecimal previousQuantity=record.getMedicineQuantity();
        record.setAnimal(animal(request.animalUuid()));
        apply(record, request);
        HealthRecord saved = healthRecords.save(record);
        inventory.replaceMedicineUsage("TREATMENT",saved.getUuid(),previousMedicine,previousQuantity,
                saved.getMedicineItem()==null?null:saved.getMedicineItem().getUuid(),saved.getMedicineQuantity(),
                saved.getAnimal().getUuid(),saved.getTreatmentStartDate()==null?LocalDate.now(clock):saved.getTreatmentStartDate());
        syncHealthReminders(saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.HealthResponse> list(UUID animalUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "treatmentStartDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Specification<HealthRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (animalUuid != null) p.add(cb.equal(root.get("animal").get("uuid"), animalUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("diseaseName")), pattern),
                        cb.like(cb.lower(root.get("medicineName")), pattern),
                        cb.like(cb.lower(root.get("diagnosis")), pattern),
                        cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return healthRecords.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public PhaseTwoDtos.HealthResponse get(UUID uuid) {
        return response(healthRecords.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Medical record not found.")));
    }

    @Transactional
    public PhaseTwoDtos.VaccinationResponse createVaccination(PhaseTwoDtos.VaccinationRequest request) {
        VaccinationRecord record = new VaccinationRecord();
        record.setFarm(farm(currentUser.farmUuid()));
        record.setAnimal(animal(request.animalUuid()));
        apply(record, request);
        VaccinationRecord saved = vaccinations.save(record);
        inventory.replaceMedicineUsage("VACCINATION",saved.getUuid(),null,null,
                saved.getMedicineItem()==null?null:saved.getMedicineItem().getUuid(),saved.getMedicineQuantity(),
                saved.getAnimal().getUuid(),saved.getVaccinationDate());
        syncVaccinationReminder(saved);
        return vaccinationResponse(saved);
    }

    @Transactional
    public PhaseTwoDtos.VaccinationResponse updateVaccination(UUID uuid, PhaseTwoDtos.VaccinationRequest request) {
        VaccinationRecord record = vaccinations.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Vaccination record not found."));
        UUID previousMedicine=record.getMedicineItem()==null?null:record.getMedicineItem().getUuid();
        BigDecimal previousQuantity=record.getMedicineQuantity();
        record.setAnimal(animal(request.animalUuid()));
        apply(record, request);
        VaccinationRecord saved = vaccinations.save(record);
        inventory.replaceMedicineUsage("VACCINATION",saved.getUuid(),previousMedicine,previousQuantity,
                saved.getMedicineItem()==null?null:saved.getMedicineItem().getUuid(),saved.getMedicineQuantity(),
                saved.getAnimal().getUuid(),saved.getVaccinationDate());
        syncVaccinationReminder(saved);
        return vaccinationResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.VaccinationResponse> listVaccinations(UUID animalUuid, String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "vaccinationDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Specification<VaccinationRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (animalUuid != null) p.add(cb.equal(root.get("animal").get("uuid"), animalUuid));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("vaccineName")), pattern),
                        cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return vaccinations.findAll(spec, pageable).map(HealthService::vaccinationResponse);
    }

    @Transactional(readOnly = true)
    public PhaseTwoDtos.VaccinationResponse getVaccination(UUID uuid) {
        return vaccinationResponse(vaccinations.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Vaccination record not found.")));
    }

    public long treatmentsInProgress(UUID farmUuid) {
        LocalDate today = LocalDate.now(clock);
        return healthRecords.countByFarm_UuidAndTreatmentStartDateLessThanEqualAndTreatmentEndDateGreaterThanEqualOrFarm_UuidAndTreatmentStartDateLessThanEqualAndTreatmentEndDateIsNull(
                farmUuid, today, today, farmUuid, today);
    }

    private void apply(HealthRecord h, PhaseTwoDtos.HealthRequest r) {
        LocalDate today = LocalDate.now(clock);
        if (r.treatmentStartDate() != null && r.treatmentStartDate().isAfter(today))
            throw new IllegalArgumentException("Treatment start date cannot be in the future.");
        if (r.treatmentEndDate() != null && (r.treatmentStartDate() == null || r.treatmentEndDate().isBefore(r.treatmentStartDate())))
            throw new IllegalArgumentException("Treatment end date must be on or after the treatment start date.");
        if (r.followUpDate() != null && r.followUpDate().isBefore(today))
            throw new IllegalArgumentException("Follow-up date cannot be in the past.");
        validateMedicine(r.medicineItemUuid(),r.medicineQuantity());
        h.setDiseaseName(r.diseaseName()); h.setSymptoms(r.symptoms()); h.setDiagnosis(r.diagnosis());
        h.setTreatmentDetails(r.treatmentDetails()); h.setMedicineName(r.medicineItemUuid()==null?r.medicineName():
                inventory.requireMedicineItem(r.medicineItemUuid()).getName());
        h.setMedicineItem(r.medicineItemUuid()==null?null:inventory.requireMedicineItem(r.medicineItemUuid()));
        h.setMedicineQuantity(r.medicineQuantity()==null?null:r.medicineQuantity().setScale(3,java.math.RoundingMode.HALF_UP));
        h.setDosage(r.dosage());
        h.setFrequency(r.frequency()); h.setTreatmentStartDate(r.treatmentStartDate()); h.setTreatmentEndDate(r.treatmentEndDate());
        h.setVeterinarianName(r.veterinarianName()); h.setVeterinarianContact(r.veterinarianContact());
        h.setMedicalExpense(r.medicalExpense()); h.setFollowUpDate(r.followUpDate()); h.setNotes(r.notes());
    }
    private void apply(VaccinationRecord v, PhaseTwoDtos.VaccinationRequest r) {
        LocalDate today = LocalDate.now(clock);
        if (r.vaccinationDate().isAfter(today)) throw new IllegalArgumentException("Vaccination date cannot be in the future.");
        if (r.nextVaccinationDate() != null && r.nextVaccinationDate().isBefore(r.vaccinationDate()))
            throw new IllegalArgumentException("Next vaccination date must be on or after the vaccination date.");
        validateMedicine(r.medicineItemUuid(),r.medicineQuantity());
        v.setVaccineName(r.vaccineName().trim()); v.setVaccinationDate(r.vaccinationDate());
        v.setMedicineItem(r.medicineItemUuid()==null?null:inventory.requireMedicineItem(r.medicineItemUuid()));
        v.setMedicineQuantity(r.medicineQuantity()==null?null:r.medicineQuantity().setScale(3,java.math.RoundingMode.HALF_UP));
        v.setNextVaccinationDate(r.nextVaccinationDate()); v.setDose(r.dose());
        v.setAdministeredBy(r.administeredBy()); v.setNotes(r.notes());
    }
    private void syncHealthReminders(HealthRecord r) {
        String base = "health:" + r.getUuid() + ":";
        reminders.syncAutomatic(r.getFarm(), r.getAnimal(), base + "medicine",
                "Treatment course: " + (r.getMedicineName() == null ? "medicine" : r.getMedicineName()),
                r.getTreatmentEndDate(), ReminderCategory.MEDICINE, r.getDosage(), ReminderPriority.NORMAL);
        reminders.syncAutomatic(r.getFarm(), r.getAnimal(), base + "follow-up", "Health follow-up",
                r.getFollowUpDate(), ReminderCategory.FOLLOW_UP, r.getDiseaseName(), ReminderPriority.HIGH);
    }
    private void syncVaccinationReminder(VaccinationRecord v) {
        reminders.syncAutomatic(v.getFarm(), v.getAnimal(), "vaccination:" + v.getUuid(),
                "Vaccination: " + v.getVaccineName(), v.getNextVaccinationDate(),
                ReminderCategory.VACCINATION, v.getDose(), ReminderPriority.NORMAL);
    }
    private Animal animal(UUID uuid) {
        return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Animal not found in this farm."));
    }
    private Farm farm(UUID uuid) {
        return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found."));
    }
    private PhaseTwoDtos.HealthResponse response(HealthRecord h) {
        return new PhaseTwoDtos.HealthResponse(h.getUuid(), h.getFarm().getUuid(), h.getAnimal().getUuid(),
                h.getAnimal().getTagNumber(), h.getAnimal().getName(), h.getDiseaseName(), h.getSymptoms(), h.getDiagnosis(),
                h.getTreatmentDetails(), h.getMedicineName(),h.getMedicineItem()==null?null:h.getMedicineItem().getUuid(),
                h.getMedicineQuantity(),h.getDosage(), h.getFrequency(), h.getTreatmentStartDate(),
                h.getTreatmentEndDate(), h.getVeterinarianName(), h.getVeterinarianContact(), h.getMedicalExpense(),
                h.getFollowUpDate(), h.getNotes(), attachments.list(h.getUuid()), h.getCreatedAt(), h.getUpdatedAt());
    }
    private static PhaseTwoDtos.VaccinationResponse vaccinationResponse(VaccinationRecord v) {
        return new PhaseTwoDtos.VaccinationResponse(v.getUuid(), v.getFarm().getUuid(), v.getAnimal().getUuid(),
                v.getAnimal().getTagNumber(), v.getAnimal().getName(), v.getVaccineName(), v.getVaccinationDate(),
                v.getNextVaccinationDate(), v.getDose(), v.getAdministeredBy(), v.getNotes(),
                v.getMedicineItem()==null?null:v.getMedicineItem().getUuid(),v.getMedicineQuantity(),v.getCreatedAt(), v.getUpdatedAt());
    }
    private void validateMedicine(UUID itemUuid,BigDecimal quantity) {
        if((itemUuid==null)!=(quantity==null))
            throw new IllegalArgumentException("Choose both a medicine inventory item and the quantity used.");
    }
}
