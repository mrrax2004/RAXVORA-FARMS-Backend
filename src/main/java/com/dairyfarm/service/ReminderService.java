package com.dairyfarm.service;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Animal;
import com.dairyfarm.model.AppUser;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.InAppNotification;
import com.dairyfarm.model.Reminder;
import com.dairyfarm.model.ReminderCategory;
import com.dairyfarm.model.ReminderPriority;
import com.dairyfarm.model.ReminderStatus;
import com.dairyfarm.repository.AnimalRepository;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.NotificationRepository;
import com.dairyfarm.repository.ReminderRepository;
import com.dairyfarm.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
public class ReminderService {
    private final ReminderRepository reminders;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    private final Clock clock;
    private final LocalTime defaultTime;

    public ReminderService(ReminderRepository reminders, NotificationRepository notifications, UserRepository users,
                           AnimalRepository animals, FarmRepository farms, CurrentUserService currentUser, Clock clock,
                           @Value("${app.reminders.default-time}") LocalTime defaultTime) {
        this.reminders = reminders; this.notifications = notifications; this.users = users; this.animals = animals;
        this.farms = farms; this.currentUser = currentUser; this.clock = clock; this.defaultTime = defaultTime;
    }

    @Transactional
    public void syncAutomatic(Farm farm, Animal animal, String key, String title, LocalDate dueDate,
                              ReminderCategory category, String description, ReminderPriority priority) {
        Reminder reminder = reminders.findByFarm_UuidAndSourceKey(farm.getUuid(), key).orElseGet(Reminder::new);
        if (dueDate == null) {
            if (reminder.getUuid() != null && reminder.getStatus() == ReminderStatus.PENDING)
                reminder.setStatus(ReminderStatus.CANCELLED);
            if (reminder.getUuid() != null) reminders.save(reminder);
            return;
        }
        boolean created = reminder.getUuid() == null;
        LocalDateTime dueAt = dueDate.atTime(defaultTime);
        boolean rescheduled = !created && !dueAt.equals(reminder.getDueAt());
        boolean reactivated = !created && reminder.getStatus() == ReminderStatus.CANCELLED;
        reminder.setFarm(farm);
        reminder.setAnimal(animal);
        reminder.setSourceKey(key);
        reminder.setAutomatic(true);
        reminder.setTitle(title);
        reminder.setDueAt(dueAt);
        reminder.setCategory(category);
        reminder.setDescription(description);
        reminder.setPriority(priority);
        if (created || reminder.getStatus() == ReminderStatus.CANCELLED || (rescheduled && reminder.getStatus() != ReminderStatus.PENDING))
            reminder.setStatus(ReminderStatus.PENDING);
        reminders.saveAndFlush(reminder);
        if (created || rescheduled || reactivated)
            notifications.findAllByReminder_Uuid(reminder.getUuid()).forEach(notification -> notification.setRead(false));
        notifyFarmUsers(farm, reminder);
    }

    private void notifyFarmUsers(Farm farm, Reminder reminder) {
        for (AppUser user : users.findAllByFarm_UuidAndActiveTrue(farm.getUuid())) {
            if (!notifications.existsByUser_UuidAndReminder_Uuid(user.getUuid(), reminder.getUuid())) {
                InAppNotification notification = new InAppNotification();
                notification.setFarm(farm);
                notification.setUser(user);
                notification.setReminder(reminder);
                notifications.save(notification);
            }
        }
    }

