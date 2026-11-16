package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "animals", indexes = {
        @Index(name = "idx_animal_tag", columnList = "tag_number"),
        @Index(name = "idx_animal_name", columnList = "name"),
        @Index(name = "idx_animal_farm_status", columnList = "farm_uuid,status")
}, uniqueConstraints = @UniqueConstraint(columnNames = {"farm_uuid", "tag_number"}))
public class Animal extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_uuid", nullable = false)
    private Farm farm;
    @Column(name = "tag_number", nullable = false, length = 60)
    private String tagNumber;
    @Column(length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "animal_type", nullable = false, length = 20)
    private AnimalType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Gender gender;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_uuid")
    private Breed breed;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother_animal_uuid")
    private Animal mother;
    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_status", nullable = false, length = 20)
    private PregnancyStatus pregnancyStatus = PregnancyStatus.NOT_PREGNANT;
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;
    @Column(name = "lactating", nullable = false)
    private boolean lactating;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(length = 80)
    private String colour;
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;
    @Column(name = "purchase_source", length = 200)
    private String purchaseSource;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalStatus status = AnimalStatus.ACTIVE;
    @Column(name = "photo_url", length = 500)
    private String photoUrl;
    @Column(length = 2000)
    private String notes;
    @Column(nullable = false)
    private boolean archived;
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getTagNumber() { return tagNumber; }
    public void setTagNumber(String tagNumber) { this.tagNumber = tagNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AnimalType getType() { return type; }
    public void setType(AnimalType type) { this.type = type; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public Breed getBreed() { return breed; }
    public void setBreed(Breed breed) { this.breed = breed; }
    public Animal getMother() { return mother; }
    public void setMother(Animal mother) { this.mother = mother; }
    public PregnancyStatus getPregnancyStatus() { return pregnancyStatus; }
    public void setPregnancyStatus(PregnancyStatus pregnancyStatus) { this.pregnancyStatus = pregnancyStatus; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    public boolean isLactating() { return lactating; }
    public void setLactating(boolean lactating) { this.lactating = lactating; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getPurchaseSource() { return purchaseSource; }
    public void setPurchaseSource(String purchaseSource) { this.purchaseSource = purchaseSource; }
    public AnimalStatus getStatus() { return status; }
    public void setStatus(AnimalStatus status) { this.status = status; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
