package com.dairyfarm.service;

import com.dairyfarm.model.AnimalStatus;
import com.dairyfarm.model.AnimalType;
import com.dairyfarm.repository.AnimalRepository;
import com.dairyfarm.repository.BreedRepository;
import com.dairyfarm.repository.UserRepository;
import com.dairyfarm.repository.VaccinationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final AnimalRepository animals;
    private final BreedRepository breeds;
    private final UserRepository users;
    private final CurrentUserService currentUser;
    private final ReproductionService reproduction;
    private final HealthService health;
    private final ReminderService reminders;
    private final VaccinationRepository vaccinations;
    private final Clock clock;
    public DashboardService(AnimalRepository animals, BreedRepository breeds, UserRepository users,
                            CurrentUserService currentUser, ReproductionService reproduction,
                            HealthService health, ReminderService reminders, VaccinationRepository vaccinations, Clock clock) {
        this.animals = animals; this.breeds = breeds; this.users = users; this.currentUser = currentUser;
        this.reproduction = reproduction; this.health = health; this.reminders = reminders;
        this.vaccinations = vaccinations; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> summary(UUID requestedFarm) {
        UUID farm = requestedFarm == null ? currentUser.farmUuid() : requestedFarm;
        if (farm == null) throw new IllegalArgumentException("Choose a farm to view its dashboard.");
        currentUser.requireFarmAccess(farm);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("farmUuid", farm);
        result.put("totalAnimals", animals.countByFarm_UuidAndArchivedFalse(farm));
        result.put("activeAnimals", animals.countByFarm_UuidAndStatusAndArchivedFalse(farm, AnimalStatus.ACTIVE));
        result.put("cows", animals.countByFarm_UuidAndTypeAndArchivedFalse(farm, AnimalType.COW));
        result.put("bulls", animals.countByFarm_UuidAndTypeAndArchivedFalse(farm, AnimalType.BULL));
        result.put("calves", animals.countByFarm_UuidAndTypeAndArchivedFalse(farm, AnimalType.CALF));
        result.put("heifers", animals.countByFarm_UuidAndTypeAndArchivedFalse(farm, AnimalType.HEIFER));
        result.put("pregnantAnimals", reproduction.pregnantAnimals(farm));
        result.put("animalsInHeat", reproduction.animalsInHeat(farm));
        result.put("upcomingPregnancyTests", reminders.upcomingPregnancyTests(farm));
        result.put("expectedDeliveriesThisMonth", reproduction.expectedDeliveriesThisMonth(farm));
        LocalDate today = LocalDate.now(clock);
        result.put("vaccinationsDue", vaccinations.countByFarm_UuidAndNextVaccinationDateBetween(farm, today, today.plusDays(30)));
        result.put("medicalTreatmentsInProgress", health.treatmentsInProgress(farm));
        result.put("overdueReminders", reminders.overdueReminders(farm));
        result.put("unreadNotifications", reminders.unreadCount());
        result.put("totalBreeds", breeds.countByFarm_Uuid(farm));
        result.put("totalUsers", users.findAllByFarm_Uuid(farm).size());
        result.put("recentAnimals", animals.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("farm").get("uuid"), farm), cb.isFalse(root.get("archived"))),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AnimalService::response).getContent());
        return result;
    }
}
