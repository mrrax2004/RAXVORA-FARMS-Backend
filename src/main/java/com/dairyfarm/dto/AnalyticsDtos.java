package com.dairyfarm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}
    public record PeriodPoint(String period, BigDecimal milkProduction, BigDecimal income, BigDecimal expenses) {}
    public record FeedUsagePoint(String item, String unit, BigDecimal quantity, BigDecimal cost) {}
    public record OverviewResponse(LocalDate from, LocalDate to, BigDecimal totalProduction,
            BigDecimal morningProduction, BigDecimal eveningProduction, BigDecimal milkRevenue,
            BigDecimal totalIncome, BigDecimal expenses, BigDecimal operatingResult,
            BigDecimal feedCost, List<FeedUsagePoint> feedUsage, BigDecimal medicalExpense,
            BigDecimal expensePerLitre, List<PeriodPoint> trend) {}
    public record AnimalPerformance(UUID animalUuid, String tagNumber, String name,
            BigDecimal milkProduction, BigDecimal individualFeedCost, BigDecimal medicalExpense,
            BigDecimal allocatedMilkRevenue, BigDecimal estimatedDirectContribution,
            boolean revenueAllocationEstimated) {}
    public record AlertResponse(String category, String severity, String title, String description,
            UUID referenceUuid, LocalDate dueDate) {}
}