    @Transactional
    public PhaseTwoDtos.ReminderResponse create(PhaseTwoDtos.ReminderRequest request) {
        if (request.dueAt().toLocalDate().isBefore(today()))
            throw new IllegalArgumentException("Reminder due date cannot be in the past.");
        Reminder reminder = new Reminder();
        reminder.setFarm(farm(currentUser.farmUuid()));
        reminder.setAnimal(animal(request.animalUuid()));
        reminder.setTitle(request.title().trim());
        reminder.setDueAt(request.dueAt());
        reminder.setCategory(request.category());
        reminder.setDescription(request.description());
        reminder.setPriority(request.priority());
        reminder.setAutomatic(false);
        reminder.setStatus(ReminderStatus.PENDING);
        Reminder saved = reminders.save(reminder);
        notifyFarmUsers(saved.getFarm(), saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.ReminderResponse> list(String bucket, ReminderStatus status, ReminderCategory category,
                                                     String search, int page, int size) {
        UUID farmUuid = currentUser.farmUuid();
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        Specification<Reminder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("farm").get("uuid"), farmUuid));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("animal").get("tagNumber")), pattern),
                        cb.like(cb.lower(root.get("animal").get("name")), pattern)));
            }
            switch (bucket == null ? "all" : bucket.toLowerCase()) {
                case "today" -> {
                    predicates.add(cb.between(root.get("dueAt"), start, tomorrow.minusNanos(1)));
                    predicates.add(cb.equal(root.get("status"), ReminderStatus.PENDING));
                }
                case "upcoming" -> {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), tomorrow));
                    predicates.add(cb.equal(root.get("status"), ReminderStatus.PENDING));
                }
                case "overdue" -> {
                    predicates.add(cb.lessThan(root.get("dueAt"), start));
                    predicates.add(cb.equal(root.get("status"), ReminderStatus.PENDING));
                }
                case "completed" -> predicates.add(cb.equal(root.get("status"), ReminderStatus.COMPLETED));
                case "cancelled" -> predicates.add(cb.equal(root.get("status"), ReminderStatus.CANCELLED));
                case "all" -> { }
                default -> throw new IllegalArgumentException("bucket must be today, upcoming, overdue, completed, cancelled, or all.");
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.ASC, "dueAt"));
        return reminders.findAll(spec, pageable).map(ReminderService::response);
    }

    @Transactional
    public PhaseTwoDtos.ReminderResponse updateStatus(UUID uuid, ReminderStatus status) {
        Reminder reminder = find(uuid);
        if (reminder.isAutomatic() && status == ReminderStatus.PENDING
                && reminder.getStatus() != ReminderStatus.PENDING)
            throw new ConflictException("Automatic reminders are reactivated by updating their source record.");
        reminder.setStatus(status);
        return response(reminders.save(reminder));
    }

    @Transactional
    public PhaseTwoDtos.ReminderResponse update(UUID uuid, PhaseTwoDtos.ReminderRequest request) {
        if (request.dueAt().toLocalDate().isBefore(today()))
            throw new IllegalArgumentException("Reminder due date cannot be in the past.");
        Reminder reminder = find(uuid);
        if (reminder.isAutomatic())
            throw new ConflictException("Automatic reminders are updated from their source record.");
        reminder.setAnimal(animal(request.animalUuid()));
        reminder.setTitle(request.title().trim());
        reminder.setDueAt(request.dueAt());
        reminder.setCategory(request.category());
        reminder.setDescription(request.description());
        reminder.setPriority(request.priority());
        return response(reminders.save(reminder));
    }

    @Transactional(readOnly = true)
    public Page<PhaseTwoDtos.NotificationResponse> notifications(int page, int size, Boolean unreadOnly) {
        UUID farmUuid = currentUser.farmUuid();
        AppUser user = currentUser.get();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.dairyfarm.model.InAppNotification> result = Boolean.TRUE.equals(unreadOnly)
                ? notifications.findAllByFarm_UuidAndUser_UuidAndReadFalseOrderByCreatedAtDesc(farmUuid, user.getUuid(), pageable)
                : notifications.findAllByFarm_UuidAndUser_UuidOrderByCreatedAtDesc(farmUuid, user.getUuid(), pageable);
        if (Boolean.TRUE.equals(unreadOnly))
            return result.map(ReminderService::notificationResponse);
        return result.map(ReminderService::notificationResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notifications.countByFarm_UuidAndUser_UuidAndReadFalse(currentUser.farmUuid(), currentUser.get().getUuid());
    }

    @Transactional
    public void markRead(UUID uuid) {
        InAppNotification notification = notifications.findByUuidAndFarm_UuidAndUser_Uuid(
                uuid, currentUser.farmUuid(), currentUser.get().getUuid())
                .orElseThrow(() -> new NotFoundException("Notification not found."));
        notification.setRead(true);
        notifications.save(notification);
    }

    @Transactional
    public void markAllRead() {
        UUID farm = currentUser.farmUuid();
        AppUser user = currentUser.get();
        Page<InAppNotification> page;
        int index = 0;
        do {
            page = notifications.findAllByFarm_UuidAndUser_UuidOrderByCreatedAtDesc(
                    farm, user.getUuid(), PageRequest.of(index++, 100));
            page.getContent().stream().filter(n -> !n.isRead()).forEach(n -> n.setRead(true));
        } while (page.hasNext());
    }

    public LocalDate today() { return LocalDate.now(clock); }
    public long upcomingPregnancyTests(UUID farmUuid) {
        LocalDate today = today();
        return reminders.countByFarm_UuidAndStatusAndCategoryAndDueAtBetween(
                farmUuid, ReminderStatus.PENDING, ReminderCategory.PREGNANCY_TEST,
                today.atStartOfDay(), today.plusDays(30).atTime(LocalTime.MAX));
    }
    public long overdueReminders(UUID farmUuid) {
        return reminders.countByFarm_UuidAndStatusAndDueAtBefore(farmUuid, ReminderStatus.PENDING, LocalDateTime.now(clock));
    }
    public LocalDateTime atDefaultTime(LocalDate date) { return date.atTime(defaultTime); }

    private Reminder find(UUID uuid) {
        return reminders.findByUuidAndFarm_Uuid(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Reminder not found."));
    }
    private Animal animal(UUID uuid) {
        return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Animal not found in this farm."));
    }
    private Farm farm(UUID uuid) {
        return farms.findById(uuid).orElseThrow(() -> new NotFoundException("Farm not found."));
    }
    public static PhaseTwoDtos.ReminderResponse response(Reminder r) {
        return new PhaseTwoDtos.ReminderResponse(r.getUuid(), r.getFarm().getUuid(), r.getAnimal().getUuid(),
                r.getAnimal().getTagNumber(), r.getAnimal().getName(), r.getTitle(), r.getDueAt(), r.getCategory(),
                r.getDescription(), r.getStatus(), r.getPriority(), r.isAutomatic(), r.getCreatedAt(), r.getUpdatedAt());
    }
    private static PhaseTwoDtos.NotificationResponse notificationResponse(InAppNotification n) {
        return new PhaseTwoDtos.NotificationResponse(n.getUuid(), n.isRead(), n.getCreatedAt(), response(n.getReminder()));
    }
}
