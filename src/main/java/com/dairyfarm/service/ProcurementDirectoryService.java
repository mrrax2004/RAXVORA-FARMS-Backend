package com.dairyfarm.service;

import com.dairyfarm.dto.ProcurementDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementDirectoryService {
    private final MilkBuyerCompanyRepository companies;
    private final MilkCollectionCentreRepository centres;
    private final BuyerPricingConfigurationRepository pricing;
    private final BuyerPaymentScheduleRepository schedules;
    private final MilkBuyerRepository legacyBuyers;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;

    public ProcurementDirectoryService(MilkBuyerCompanyRepository companies, MilkCollectionCentreRepository centres,
            BuyerPricingConfigurationRepository pricing, BuyerPaymentScheduleRepository schedules,
            MilkBuyerRepository legacyBuyers, FarmRepository farms, CurrentUserService currentUser) {
        this.companies = companies;
        this.centres = centres;
        this.pricing = pricing;
        this.schedules = schedules;
        this.legacyBuyers = legacyBuyers;
        this.farms = farms;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ProcurementDtos.CompanyResponse> companies() {
        UUID farm = farmUuid();
        return companies.findAllByFarm_UuidOrderByCompanyNameAsc(farm).stream()
                .map(ProcurementDirectoryService::companyResponse).toList();
    }

    @Transactional
    public ProcurementDtos.CompanyResponse createCompany(ProcurementDtos.CompanyRequest request) {
        requireAdmin();
        Farm farm = lockedFarm();
        String code = request.companyCode().trim();
        if (companies.existsByFarm_UuidAndCompanyCodeIgnoreCase(farm.getUuid(), code))
            throw new ConflictException("A buyer company with this code already exists in this farm.");
        MilkBuyerCompany company = new MilkBuyerCompany();
        company.setFarm(farm);
        applyCompany(company, request);
        return companyResponse(companies.save(company));
    }

    @Transactional
    public ProcurementDtos.CompanyResponse updateCompany(UUID uuid, ProcurementDtos.CompanyRequest request) {
        requireAdmin();
        MilkBuyerCompany company = company(uuid);
        if (companies.existsByFarm_UuidAndCompanyCodeIgnoreCaseAndUuidNot(
                farmUuid(), request.companyCode().trim(), uuid))
            throw new ConflictException("A buyer company with this code already exists in this farm.");
        applyCompany(company, request);
        return companyResponse(companies.save(company));
    }

    @Transactional(readOnly = true)
    public List<ProcurementDtos.CentreResponse> centres(UUID companyUuid) {
        UUID farm = farmUuid();
        if (companyUuid == null)
            return centres.findAllByFarm_UuidOrderByCompany_CompanyNameAscCentreNameAsc(farm).stream()
                    .map(ProcurementDirectoryService::centreResponse).toList();
        company(companyUuid);
        return centres.findAllByFarm_UuidAndCompany_UuidOrderByCentreNameAsc(farm, companyUuid).stream()
                .map(ProcurementDirectoryService::centreResponse).toList();
    }

    @Transactional
    public ProcurementDtos.CentreResponse createCentre(ProcurementDtos.CentreRequest request) {
        requireAdmin();
        Farm farm = lockedFarm();
        MilkBuyerCompany company = company(request.companyUuid());
        String code = request.centreCode().trim();
        if (centres.existsByFarm_UuidAndCentreCodeIgnoreCase(farm.getUuid(), code)
                || legacyBuyers.existsByFarm_UuidAndCentreCodeIgnoreCase(farm.getUuid(), code))
            throw new ConflictException("A collection centre with this code already exists in this farm.");

        MilkBuyer legacy = new MilkBuyer();
        legacy.setFarm(farm);
        legacy.setType(MilkBuyerType.AAVIN);
        legacy.setCompanyName(company.getCompanyName());
        legacy.setCentreName(request.centreName().trim());
        legacy.setCentreCode(code);
        legacy.setContactPerson(clean(request.contactPerson()));
        legacy.setPhoneNumber(clean(request.phoneNumber()));
        legacy.setPaymentCycleDays(15);
        legacy.setStatus(request.status());
        legacy = legacyBuyers.save(legacy);

        MilkCollectionCentre centre = new MilkCollectionCentre();
        centre.setFarm(farm);
        centre.setCompany(company);
        centre.setLegacyBuyer(legacy);
        centre.setCentreName(legacy.getCentreName());
        centre.setCentreCode(legacy.getCentreCode());
        centre.setContactPerson(legacy.getContactPerson());
        centre.setPhoneNumber(legacy.getPhoneNumber());
        centre.setStatus(legacy.getStatus());
        return centreResponse(centres.save(centre));
    }

    @Transactional
    public ProcurementDtos.CentreResponse updateCentre(UUID uuid, ProcurementDtos.CentreRequest request) {
        requireAdmin();
        MilkCollectionCentre centre = centres.findByUuidAndFarm_Uuid(uuid, farmUuid())
                .orElseThrow(() -> new NotFoundException("Collection centre not found in this farm."));
        MilkBuyerCompany company = company(request.companyUuid());
        String code = request.centreCode().trim();
        if (centres.existsByFarm_UuidAndCentreCodeIgnoreCaseAndUuidNot(farmUuid(), code, uuid)
                || legacyBuyers.existsByFarm_UuidAndCentreCodeIgnoreCase(farmUuid(), code)
                && !centre.getLegacyBuyer().getCentreCode().equalsIgnoreCase(code))
            throw new ConflictException("A collection centre with this code already exists in this farm.");

        MilkBuyer legacy = centre.getLegacyBuyer();
        legacy.setCompanyName(company.getCompanyName());
        legacy.setCentreName(request.centreName().trim());
        legacy.setCentreCode(code);
        legacy.setContactPerson(clean(request.contactPerson()));
        legacy.setPhoneNumber(clean(request.phoneNumber()));
        legacy.setStatus(request.status());
        legacyBuyers.save(legacy);
        centre.setCompany(company);
        centre.setCentreName(legacy.getCentreName());
        centre.setCentreCode(code);
        centre.setContactPerson(legacy.getContactPerson());
        centre.setPhoneNumber(legacy.getPhoneNumber());
        centre.setStatus(legacy.getStatus());
        return centreResponse(centres.save(centre));
    }

    @Transactional(readOnly = true)
    public List<ProcurementDtos.PricingResponse> pricing(UUID companyUuid) {
        company(companyUuid);
        return pricing.findAllByFarm_UuidAndCompany_UuidOrderByEffectiveFromDesc(farmUuid(), companyUuid).stream()
                .map(ProcurementDirectoryService::pricingResponse).toList();
    }

    @Transactional
    public ProcurementDtos.PricingResponse addPricing(UUID companyUuid, ProcurementDtos.PricingRequest request) {
        requireAdmin();
        Farm farm = lockedFarm();
        MilkBuyerCompany company = company(companyUuid);
        validatePricing(request);
        if (request.effectiveUntil() != null && request.effectiveUntil().isBefore(request.effectiveFrom()))
            throw new IllegalArgumentException("The pricing end date must be on or after its start date.");
        boolean overlaps = pricing.findAllByFarm_UuidAndCompany_UuidOrderByEffectiveFromDesc(farm.getUuid(), companyUuid)
                .stream().anyMatch(existing -> overlaps(request.effectiveFrom(), request.effectiveUntil(),
                        existing.getEffectiveFrom(), existing.getEffectiveUntil()));
        if (overlaps)
            throw new ConflictException("Pricing configuration periods for this company cannot overlap.");
        BuyerPricingConfiguration row = new BuyerPricingConfiguration();
        row.setFarm(farm);
        row.setCompany(company);
        row.setPricingMethod(request.pricingMethod());
        row.setEffectiveFrom(request.effectiveFrom());
        row.setEffectiveUntil(request.effectiveUntil());
        row.setBaseFatPercentage(request.baseFatPercentage());
        row.setBaseRate(request.baseRate());
        row.setFatRatePerPercentage(request.fatRatePerPercentage());
        row.setSnfRatePerPercentage(request.snfRatePerPercentage());
        row.setFixedRatePerLitre(request.fixedRatePerLitre());
        row.setNotes(clean(request.notes()));
        row.setCreatedBy(currentUser.get());
        return pricingResponse(pricing.save(row));
    }

    @Transactional(readOnly = true)
    public ProcurementDtos.PaymentScheduleResponse schedule(UUID companyUuid) {
        company(companyUuid);
        return schedules.findByCompany_UuidAndFarm_Uuid(companyUuid, farmUuid())
                .map(ProcurementDirectoryService::scheduleResponse).orElse(null);
    }

    @Transactional
    public ProcurementDtos.PaymentScheduleResponse saveSchedule(UUID companyUuid,
            ProcurementDtos.PaymentScheduleRequest request) {
        requireAdmin();
        Farm farm = lockedFarm();
        MilkBuyerCompany company = company(companyUuid);
        if (request.paymentCycle() == BuyerPaymentCycle.CUSTOM_DATE_RANGE
                && request.customPeriodDays() == null
                && (blank(request.periodStartRule()) || blank(request.periodEndRule())))
            throw new IllegalArgumentException("Custom schedules require a period duration or both period boundary rules.");
        BuyerPaymentSchedule row = schedules.findByCompany_UuidAndFarm_Uuid(companyUuid, farm.getUuid())
                .orElseGet(BuyerPaymentSchedule::new);
        row.setFarm(farm);
        row.setCompany(company);
        row.setPaymentCycle(request.paymentCycle());
        row.setCustomPeriodDays(request.customPeriodDays());
        row.setPeriodStartRule(clean(request.periodStartRule()));
        row.setPeriodEndRule(clean(request.periodEndRule()));
        row.setExpectedPaymentDay(request.expectedPaymentDay());
        row.setGracePeriodDays(request.gracePeriodDays());
        row.setNotes(clean(request.notes()));
        return scheduleResponse(schedules.save(row));
    }

    private void validatePricing(ProcurementDtos.PricingRequest r) {
        switch (r.pricingMethod()) {
            case FAT_SNF_RATE_CHART, MANUAL_RATE -> { }
            case FAT_BASED_RATE -> {
                if (r.baseFatPercentage() == null || r.baseRate() == null || r.fatRatePerPercentage() == null)
                    throw new IllegalArgumentException("FAT_BASED_RATE requires a base Fat, base rate, and Fat coefficient.");
            }
            case FAT_SNF_FORMULA -> {
                if (r.baseRate() == null || r.fatRatePerPercentage() == null || r.snfRatePerPercentage() == null)
                    throw new IllegalArgumentException("FAT_SNF_FORMULA requires a base rate and configured Fat/SNF coefficients.");
            }
            case DAILY_FIXED_RATE, CUSTOMER_NEGOTIATED_RATE -> {
                if (r.fixedRatePerLitre() == null)
                    throw new IllegalArgumentException(r.pricingMethod() + " requires a configured rate per litre.");
            }
        }
    }

    private MilkBuyerCompany company(UUID uuid) {
        return companies.findByUuidAndFarm_Uuid(uuid, farmUuid())
                .orElseThrow(() -> new NotFoundException("Milk buyer company not found in this farm."));
    }
    private Farm lockedFarm() {
        return farms.findLockedByUuid(farmUuid()).orElseThrow(() -> new NotFoundException("Farm not found."));
    }
    private UUID farmUuid() { return currentUser.farmUuid(); }
    private void requireAdmin() {
        if (!currentUser.isAdmin(currentUser.get()))
            throw new AccessDeniedException("Administrator access is required.");
    }
    private static void applyCompany(MilkBuyerCompany company, ProcurementDtos.CompanyRequest request) {
        company.setCompanyName(request.companyName().trim());
        company.setCompanyCode(request.companyCode().trim());
        company.setCompanyType(request.companyType());
        company.setContactPerson(clean(request.contactPerson()));
        company.setPhoneNumber(clean(request.phoneNumber()));
        company.setAddress(clean(request.address()));
        company.setGstNumber(clean(request.gstNumber()));
        company.setStatus(request.status());
        company.setNotes(clean(request.notes()));
    }
    private static ProcurementDtos.CompanyResponse companyResponse(MilkBuyerCompany c) {
        return new ProcurementDtos.CompanyResponse(c.getUuid(), c.getCompanyName(), c.getCompanyCode(),
                c.getCompanyType(), c.getContactPerson(), c.getPhoneNumber(), c.getAddress(), c.getGstNumber(),
                c.getStatus(), c.getNotes(), c.getCreatedAt());
    }
    private static ProcurementDtos.CentreResponse centreResponse(MilkCollectionCentre c) {
        return new ProcurementDtos.CentreResponse(c.getUuid(), c.getLegacyBuyer().getUuid(), c.getCompany().getUuid(),
                c.getCompany().getCompanyName(), c.getCentreName(), c.getCentreCode(), c.getContactPerson(),
                c.getPhoneNumber(), c.getStatus(), c.getCreatedAt());
    }
    private static ProcurementDtos.PricingResponse pricingResponse(BuyerPricingConfiguration p) {
        return new ProcurementDtos.PricingResponse(p.getUuid(), p.getCompany().getUuid(), p.getPricingMethod(),
                p.getEffectiveFrom(), p.getEffectiveUntil(), p.getBaseFatPercentage(), p.getBaseRate(),
                p.getFatRatePerPercentage(), p.getSnfRatePerPercentage(), p.getFixedRatePerLitre(),
                p.getNotes(), p.getCreatedAt());
    }
    private static ProcurementDtos.PaymentScheduleResponse scheduleResponse(BuyerPaymentSchedule s) {
        return new ProcurementDtos.PaymentScheduleResponse(s.getUuid(), s.getCompany().getUuid(), s.getPaymentCycle(),
                s.getCustomPeriodDays(), s.getPeriodStartRule(), s.getPeriodEndRule(), s.getExpectedPaymentDay(),
                s.getGracePeriodDays(), s.getNotes(), s.getUpdatedAt());
    }
    private static boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return (aEnd == null || !aEnd.isBefore(bStart)) && (bEnd == null || !bEnd.isBefore(aStart));
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
