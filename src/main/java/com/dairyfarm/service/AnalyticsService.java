package com.dairyfarm.service;

import com.dairyfarm.dto.AnalyticsDtos;
import com.dairyfarm.dto.FinanceDtos;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final MilkProductionRepository production;
    private final FeedConsumptionRepository consumptions;
    private final HealthRecordRepository healthRecords;
    private final AnimalRepository animals;
    private final InventoryService inventory;
    private final FinanceService finance;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public AnalyticsService(MilkProductionRepository production,FeedConsumptionRepository consumptions,
            HealthRecordRepository healthRecords,AnimalRepository animals,InventoryService inventory,
            FinanceService finance,CurrentUserService currentUser,Clock clock) {
        this.production=production; this.consumptions=consumptions; this.healthRecords=healthRecords;
        this.animals=animals; this.inventory=inventory; this.finance=finance; this.currentUser=currentUser; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public AnalyticsDtos.OverviewResponse overview(LocalDate from,LocalDate to) {
        validateRange(from,to); UUID farm=currentUser.farmUuid();
        List<MilkProduction> productionRows=production.findAllByFarm_UuidAndProductionDateBetweenOrderByProductionDateAscSessionAsc(farm,from,to);
        BigDecimal total=ZERO,morning=ZERO,evening=ZERO;
        Map<YearMonth,MonthlyAnalytics> monthly=new TreeMap<>();
        for(MilkProduction row:productionRows) {
            total=total.add(row.getQuantity());
            if(row.getSession()==MilkingSession.MORNING) morning=morning.add(row.getQuantity());
            else if(row.getSession()==MilkingSession.EVENING) evening=evening.add(row.getQuantity());
            MonthlyAnalytics point=monthly.computeIfAbsent(YearMonth.from(row.getProductionDate()),unused->new MonthlyAnalytics());
            point.production=point.production.add(row.getQuantity());
        }
        List<FinanceDtos.ReportRow> financialRows=finance.report(from,to);
        BigDecimal income=ZERO,expenses=ZERO,milkRevenue=ZERO;
        for(FinanceDtos.ReportRow row:financialRows) {
            income=income.add(value(row.income())); expenses=expenses.add(value(row.expense()));
            if("MILK_SALES".equals(row.source())||"AAVIN_SETTLEMENT".equals(row.source()))
                milkRevenue=milkRevenue.add(value(row.income()));
            YearMonth month=YearMonth.from(row.date());
            MonthlyAnalytics point=monthly.computeIfAbsent(month,unused->new MonthlyAnalytics());
            point.income=point.income.add(value(row.income())); point.expenses=point.expenses.add(value(row.expense()));
        }
        BigDecimal feedCost=ZERO; Map<UUID,FeedUsageTotal> feedUsage=new HashMap<>();
        for(FeedConsumption row:consumptions.findAllByFarm_UuidAndDateBetween(farm,from,to)) {
            FeedUsageTotal usage=feedUsage.computeIfAbsent(row.getItem().getUuid(),unused->
                    new FeedUsageTotal(row.getItem().getName(),row.getItem().getUnit()));
            usage.quantity=usage.quantity.add(row.getQuantity()); usage.cost=usage.cost.add(row.getTotalCost());
            feedCost=feedCost.add(row.getTotalCost());
        }
        BigDecimal medicalExpense=ZERO;
        for(HealthRecord row:healthRecords.findAllByFarm_UuidOrderByTreatmentStartDateDescCreatedAtDesc(farm))
            if(row.getMedicalExpense()!=null&&row.getTreatmentStartDate()!=null
                    &&!row.getTreatmentStartDate().isBefore(from)&&!row.getTreatmentStartDate().isAfter(to))
                medicalExpense=medicalExpense.add(row.getMedicalExpense());
        BigDecimal expensePerLitre=total.signum()==0?null:money(expenses.divide(total,2,RoundingMode.HALF_UP));
        List<AnalyticsDtos.PeriodPoint> trend=monthly.entrySet().stream().map(entry->
                new AnalyticsDtos.PeriodPoint(entry.getKey().toString(),quantity(entry.getValue().production),
                        money(entry.getValue().income),money(entry.getValue().expenses))).toList();
        List<AnalyticsDtos.FeedUsagePoint> feedUsageRows=feedUsage.values().stream()
                .map(item->new AnalyticsDtos.FeedUsagePoint(item.name,item.unit,quantity(item.quantity),money(item.cost)))
                .sorted(Comparator.comparing(AnalyticsDtos.FeedUsagePoint::item)).toList();
        return new AnalyticsDtos.OverviewResponse(from,to,quantity(total),quantity(morning),quantity(evening),
                money(milkRevenue),money(income),money(expenses),money(income.subtract(expenses)),
                money(feedCost),feedUsageRows,money(medicalExpense),expensePerLitre,trend);
    }

    @Transactional(readOnly=true)
    public List<AnalyticsDtos.AnimalPerformance> animalPerformance(LocalDate from,LocalDate to) {
        validateRange(from,to); UUID farm=currentUser.farmUuid();
        Map<UUID,BigDecimal> yields=new HashMap<>(),feedCosts=new HashMap<>(),medicalCosts=new HashMap<>();
        BigDecimal totalProduction=ZERO,milkRevenue=ZERO;
        for(MilkProduction row:production.findAllByFarm_UuidAndProductionDateBetweenOrderByProductionDateAscSessionAsc(farm,from,to)) {
            totalProduction=totalProduction.add(row.getQuantity());
            yields.merge(row.getAnimal().getUuid(),row.getQuantity(),BigDecimal::add);
        }
        for(FeedConsumption row:consumptions.findAllByFarm_UuidAndDateBetween(farm,from,to))
            if(row.getAnimal()!=null) feedCosts.merge(row.getAnimal().getUuid(),row.getTotalCost(),BigDecimal::add);
        for(HealthRecord row:healthRecords.findAllByFarm_UuidOrderByTreatmentStartDateDescCreatedAtDesc(farm))
            if(row.getMedicalExpense()!=null&&row.getTreatmentStartDate()!=null
                    &&!row.getTreatmentStartDate().isBefore(from)&&!row.getTreatmentStartDate().isAfter(to))
                medicalCosts.merge(row.getAnimal().getUuid(),row.getMedicalExpense(),BigDecimal::add);
        for(FinanceDtos.ReportRow row:finance.report(from,to))
            if("MILK_SALES".equals(row.source())||"AAVIN_SETTLEMENT".equals(row.source()))
                milkRevenue=milkRevenue.add(value(row.income()));
        BigDecimal farmProduction=totalProduction,farmMilkRevenue=milkRevenue;
        return animals.findAllByFarm_UuidAndArchivedFalse(farm).stream()
                .filter(animal->animal.getStatus()==AnimalStatus.ACTIVE)
                .map(animal->{
                    BigDecimal liters=yields.getOrDefault(animal.getUuid(),ZERO);
                    BigDecimal feed=feedCosts.getOrDefault(animal.getUuid(),ZERO);
                    BigDecimal medical=medicalCosts.getOrDefault(animal.getUuid(),ZERO);
                    BigDecimal allocated=farmProduction.signum()==0||liters.signum()==0?ZERO:
                            money(farmMilkRevenue.multiply(liters).divide(farmProduction,2,RoundingMode.HALF_UP));
                    return new AnalyticsDtos.AnimalPerformance(animal.getUuid(),animal.getTagNumber(),animal.getName(),
                            quantity(liters),money(feed),money(medical),money(allocated),
                            money(allocated.subtract(feed).subtract(medical)),farmProduction.signum()>0);
                }).sorted(Comparator.comparing(AnalyticsDtos.AnimalPerformance::estimatedDirectContribution).reversed())
                .toList();
    }

    @Transactional(readOnly=true)
    public List<AnalyticsDtos.AlertResponse> alerts() {
        List<AnalyticsDtos.AlertResponse> result=new ArrayList<>(); LocalDate today=today();
        for(var alert:inventory.alerts()) {
            String severity=alert.alertType().equals("EXPIRED")?"HIGH":
                    alert.alertType().equals("LOW_STOCK")?"MEDIUM":"LOW";
            result.add(new AnalyticsDtos.AlertResponse("INVENTORY",severity,
                    alert.alertType().replace('_',' ')+": "+alert.itemName(),
                    "Current stock: "+alert.currentStock()+" "+alert.unit(),alert.itemUuid(),alert.expiryDate()));
        }
        for(Animal animal:animals.findAllByFarm_UuidAndPregnancyStatusAndArchivedFalseAndExpectedDeliveryDateBetweenOrderByExpectedDeliveryDateAsc(
                currentUser.farmUuid(),PregnancyStatus.PREGNANT,today,today.plusDays(14)))
            result.add(new AnalyticsDtos.AlertResponse("EXPECTED_DELIVERY","MEDIUM",
                    "Expected calving: "+animal.getTagNumber(),"Expected delivery is approaching.",
                    animal.getUuid(),animal.getExpectedDeliveryDate()));
        FinanceDtos.SummaryResponse financeSummary=finance.summary();
        if(financeSummary.customerReceivables().signum()>0)
            result.add(new AnalyticsDtos.AlertResponse("RECEIVABLE","MEDIUM","Customer balances are outstanding",
                    "Outstanding retail customer balance: ₹"+financeSummary.customerReceivables(),null,null));
        if(financeSummary.supplierPayables().signum()>0)
            result.add(new AnalyticsDtos.AlertResponse("PAYABLE","MEDIUM","Supplier balances are outstanding",
                    "Unpaid inventory purchases: ₹"+financeSummary.supplierPayables(),null,null));
        return result;
    }

    private void validateRange(LocalDate from,LocalDate to) {
        if(from==null||to==null||to.isBefore(from)) throw new IllegalArgumentException("Provide a valid date range.");
    }
    private LocalDate today() { return LocalDate.now(clock); }
    private BigDecimal value(BigDecimal input) { return input==null?ZERO:input; }
    private BigDecimal money(BigDecimal input) { return input.setScale(2,RoundingMode.HALF_UP); }
    private BigDecimal quantity(BigDecimal input) { return input.setScale(3,RoundingMode.HALF_UP); }
    private static final class MonthlyAnalytics {
        private BigDecimal production=ZERO;
        private BigDecimal income=ZERO;
        private BigDecimal expenses=ZERO;
    }
    private static final class FeedUsageTotal {
        private final String name;
        private final String unit;
        private BigDecimal quantity=ZERO;
        private BigDecimal cost=ZERO;
        private FeedUsageTotal(String name,String unit) { this.name=name; this.unit=unit; }
    }
}
