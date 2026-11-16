package com.dairyfarm.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feed_rations")
public class FeedRation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(nullable = false, length = 160)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_group", length = 24)
    private RationTargetGroup targetGroup;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_uuid")
    private Animal animal;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;
    @Column(nullable = false)
    private boolean active = true;
    @OneToMany(mappedBy = "ration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedRationLine> lines = new ArrayList<>();
    public Farm getFarm() { return farm; }
    public void setFarm(Farm value) { farm = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public RationTargetGroup getTargetGroup() { return targetGroup; }
    public void setTargetGroup(RationTargetGroup value) { targetGroup = value; }
    public Animal getAnimal() { return animal; }
    public void setAnimal(Animal value) { animal = value; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate value) { effectiveFrom = value; }
    public LocalDate getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDate value) { effectiveUntil = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public List<FeedRationLine> getLines() { return lines; }
    public void setLines(List<FeedRationLine> value) {
        lines.clear();
        for (FeedRationLine line : value) { line.setRation(this); lines.add(line); }
    }
}
