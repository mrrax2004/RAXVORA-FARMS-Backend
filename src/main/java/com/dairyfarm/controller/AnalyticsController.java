package com.dairyfarm.controller;

import com.dairyfarm.dto.AnalyticsDtos;
import com.dairyfarm.service.AnalyticsService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
public class AnalyticsController {
    private final AnalyticsService analytics;
    public AnalyticsController(AnalyticsService analytics) { this.analytics=analytics; }

    @GetMapping("/overview")
    public AnalyticsDtos.OverviewResponse overview(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.overview(from,to);
    }
    @GetMapping("/animals")
    public java.util.List<AnalyticsDtos.AnimalPerformance> animalPerformance(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.animalPerformance(from,to);
    }
    @GetMapping("/alerts")
    public java.util.List<AnalyticsDtos.AlertResponse> alerts() { return analytics.alerts(); }

    @GetMapping(value="/animals.csv",produces="text/csv")
    public ResponseEntity<String> animalCsv(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv=new StringBuilder("animal_tag,name,milk_production,individual_feed_cost,medical_expense,allocated_milk_revenue,estimated_direct_contribution,revenue_allocation_estimated\r\n");
        analytics.animalPerformance(from,to).forEach(row->csv.append(escape(row.tagNumber())).append(',')
                .append(escape(row.name())).append(',').append(row.milkProduction()).append(',')
                .append(row.individualFeedCost()).append(',').append(row.medicalExpense()).append(',')
                .append(row.allocatedMilkRevenue()).append(',').append(row.estimatedDirectContribution()).append(',')
                .append(row.revenueAllocationEstimated()).append("\r\n"));
        return csvResponse("animal-profitability.csv",csv.toString());
    }

    private ResponseEntity<String> csvResponse(String fileName,String content) {
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+fileName+"\"").body(content);
    }
    private static String escape(String text) {
        return text==null?"":"\""+text.replace("\"","\"\"")+"\"";
    }
}
