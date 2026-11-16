package com.dairyfarm.service;

import com.dairyfarm.dto.MilkProcurementDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilkProcurementService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final MilkBuyerRepository buyers;
    private final AavinRateChartRepository charts;
    private final AavinRateChartAuditRepository chartAudits;
    private final AavinRateSlabRepository slabs;
    private final AavinMilkSupplyRepository supplies;
    private final AavinMilkTestRepository tests;
    private final AavinSupplyAuditRepository audits;
    private final AavinSettlementRepository settlements;
    private final AavinSettlementItemRepository settlementItems;
    private final AavinPaymentRepository payments;
    private final AavinAdjustmentRepository adjustments;
    private final MilkCollectionCentreRepository centres;
    private final BuyerPricingConfigurationRepository buyerPricing;
    private final BuyerPaymentScheduleRepository paymentSchedules;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    private final MilkStockService milkStock;
    private final Clock clock;

    public MilkProcurementService(MilkBuyerRepository buyers, AavinRateChartRepository charts, AavinRateChartAuditRepository chartAudits,
            AavinRateSlabRepository slabs,
            AavinMilkSupplyRepository supplies, AavinMilkTestRepository tests, AavinSupplyAuditRepository audits,
            AavinSettlementRepository settlements, AavinSettlementItemRepository settlementItems,
            AavinPaymentRepository payments, AavinAdjustmentRepository adjustments,
            MilkCollectionCentreRepository centres, BuyerPricingConfigurationRepository buyerPricing,
            BuyerPaymentScheduleRepository paymentSchedules, FarmRepository farms,
            CurrentUserService currentUser, MilkStockService milkStock, Clock clock) {
        this.buyers=buyers; this.charts=charts; this.chartAudits=chartAudits; this.slabs=slabs; this.supplies=supplies; this.tests=tests;
        this.audits=audits; this.settlements=settlements; this.settlementItems=settlementItems;
        this.payments=payments; this.adjustments=adjustments; this.farms=farms;
        this.centres=centres; this.buyerPricing=buyerPricing;
        this.paymentSchedules=paymentSchedules;
        this.currentUser=currentUser; this.milkStock=milkStock; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public List<MilkProcurementDtos.BuyerResponse> buyers() {
        return buyers.findAllByFarm_UuidOrderByCentreNameAsc(farmUuid()).stream().map(MilkProcurementService::buyerResponse).toList();
    }
    @Transactional
    public MilkProcurementDtos.BuyerResponse createBuyer(MilkProcurementDtos.BuyerRequest request) {
        requireManager();
        Farm farm=lockedFarm();
        if(buyers.existsByFarm_UuidAndCentreCodeIgnoreCase(farm.getUuid(),request.centreCode().trim()))
            throw new ConflictException("This collection-centre code already exists in this farm.");
        MilkBuyer buyer=new MilkBuyer(); buyer.setFarm(farm); buyer.setType(MilkBuyerType.AAVIN);
        applyBuyer(buyer,request); return buyerResponse(buyers.save(buyer));
    }
    @Transactional
    public MilkProcurementDtos.BuyerResponse updateBuyer(UUID uuid,MilkProcurementDtos.BuyerRequest request) {
        requireManager();
        Farm farm=lockedFarm();
        MilkBuyer buyer=buyer(uuid);
        if(!buyer.getCentreCode().equalsIgnoreCase(request.centreCode().trim())
                && buyers.existsByFarm_UuidAndCentreCodeIgnoreCase(farm.getUuid(),request.centreCode().trim()))
            throw new ConflictException("This collection-centre code already exists in this farm.");
        applyBuyer(buyer,request); return buyerResponse(buyers.save(buyer));
    }
    @Transactional(readOnly=true)
    public List<MilkProcurementDtos.RateChartResponse> rateCharts(UUID buyerUuid) {
        MilkBuyer buyer=buyer(buyerUuid);
        return charts.findAllByFarm_UuidAndBuyer_UuidOrderByEffectiveFromDesc(farmUuid(),buyer.getUuid())
                .stream().map(this::chartResponse).toList();
    }
    @Transactional
    public MilkProcurementDtos.RateChartResponse updateRateChartPeriod(UUID uuid,MilkProcurementDtos.RateChartPeriodRequest request) {
        requireAdmin();
        Farm farm=lockedFarm();
        AavinRateChart chart=charts.findByUuidAndFarm_Uuid(uuid,farm.getUuid())
                .orElseThrow(()->new NotFoundException("Buyer rate chart not found in this farm."));
        validPeriod(chart.getEffectiveFrom(),request.effectiveUntil());
        List<AavinRateChart> versions=charts.findAllByFarm_UuidAndBuyer_UuidOrderByEffectiveFromDesc(
                farm.getUuid(),chart.getBuyer().getUuid());
        boolean overlaps=versions.stream().filter(existing->!existing.getUuid().equals(chart.getUuid()))
                .anyMatch(existing->overlaps(chart.getEffectiveFrom(),request.effectiveUntil(),
                        existing.getEffectiveFrom(),existing.getEffectiveUntil()));
        if(overlaps) throw new ConflictException("The updated rate-chart period would overlap another version.");
        if(tests.existsByFarm_UuidAndRateChart_UuidAndSupply_SupplyDateGreaterThan(
                farm.getUuid(),chart.getUuid(),request.effectiveUntil()))
            throw new ConflictException("The chart period cannot be shortened past a supply already recorded with this version.");
        AavinRateChartAudit audit=new AavinRateChartAudit();
        audit.setFarm(farm); audit.setChart(chart); audit.setChangedBy(currentUser.get());
        audit.setReason(request.reason().trim()); audit.setPreviousEffectiveUntil(chart.getEffectiveUntil());
        audit.setNewEffectiveUntil(request.effectiveUntil());
        chart.setEffectiveUntil(request.effectiveUntil());
        chartAudits.save(audit);
        return chartResponse(charts.save(chart));
    }
    @Transactional(readOnly=true)
    public List<MilkProcurementDtos.RateChartAuditResponse> rateChartAudit(UUID uuid) {
        UUID farm=farmUuid();
        if(charts.findByUuidAndFarm_Uuid(uuid,farm).isEmpty())
            throw new NotFoundException("Buyer rate chart not found in this farm.");
        return chartAudits.findAllByFarm_UuidAndChart_UuidOrderByCreatedAtDesc(farm,uuid).stream()
                .map(a->new MilkProcurementDtos.RateChartAuditResponse(a.getUuid(),a.getPreviousEffectiveUntil(),
                        a.getNewEffectiveUntil(),a.getReason(),a.getChangedBy().getFullName(),a.getCreatedAt())).toList();
    }
    @Transactional
    public MilkProcurementDtos.RateChartResponse createRateChart(MilkProcurementDtos.RateChartRequest request) {
        requireAdmin();
        Farm farm=lockedFarm();
        MilkBuyer buyer=buyer(request.buyerUuid());
        validPeriod(request.effectiveFrom(),request.effectiveUntil());
        boolean overlaps=charts.findAllByFarm_UuidAndBuyer_UuidOrderByEffectiveFromDesc(farm.getUuid(),buyer.getUuid()).stream()
                .anyMatch(existing->overlaps(request.effectiveFrom(),request.effectiveUntil(),
                        existing.getEffectiveFrom(),existing.getEffectiveUntil()));
        if(overlaps) throw new ConflictException("Buyer rate-chart effective periods for this centre cannot overlap. Add a new version after closing the previous period.");
        List<MilkProcurementDtos.RateSlabRequest> requested=request.slabs();
        for(int i=0;i<requested.size();i++) for(int j=i+1;j<requested.size();j++)
            if(equal(requested.get(i).fatPercentage(),requested.get(j).fatPercentage())
                    && equal(requested.get(i).snfPercentage(),requested.get(j).snfPercentage()))
                throw new ConflictException("A rate chart cannot contain the same exact Fat/SNF combination twice.");
        AavinRateChart chart=new AavinRateChart(); chart.setFarm(farm); chart.setBuyer(buyer);
        chart.setName(request.name().trim()); chart.setEffectiveFrom(request.effectiveFrom());
        chart.setEffectiveUntil(request.effectiveUntil()); chart.setNotes(request.notes()); chart.setCreatedBy(currentUser.get());
        chart=charts.save(chart);
        List<AavinRateSlab> values=new ArrayList<>();
        for(MilkProcurementDtos.RateSlabRequest row:requested) {
            AavinRateSlab slab=new AavinRateSlab(); slab.setChart(chart);
            slab.setFatPercentage(row.fatPercentage()); slab.setSnfPercentage(row.snfPercentage());
            slab.setRatePerLitre(money(row.ratePerLitre())); values.add(slab);
        }
        slabs.saveAll(values);
        return chartResponse(chart);
    }

    @Transactional
    public MilkProcurementDtos.SupplyResponse createSupply(MilkProcurementDtos.SupplyRequest request) {
        Farm farm=lockedFarm();
        MilkBuyer buyer=buyer(request.buyerUuid());
        if(buyer.getStatus()!=MilkBuyerStatus.ACTIVE) throw new ConflictException("Supplies cannot be added to an inactive buyer centre.");
        if(request.supplyDate().isAfter(today())) throw new IllegalArgumentException("Supply date cannot be in the future.");
        BigDecimal accepted=request.acceptedQuantity();
        BigDecimal rejected=request.rejectedQuantity();
        if(accepted!=null||rejected!=null) {
            accepted=accepted==null?request.quantity().subtract(rejected==null?ZERO:rejected):accepted;
            rejected=rejected==null?request.quantity().subtract(accepted):rejected;
            if(accepted.add(rejected).compareTo(request.quantity())!=0)
                throw new IllegalArgumentException("Accepted and rejected quantities must add up to the supplied quantity.");
            if(rejected.signum()>0&&(request.rejectionReason()==null||request.rejectionReason().isBlank()))
                throw new IllegalArgumentException("Provide a reason for rejected milk.");
        } else {
            accepted=null;
            rejected=ZERO;
        }
        if(request.receiptNumber()!=null && !request.receiptNumber().isBlank()
                && supplies.existsByFarm_UuidAndBuyer_UuidAndReceiptNumber(farm.getUuid(),buyer.getUuid(),request.receiptNumber().trim()))
            throw new ConflictException("This buyer receipt number has already been recorded.");
        BigDecimal companyFat = request.buyerFat() == null ? request.farmFat() : request.buyerFat();
        BigDecimal companySnf = request.buyerSnf() == null ? request.farmSnf() : request.buyerSnf();
        RateMatch match=findRate(buyer,request.supplyDate(),companyFat,companySnf);
        AavinMilkSupply supply=new AavinMilkSupply(); supply.setFarm(farm); supply.setBuyer(buyer);
        supply.setSupplyDate(request.supplyDate()); supply.setSession(request.session());
        supply.setQuantity(money(request.quantity())); supply.setReceiptNumber(clean(request.receiptNumber()));
        supply.setRejectedQuantity(money(rejected));
        supply.setRejectionReason(clean(request.rejectionReason()));
        supply.setNotes(request.notes()); supply.setRecordedBy(currentUser.get());
        supply=supplies.save(supply);
        AavinMilkTest test=new AavinMilkTest(); test.setFarm(farm); test.setSupply(supply);
        // Legacy farmFat/farmSnf fields are accepted as company-reported readings for old clients.
        // New clients send buyerFat/buyerSnf; no on-farm quality test is recorded.
        test.setFarmFat(null); test.setFarmSnf(null); test.setDensity(request.density());
        test.setAavinFat(companyFat); test.setAavinSnf(companySnf);
        test.setAcceptedQuantity(accepted==null?null:money(accepted));
        test.setSampleReference(clean(request.sampleReference())); test.setRateChart(match.chart());
        test.setRateSlab(match.slab()); test.setMatchedRate(match.rate());
        test.setStatus(match.rate()==null?AavinVerificationStatus.RATE_UNAVAILABLE:AavinVerificationStatus.PENDING_VERIFICATION);
        test.setVerifiedBy(currentUser.get()); tests.save(test);
        return supplyResponse(supply,test);
    }
    @Transactional(readOnly=true)
    public Page<MilkProcurementDtos.SupplyResponse> supplies(LocalDate from,LocalDate to,UUID buyerUuid,int page,int size) {
        range(from,to); UUID farm=farmUuid();
        Page<AavinMilkSupply> result=buyerUuid==null
                ?supplies.findAllByFarm_UuidAndSupplyDateBetween(farm,from,to,page(page,size))
                :supplies.findAllByFarm_UuidAndBuyer_UuidAndSupplyDateBetween(farm,buyerUuid,from,to,page(page,size));
        return result.map(row->supplyResponse(row,test(row.getUuid())));
    }
    @Transactional(readOnly=true)
    public MilkProcurementDtos.SupplyResponse supply(UUID uuid) {
        AavinMilkSupply supply=findSupply(uuid);
        return supplyResponse(supply,test(uuid));
    }
    @Transactional
    public MilkProcurementDtos.SupplyResponse verifySupply(UUID uuid,MilkProcurementDtos.SupplyVerificationRequest request) {
        requireVerifier();
        Farm farm=lockedFarm();
        AavinMilkSupply supply=supplies.findByUuidAndFarm_Uuid(uuid,farm.getUuid())
                .orElseThrow(()->new NotFoundException("Milk supply not found."));
        if(supply.getSettlement()!=null)
            throw new ConflictException("A supply included in a settlement cannot be changed.");
        AavinMilkTest test=test(uuid);
        if(request.acceptedQuantity()!=null && request.acceptedQuantity().compareTo(supply.getQuantity())>0)
            throw new IllegalArgumentException("Accepted quantity cannot exceed the supplied quantity.");
        if(request.receiptNumber()!=null && !request.receiptNumber().isBlank()
                && supplies.existsByFarm_UuidAndBuyer_UuidAndReceiptNumberAndUuidNot(farm.getUuid(),supply.getBuyer().getUuid(),
                        request.receiptNumber().trim(),supply.getUuid()))
            throw new ConflictException("This receipt number is already assigned to another supply.");
        BigDecimal verifiedRate = request.aavinRate() == null
                ? (request.status() == AavinVerificationStatus.VERIFIED ? effectiveEstimateRate(test) : null)
                : money(request.aavinRate());
        if(request.status()==AavinVerificationStatus.VERIFIED
                && (verifiedRate==null || request.acceptedQuantity()==null))
            throw new IllegalArgumentException("Verification requires an accepted quantity and an applicable or reported rate.");
        if(request.status()==AavinVerificationStatus.RATE_UNAVAILABLE)
            throw new IllegalArgumentException("Rate-unavailable status is assigned automatically when no exact chart combination exists.");
        AavinSupplyAudit audit=new AavinSupplyAudit(); audit.setFarm(farm); audit.setSupply(supply);
        audit.setChangedBy(currentUser.get()); audit.setReason(request.reason().trim());
        audit.setPreviousRate(test.getAavinRate()!=null?test.getAavinRate():effectiveEstimateRate(test));
        audit.setPreviousAavinFat(test.getAavinFat()); audit.setPreviousAavinSnf(test.getAavinSnf());
        audit.setPreviousAcceptedQuantity(test.getAcceptedQuantity()); audit.setPreviousStatus(test.getStatus().name());
        if(request.aavinFat()!=null) test.setAavinFat(request.aavinFat());
        if(request.aavinSnf()!=null) test.setAavinSnf(request.aavinSnf());
        test.setAavinRate(verifiedRate);
        test.setAcceptedQuantity(request.acceptedQuantity()==null?null:money(request.acceptedQuantity()));
        BigDecimal previousStockMovement = milkStock.supplyMovement(farm.getUuid(), supply.getUuid());
        BigDecimal targetStockMovement = request.status() == AavinVerificationStatus.VERIFIED
                ? money(request.acceptedQuantity()).negate() : previousStockMovement;
        BigDecimal stockChange = targetStockMovement.subtract(previousStockMovement);
        if (stockChange.signum() != 0) {
            MilkStockTransactionType movementType = stockChange.signum() < 0
                    ? (previousStockMovement.signum() == 0
                        ? MilkStockTransactionType.COMPANY_SUPPLY_OUT : MilkStockTransactionType.REVERSAL_OUT)
                    : (previousStockMovement.signum() == 0
                        ? MilkStockTransactionType.RETURN_IN : MilkStockTransactionType.REVERSAL_IN);
            milkStock.record(farm, movementType, stockChange, null, null, null, null, supply,
                    "Company supply stock confirmation · " + request.reason().trim());
        }
        if(request.receiptNumber()!=null) supply.setReceiptNumber(clean(request.receiptNumber()));
        test.setStatus(request.status()); test.setVerificationNotes(request.reason().trim()); test.setVerifiedBy(currentUser.get());
        tests.save(test); supplies.save(supply);
        audit.setNewRate(test.getAavinRate()!=null?test.getAavinRate():effectiveEstimateRate(test));
        audit.setNewAavinFat(test.getAavinFat()); audit.setNewAavinSnf(test.getAavinSnf());
        audit.setNewAcceptedQuantity(test.getAcceptedQuantity()); audit.setNewStatus(test.getStatus().name()); audits.save(audit);
        return supplyResponse(supply,test);
    }
    @Transactional
    public MilkProcurementDtos.SupplyResponse overrideSupplyRate(UUID uuid,MilkProcurementDtos.SupplyRateOverrideRequest request) {
        requireAdmin();
        Farm farm=lockedFarm();
        AavinMilkSupply supply=supplies.findByUuidAndFarm_Uuid(uuid,farm.getUuid())
                .orElseThrow(()->new NotFoundException("Milk supply not found."));
        if(supply.getSettlement()!=null)
            throw new ConflictException("A supply included in a settlement cannot be changed.");
        AavinMilkTest test=test(uuid);
        AavinSupplyAudit audit=new AavinSupplyAudit(); audit.setFarm(farm); audit.setSupply(supply);
        audit.setChangedBy(currentUser.get()); audit.setReason(request.reason().trim());
        audit.setPreviousRate(effectiveEstimateRate(test)); audit.setNewRate(money(request.ratePerLitre()));
        audit.setPreviousStatus(test.getStatus().name()); audit.setNewStatus(test.getStatus().name());
        test.setManualRate(money(request.ratePerLitre())); test.setVerifiedBy(currentUser.get()); tests.save(test);
        audits.save(audit);
        return supplyResponse(supply,test);
    }
    @Transactional(readOnly=true)
    public List<MilkProcurementDtos.SupplyAuditResponse> supplyAudit(UUID uuid) {
        findSupply(uuid);
        return audits.findAllByFarm_UuidAndSupply_UuidOrderByCreatedAtDesc(farmUuid(),uuid).stream()
                .map(a->new MilkProcurementDtos.SupplyAuditResponse(a.getUuid(),a.getReason(),a.getPreviousRate(),a.getNewRate(),
                        a.getPreviousStatus(),a.getNewStatus(),a.getChangedBy().getFullName(),a.getCreatedAt())).toList();
    }

    @Transactional
    public MilkProcurementDtos.SettlementResponse createSettlement(MilkProcurementDtos.SettlementRequest request) {
        requireSettlementManager();
        Farm farm=lockedFarm(); MilkBuyer buyer=buyer(request.buyerUuid());
        validPeriod(request.periodStart(),request.periodEnd());
        if(settlements.existsByFarm_UuidAndBuyer_UuidAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                farm.getUuid(),buyer.getUuid(),request.periodEnd(),request.periodStart()))
            throw new ConflictException("This settlement period overlaps an existing settlement for the buyer centre.");
        List<AavinMilkSupply> candidates=supplies.findAllByFarm_UuidAndSupplyDateBetweenOrderBySupplyDateAscSessionAsc(
                farm.getUuid(),request.periodStart(),request.periodEnd()).stream()
                .filter(row->row.getBuyer().getUuid().equals(buyer.getUuid()))
                .filter(row->row.getSettlement()==null && !settlementItems.existsByFarm_UuidAndSupply_Uuid(farm.getUuid(),row.getUuid()))
                .filter(row->eligibleForSettlement(test(row.getUuid())))
                .toList();
        if(candidates.isEmpty()) throw new ConflictException("No verified, priced and un-settled buyer supplies are available in this period.");
        String number=request.settlementNumber().trim();
        if(settlements.existsByFarm_UuidAndBuyer_UuidAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                farm.getUuid(),buyer.getUuid(),request.periodEnd(),request.periodStart()))
            throw new ConflictException("Settlement period already exists.");
        AavinSettlement settlement=new AavinSettlement(); settlement.setFarm(farm); settlement.setBuyer(buyer);
        settlement.setSettlementNumber(number); settlement.setPeriodStart(request.periodStart());
        settlement.setPeriodEnd(request.periodEnd()); settlement.setCreatedBy(currentUser.get());
        settlement.setTotalQuantity(ZERO); settlement.setTotalAcceptedQuantity(ZERO);
        settlement.setGrossMilkValue(ZERO); settlement.setDeductions(ZERO); settlement.setBonuses(ZERO);
        settlement.setAdjustments(ZERO); settlement.setNetPayable(ZERO);
        settlement=settlements.saveAndFlush(settlement);
        BigDecimal total=ZERO,accepted=ZERO,gross=ZERO;
        for(AavinMilkSupply supply:candidates) {
            AavinMilkTest test=test(supply.getUuid());
            BigDecimal qty=money(test.getAcceptedQuantity()),rate=finalRate(test);
            BigDecimal value=money(qty.multiply(rate));
            AavinSettlementItem item=new AavinSettlementItem(); item.setFarm(farm); item.setSettlement(settlement);
            item.setSupply(supply); item.setAcceptedQuantity(qty); item.setAppliedRate(rate); item.setMilkValue(value);
            settlementItems.save(item); supply.setSettlement(settlement); supplies.save(supply);
            total=total.add(supply.getQuantity()); accepted=accepted.add(qty); gross=gross.add(value);
        }
        settlement.setTotalQuantity(money(total)); settlement.setTotalAcceptedQuantity(money(accepted));
        settlement.setGrossMilkValue(money(gross)); settlement.setNetPayable(money(gross));
        return settlementResponse(settlements.save(settlement));
    }
    @Transactional(readOnly=true)
    public Page<MilkProcurementDtos.SettlementResponse> settlements(int page,int size) {
        return settlements.findAllByFarm_UuidOrderByPeriodStartDesc(farmUuid(),page(page,size))
                .map(this::settlementResponse);
    }
    @Transactional(readOnly=true)
    public MilkProcurementDtos.SettlementResponse settlement(UUID uuid) { return settlementResponse(findSettlement(uuid)); }
    @Transactional
    public MilkProcurementDtos.SettlementResponse addAdjustment(UUID settlementUuid,MilkProcurementDtos.AdjustmentRequest request) {
        requireSettlementManager();         Farm farm=lockedFarm(); AavinSettlement settlement=findSettlement(settlementUuid);
        if(settlement.getStatus()==AavinSettlementStatus.DISPUTED || settlement.getStatus()==AavinSettlementStatus.PAID)
            throw new ConflictException("Adjustments cannot be added to disputed or fully paid settlements.");
        if((request.type()==AavinAdjustmentType.BONUS || request.type()==AavinAdjustmentType.DEDUCTION)
                && request.amount().signum()<0)
            throw new IllegalArgumentException("Bonus and deduction amounts must be positive; use ADJUSTMENT for a signed amount.");
        if(payments.totalForSettlement(farm.getUuid(),settlement.getUuid()).signum()>0)
            throw new ConflictException("Do not change a settlement after a payment has been recorded.");
        AavinAdjustment adjustment=new AavinAdjustment(); adjustment.setFarm(farm); adjustment.setSettlement(settlement);
        adjustment.setType(request.type()); adjustment.setAmount(money(request.amount()));
        adjustment.setDescription(request.description().trim()); adjustment.setRecordedBy(currentUser.get());
        adjustments.save(adjustment); recomputeSettlement(settlement);
        return settlementResponse(settlements.save(settlement));
    }
    @Transactional
    public MilkProcurementDtos.SettlementResponse addPayment(UUID settlementUuid,MilkProcurementDtos.PaymentRequest request) {
        requireSettlementManager(); Farm farm=lockedFarm(); AavinSettlement settlement=findSettlement(settlementUuid);
        if(settlement.getStatus()==AavinSettlementStatus.DISPUTED)
            throw new ConflictException("Payments cannot be recorded while the settlement is disputed.");
        BigDecimal received=payments.totalForSettlement(farm.getUuid(),settlement.getUuid());
        BigDecimal amount=money(request.amount());
        if(received.add(amount).compareTo(settlement.getNetPayable())>0)
            throw new ConflictException("Payment exceeds the settlement's remaining balance.");
        AavinPayment payment=new AavinPayment(); payment.setFarm(farm); payment.setSettlement(settlement);
        payment.setPaymentDate(request.paymentDate()); payment.setAmount(amount);
        payment.setPaymentReference(clean(request.paymentReference())); payment.setNotes(request.notes());
        payment.setRecordedBy(currentUser.get()); payments.save(payment); recomputeSettlement(settlement);
        return settlementResponse(settlements.save(settlement));
    }
    @Transactional
    public MilkProcurementDtos.SettlementResponse settlementStatus(UUID uuid,MilkProcurementDtos.SettlementStateRequest request) {
        requireSettlementManager(); lockedFarm(); AavinSettlement settlement=findSettlement(uuid);
        if(Boolean.TRUE.equals(request.disputed())) {
            if(request.reason()==null || request.reason().isBlank())
                throw new IllegalArgumentException("A reason is required to dispute a settlement.");
            settlement.setStatus(AavinSettlementStatus.DISPUTED); settlement.setStatusReason(request.reason().trim());
        } else {
            settlement.setStatusReason(null); recomputeSettlement(settlement);
        }
        return settlementResponse(settlements.save(settlement));
    }

    @Transactional(readOnly=true)
    public MilkProcurementDtos.DashboardResponse dashboard() {
        UUID farm=farmUuid(); LocalDate date=today();
        List<AavinMilkSupply> todayRows=supplies.findAllByFarm_UuidAndSupplyDate(farm,date);
        BigDecimal volume=ZERO,fat=ZERO,snf=ZERO,value=ZERO; int fatMeasured=0,snfMeasured=0;
        for(AavinMilkSupply row:todayRows) {
            volume=volume.add(row.getQuantity()); AavinMilkTest test=test(row.getUuid());
            if(test.getAavinFat()!=null) { fat=fat.add(test.getAavinFat()); fatMeasured++; }
            if(test.getAavinSnf()!=null) { snf=snf.add(test.getAavinSnf()); snfMeasured++; }
            BigDecimal rate=effectiveEstimateRate(test);
            if(test.getStatus()==AavinVerificationStatus.VERIFIED && test.getAcceptedQuantity()!=null
                    && finalRate(test)!=null)
                value=value.add(test.getAcceptedQuantity().multiply(finalRate(test)));
            else if(rate!=null) value=value.add(row.getQuantity().multiply(rate));
        }
        List<MilkBuyer> active=buyers.findAllByFarm_UuidAndStatus(farm,MilkBuyerStatus.ACTIVE);
        BigDecimal cycleAmount=ZERO; LocalDate next=null;
        for(MilkBuyer buyer:active) {
            List<AavinSettlement> history=settlements.findAllByFarm_UuidAndBuyer_UuidOrderByPeriodEndDesc(farm,buyer.getUuid());
            LocalDate cycleStart=history.isEmpty()?null:history.get(0).getPeriodEnd().plusDays(1);
            if(cycleStart!=null) {
                LocalDate expected=expectedPaymentDate(cycleStart,buyer);
                if(expected!=null) next=next==null||expected.isBefore(next)?expected:next;
            }
            List<AavinMilkSupply> eligible=cycleStart==null
                    ?supplies.findAllByFarm_Uuid(farm)
                    :supplies.findAllByFarm_UuidAndSupplyDateBetweenOrderBySupplyDateAscSessionAsc(farm,cycleStart,date);
            for(AavinMilkSupply supply:eligible) {
                if(!supply.getBuyer().getUuid().equals(buyer.getUuid())) continue;
                if(supply.getSettlement()!=null || supply.getSupplyDate().isAfter(date)) continue;
                AavinMilkTest test=test(supply.getUuid());
                if(eligibleForSettlement(test))
                    cycleAmount=cycleAmount.add(test.getAcceptedQuantity().multiply(finalRate(test)));
            }
        }
        BigDecimal pending=ZERO;
        for(AavinSettlement s:settlements.findAllByFarm_Uuid(farm))
            if(s.getStatus()!=AavinSettlementStatus.PAID)
                pending=pending.add(s.getNetPayable().subtract(payments.totalForSettlement(farm,s.getUuid())).max(ZERO));
        LocalDate last=payments.findFirstByFarm_UuidOrderByPaymentDateDescCreatedAtDesc(farm).map(AavinPayment::getPaymentDate).orElse(null);
        return new MilkProcurementDtos.DashboardResponse(date,money(volume),fatMeasured==0?null:money(fat.divide(BigDecimal.valueOf(fatMeasured),2,RoundingMode.HALF_UP)),
                snfMeasured==0?null:money(snf.divide(BigDecimal.valueOf(snfMeasured),2,RoundingMode.HALF_UP)),
                money(value),money(cycleAmount),next,money(pending),last);
    }

    @Transactional(readOnly=true)
    public List<MilkProcurementDtos.ReportRow> report(String type,LocalDate from,LocalDate to) {
        range(from,to); UUID farm=farmUuid(); List<MilkProcurementDtos.ReportRow> rows=new ArrayList<>();
        if(List.of("supplies","sessions","quality","values","comparison").contains(type)) {
            supplies.findAllByFarm_UuidAndSupplyDateBetweenOrderBySupplyDateAscSessionAsc(farm,from,to).forEach(s->{
                AavinMilkTest t=test(s.getUuid()); BigDecimal rate=effectiveEstimateRate(t);
                String category=switch(type) { case "sessions"->s.getSession().name(); case "quality"->"FAT_SNF"; case "values"->"MILK_VALUE"; case "comparison"->"FARM_AAVIN"; default->"SUPPLY"; };
                BigDecimal amount=rate==null?null:t.getStatus()==AavinVerificationStatus.VERIFIED
                        && t.getAcceptedQuantity()!=null?money(t.getAcceptedQuantity().multiply(rate))
                        :money(s.getQuantity().multiply(rate));
                rows.add(new MilkProcurementDtos.ReportRow(s.getSupplyDate(),category,s.getBuyer().getCentreName(),s.getSession().name(),
                        s.getQuantity(),type.equals("comparison")?difference(t.getFarmFat(),t.getAavinFat()):t.getFarmFat(),
                        type.equals("comparison")?difference(t.getFarmSnf(),t.getAavinSnf()):t.getFarmSnf(),rate,
                        amount,t.getStatus().name(),s.getReceiptNumber()));
            });
        } else if(type.equals("payments")) {
            payments.findAllByFarm_UuidAndPaymentDateBetweenOrderByPaymentDateAscCreatedAtAsc(farm,from,to)
                    .forEach(payment->rows.add(new MilkProcurementDtos.ReportRow(payment.getPaymentDate(),"PAYMENT",
                            payment.getSettlement().getBuyer().getCentreName(),"",null,null,null,null,payment.getAmount(),
                            payment.getSettlement().getStatus().name(),payment.getSettlement().getSettlementNumber()
                                    +(payment.getPaymentReference()==null?"":" "+payment.getPaymentReference()))));
        } else if(List.of("settlements","pending").contains(type)) {
            settlements.findAllByFarm_Uuid(farm).stream()
                    .filter(s->!s.getPeriodEnd().isBefore(from)&&!s.getPeriodStart().isAfter(to))
                    .forEach(s->{
                        BigDecimal received=payments.totalForSettlement(farm,s.getUuid());
                        BigDecimal outstanding=s.getNetPayable().subtract(received).max(ZERO);
                        if(type.equals("pending")&&outstanding.signum()==0) return;
                        rows.add(new MilkProcurementDtos.ReportRow(s.getPeriodEnd(),type.toUpperCase(),s.getBuyer().getCentreName(),"",
                                s.getTotalAcceptedQuantity(),null,null,null,s.getNetPayable(),s.getStatus().name(),
                                s.getSettlementNumber()+" received "+received+" outstanding "+outstanding));
                    });
        } else throw new IllegalArgumentException("Report type must be supplies, sessions, quality, values, settlements, payments, pending, or comparison.");
        return rows;
    }

    private RateMatch findRate(MilkBuyer buyer,LocalDate date,BigDecimal fat,BigDecimal snf) {
        MilkCollectionCentre centre=centres.findByLegacyBuyer_UuidAndFarm_Uuid(buyer.getUuid(),farmUuid()).orElse(null);
        if(centre!=null) {
            List<BuyerPricingConfiguration> configurations=buyerPricing
                    .findAllByFarm_UuidAndCompany_UuidAndEffectiveFromLessThanEqual(
                            farmUuid(),centre.getCompany().getUuid(),date).stream()
                    .filter(c->c.getEffectiveUntil()==null||!c.getEffectiveUntil().isBefore(date)).toList();
            if(configurations.size()>1)
                throw new ConflictException("Multiple pricing configurations apply on this date; correct the overlapping periods.");
            if(!configurations.isEmpty()) {
                BuyerPricingConfiguration configuration=configurations.get(0);
                BigDecimal configuredRate=switch(configuration.getPricingMethod()) {
                    case FAT_BASED_RATE -> fat == null ? null : configuration.getBaseRate()
                            .add(fat.subtract(configuration.getBaseFatPercentage())
                            .multiply(configuration.getFatRatePerPercentage()));
                    case FAT_SNF_FORMULA -> fat == null || snf == null ? null : configuration.getBaseRate()
                            .add(fat.multiply(configuration.getFatRatePerPercentage()))
                            .add(snf.multiply(configuration.getSnfRatePerPercentage()));
                    case DAILY_FIXED_RATE, CUSTOMER_NEGOTIATED_RATE -> configuration.getFixedRatePerLitre();
                    case MANUAL_RATE -> null;
                    case FAT_SNF_RATE_CHART -> null;
                };
                if(configuration.getPricingMethod()!=BuyerPricingMethod.FAT_SNF_RATE_CHART)
                    return new RateMatch(null,null,configuredRate==null||configuredRate.signum()<0?null:money(configuredRate));
            } else if(centre.getCompany().getCompanyType()!=MilkBuyerCompanyType.COOPERATIVE
                    && centre.getCompany().getCompanyType()!=MilkBuyerCompanyType.DAIRY_COMPANY) {
                return new RateMatch(null,null,null);
            }
        }
        List<AavinRateChart> matchingCharts=charts.findAllByFarm_UuidAndBuyer_UuidAndEffectiveFromLessThanEqual(
                        farmUuid(),buyer.getUuid(),date).stream()
                .filter(c->c.getEffectiveUntil()==null||!c.getEffectiveUntil().isBefore(date)).toList();
        if(matchingCharts.size()>1) throw new ConflictException("Multiple buyer rate charts apply on this date; correct the overlapping chart periods.");
        if(matchingCharts.isEmpty()) return new RateMatch(null,null,null);
        AavinRateChart chart=matchingCharts.get(0);
        if(fat==null||snf==null) return new RateMatch(chart,null,null);
        AavinRateSlab slab=slabs.findByChart_UuidAndFatPercentageAndSnfPercentage(chart.getUuid(),fat,snf).orElse(null);
        return new RateMatch(chart,slab,slab==null?null:slab.getRatePerLitre());
    }
    private LocalDate expectedPaymentDate(LocalDate periodStart, MilkBuyer buyer) {
        MilkCollectionCentre centre=centres.findByLegacyBuyer_UuidAndFarm_Uuid(buyer.getUuid(),farmUuid()).orElse(null);
        BuyerPaymentSchedule schedule=centre==null?null:paymentSchedules
                .findByCompany_UuidAndFarm_Uuid(centre.getCompany().getUuid(),farmUuid()).orElse(null);
        if(schedule==null) return periodStart.plusDays(Math.max(1,buyer.getPaymentCycleDays())-1L);
        LocalDate periodEnd=switch(schedule.getPaymentCycle()) {
            case DAILY -> periodStart;
            case WEEKLY -> periodStart.plusDays(6);
            case EVERY_15_DAYS -> periodStart.plusDays(14);
            case MONTHLY -> periodStart.plusMonths(1).minusDays(1);
            case CUSTOM_DATE_RANGE -> schedule.getCustomPeriodDays()==null?null:
                    periodStart.plusDays(schedule.getCustomPeriodDays()-1L);
        };
        if(periodEnd==null) return null;
        periodEnd=periodEnd.plusDays(schedule.getGracePeriodDays());
        if(schedule.getExpectedPaymentDay()!=null && schedule.getPaymentCycle()==BuyerPaymentCycle.MONTHLY) {
            int day=Math.min(schedule.getExpectedPaymentDay(),periodEnd.lengthOfMonth());
            periodEnd=periodEnd.withDayOfMonth(day);
            if(periodEnd.isBefore(periodStart.plusMonths(1).minusDays(1)))
                periodEnd=periodEnd.plusMonths(1).withDayOfMonth(
                        Math.min(schedule.getExpectedPaymentDay(),periodEnd.plusMonths(1).lengthOfMonth()));
        }
        return periodEnd;
    }
    private void recomputeSettlement(AavinSettlement settlement) {
        UUID farm=settlement.getFarm().getUuid();
        List<AavinAdjustment> rows=adjustments.findAllByFarm_UuidAndSettlement_Uuid(farm,settlement.getUuid());
        BigDecimal deduction=ZERO,bonus=ZERO,other=ZERO;
        for(AavinAdjustment row:rows) switch(row.getType()) {
            case DEDUCTION -> deduction=deduction.add(row.getAmount());
            case BONUS -> bonus=bonus.add(row.getAmount());
            case ADJUSTMENT -> other=other.add(row.getAmount());
        }
        BigDecimal net=settlement.getGrossMilkValue().add(bonus).subtract(deduction).add(other);
        if(net.signum()<0) throw new IllegalArgumentException("Adjustments cannot make net payable negative.");
        settlement.setDeductions(money(deduction)); settlement.setBonuses(money(bonus));
        settlement.setAdjustments(money(other)); settlement.setNetPayable(money(net));
        BigDecimal received=payments.totalForSettlement(farm,settlement.getUuid());
        if(settlement.getStatus()!=AavinSettlementStatus.DISPUTED)
            settlement.setStatus(received.compareTo(net)>=0?AavinSettlementStatus.PAID:
                    received.signum()>0?AavinSettlementStatus.PARTIALLY_PAID:AavinSettlementStatus.PENDING);
    }
    private boolean eligibleForSettlement(AavinMilkTest t) {
        return t.getStatus()==AavinVerificationStatus.VERIFIED && finalRate(t)!=null && t.getAcceptedQuantity()!=null;
    }
    private MilkProcurementDtos.SettlementResponse settlementResponse(AavinSettlement s) {
        UUID farm=s.getFarm().getUuid(); List<MilkProcurementDtos.SettlementItemResponse> items=settlementItems
                .findAllByFarm_UuidAndSettlement_UuidOrderBySupply_SupplyDateAsc(farm,s.getUuid()).stream()
                .map(i->new MilkProcurementDtos.SettlementItemResponse(i.getSupply().getUuid(),i.getSupply().getSupplyDate(),
                        i.getSupply().getSession(),i.getSupply().getQuantity(),i.getAcceptedQuantity(),
                        i.getAppliedRate(),i.getMilkValue(),i.getSupply().getReceiptNumber())).toList();
        List<MilkProcurementDtos.PaymentResponse> paymentRows=payments
                .findAllByFarm_UuidAndSettlement_UuidOrderByPaymentDateAscCreatedAtAsc(farm,s.getUuid()).stream()
                .map(p->new MilkProcurementDtos.PaymentResponse(p.getUuid(),p.getPaymentDate(),p.getAmount(),
                        p.getPaymentReference(),p.getNotes(),p.getRecordedBy().getFullName(),p.getCreatedAt())).toList();
        List<MilkProcurementDtos.AdjustmentResponse> adjustmentRows=adjustments.findAllByFarm_UuidAndSettlement_Uuid(farm,s.getUuid()).stream()
                .map(a->new MilkProcurementDtos.AdjustmentResponse(a.getUuid(),a.getType(),a.getAmount(),a.getDescription(),
                        a.getRecordedBy().getFullName(),a.getCreatedAt())).toList();
        BigDecimal received=payments.totalForSettlement(farm,s.getUuid());
        return new MilkProcurementDtos.SettlementResponse(s.getUuid(),s.getBuyer().getUuid(),s.getBuyer().getCentreName(),
                s.getSettlementNumber(),s.getPeriodStart(),s.getPeriodEnd(),s.getTotalQuantity(),
                s.getTotalAcceptedQuantity(),s.getGrossMilkValue(),s.getDeductions(),s.getBonuses(),s.getAdjustments(),
                s.getNetPayable(),received,s.getNetPayable().subtract(received).max(ZERO),s.getStatus(),s.getStatusReason(),
                items,adjustmentRows,paymentRows,s.getCreatedAt());
    }
    private MilkProcurementDtos.SupplyResponse supplyResponse(AavinMilkSupply s,AavinMilkTest t) {
        BigDecimal estimateRate=effectiveEstimateRate(t);
        BigDecimal appliedRate=finalRate(t);
        boolean estimate=t.getStatus()!=AavinVerificationStatus.VERIFIED||appliedRate==null||t.getAcceptedQuantity()==null;
        BigDecimal estimated=estimateRate==null?null:money(s.getQuantity().multiply(estimateRate));
        BigDecimal finalValue=estimate?null:money(t.getAcceptedQuantity().multiply(appliedRate));
        return new MilkProcurementDtos.SupplyResponse(s.getUuid(),s.getBuyer().getUuid(),s.getBuyer().getCompanyName(),
                s.getBuyer().getCentreName(),s.getSupplyDate(),s.getSession(),s.getQuantity(),t.getFarmFat(),t.getFarmSnf(),
                t.getDensity(),t.getSampleReference(),s.getReceiptNumber(),t.getAavinFat(),t.getAavinSnf(),
                difference(t.getFarmFat(),t.getAavinFat()),difference(t.getFarmSnf(),t.getAavinSnf()),t.getAcceptedQuantity(),
                s.getRejectedQuantity(),s.getRejectionReason(),
                t.getMatchedRate(),t.getManualRate(),t.getAavinRate(),estimateRate,estimated,finalValue,estimate,
                t.getRateChart()==null?null:t.getRateChart().getUuid(),t.getRateChart()==null?null:t.getRateChart().getName(),
                t.getStatus(),s.getNotes(),t.getVerificationNotes(),s.getSettlement()==null?null:s.getSettlement().getUuid(),s.getCreatedAt());
    }
    private MilkProcurementDtos.RateChartResponse chartResponse(AavinRateChart chart) {
        return new MilkProcurementDtos.RateChartResponse(chart.getUuid(),chart.getBuyer().getUuid(),chart.getBuyer().getCentreName(),
                chart.getName(),chart.getEffectiveFrom(),chart.getEffectiveUntil(),chart.getNotes(),chart.getCreatedBy().getFullName(),
                chart.getCreatedAt(),slabs.findAllByChart_UuidOrderByFatPercentageAscSnfPercentageAsc(chart.getUuid()).stream()
                .map(s->new MilkProcurementDtos.RateSlabResponse(s.getUuid(),s.getFatPercentage(),s.getSnfPercentage(),s.getRatePerLitre())).toList());
    }
    private static MilkProcurementDtos.BuyerResponse buyerResponse(MilkBuyer b) {
        return new MilkProcurementDtos.BuyerResponse(b.getUuid(),b.getType().name(),b.getCompanyName(),b.getCentreName(),
                b.getCentreCode(),b.getContactPerson(),b.getPhoneNumber(),b.getPaymentCycleDays(),b.getStatus());
    }
    private void applyBuyer(MilkBuyer b,MilkProcurementDtos.BuyerRequest r) {
        b.setCompanyName(r.companyName().trim()); b.setCentreName(r.centreName().trim()); b.setCentreCode(r.centreCode().trim());
        b.setContactPerson(clean(r.contactPerson())); b.setPhoneNumber(clean(r.phoneNumber()));
        b.setPaymentCycleDays(r.paymentCycleDays()==null?15:r.paymentCycleDays()); b.setStatus(r.status());
    }
    private AavinMilkTest test(UUID supplyUuid) {
        return tests.findBySupply_UuidAndFarm_Uuid(supplyUuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Milk supply quality record not found."));
    }
    private AavinMilkSupply findSupply(UUID uuid) {
        return supplies.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Milk supply not found in this farm."));
    }
    private MilkBuyer buyer(UUID uuid) {
        return buyers.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Buyer/collection centre not found in this farm."));
    }
    private AavinSettlement findSettlement(UUID uuid) {
        return settlements.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Buyer settlement not found in this farm."));
    }
    private Farm lockedFarm() {
        return farms.findLockedByUuid(farmUuid()).orElseThrow(()->new NotFoundException("Farm not found."));
    }
    private UUID farmUuid() { return currentUser.farmUuid(); }
    private LocalDate today() { return LocalDate.now(clock); }
    private void requireAdmin() { if(!currentUser.isAdmin(currentUser.get())) throw new AccessDeniedException("Administrator access is required."); }
    private void requireManager() {
        AppUser user=currentUser.get();
        if(user.getRoles().stream().noneMatch(r->r.getName()==UserRole.ADMIN||r.getName()==UserRole.MANAGER))
            throw new AccessDeniedException("Administrator or manager access is required.");
    }
    private void requireVerifier() {
        AppUser user=currentUser.get();
        if(user.getRoles().stream().noneMatch(r->r.getName()==UserRole.ADMIN||r.getName()==UserRole.MANAGER
                ||r.getName()==UserRole.ACCOUNTANT))
            throw new AccessDeniedException("Administrator, manager, or accountant access is required.");
    }
    private void requireSettlementManager() { requireVerifier(); }
    private void validPeriod(LocalDate start,LocalDate end) {
        if(start==null||(end!=null&&end.isBefore(start)))
            throw new IllegalArgumentException("Provide a valid effective/settlement date period.");
    }
    private static boolean overlaps(LocalDate aStart,LocalDate aEnd,LocalDate bStart,LocalDate bEnd) {
        return (aEnd==null||!aEnd.isBefore(bStart))&&(bEnd==null||!bEnd.isBefore(aStart));
    }
    private void range(LocalDate from,LocalDate to) {
        if(from==null||to==null||to.isBefore(from)) throw new IllegalArgumentException("Provide a valid date range.");
    }
    private Pageable page(int p,int s) { return PageRequest.of(Math.max(0,p),Math.clamp(s,1,100),Sort.by(Sort.Direction.DESC,"createdAt")); }
    private static BigDecimal money(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP); }
    private static BigDecimal difference(BigDecimal farm,BigDecimal buyer) {
        return farm==null||buyer==null?null:money(buyer.subtract(farm));
    }
    private BigDecimal effectiveEstimateRate(AavinMilkTest t) {
        if(t.getManualRate()!=null) return t.getManualRate();
        return t.getAavinRate()!=null?t.getAavinRate():t.getMatchedRate();
    }
    private BigDecimal finalRate(AavinMilkTest t) { return t.getManualRate()!=null?t.getManualRate():t.getAavinRate(); }
    private static boolean equal(BigDecimal a,BigDecimal b) { return a.compareTo(b)==0; }
    private static String clean(String s) { return s==null||s.isBlank()?null:s.trim(); }
    private record RateMatch(AavinRateChart chart,AavinRateSlab slab,BigDecimal rate) {}
}
