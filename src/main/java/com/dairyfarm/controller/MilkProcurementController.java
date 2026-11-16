package com.dairyfarm.controller;

import com.dairyfarm.dto.MilkProcurementDtos;
import com.dairyfarm.service.MilkProcurementService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/aavin", "/api/procurement"})
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER','ACCOUNTANT')")
public class MilkProcurementController {
    private final MilkProcurementService procurement;
    public MilkProcurementController(MilkProcurementService procurement) { this.procurement=procurement; }

    @GetMapping("/dashboard")
    public MilkProcurementDtos.DashboardResponse dashboard() { return procurement.dashboard(); }

    @GetMapping("/buyers")
    public java.util.List<MilkProcurementDtos.BuyerResponse> buyers() { return procurement.buyers(); }
    @PostMapping("/buyers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public MilkProcurementDtos.BuyerResponse createBuyer(@Valid @RequestBody MilkProcurementDtos.BuyerRequest request) {
        return procurement.createBuyer(request);
    }
    @PutMapping("/buyers/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public MilkProcurementDtos.BuyerResponse updateBuyer(@PathVariable UUID uuid,@Valid @RequestBody MilkProcurementDtos.BuyerRequest request) {
        return procurement.updateBuyer(uuid,request);
    }

    @GetMapping("/rate-charts")
    public java.util.List<MilkProcurementDtos.RateChartResponse> rateCharts(@RequestParam UUID buyerUuid) {
        return procurement.rateCharts(buyerUuid);
    }
    @PostMapping("/rate-charts")
    @PreAuthorize("hasRole('ADMIN')")
    public MilkProcurementDtos.RateChartResponse createRateChart(@Valid @RequestBody MilkProcurementDtos.RateChartRequest request) {
        return procurement.createRateChart(request);
    }
    @PutMapping("/rate-charts/{uuid}/period")
    @PreAuthorize("hasRole('ADMIN')")
    public MilkProcurementDtos.RateChartResponse updateRateChartPeriod(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.RateChartPeriodRequest request) {
        return procurement.updateRateChartPeriod(uuid,request);
    }
    @GetMapping("/rate-charts/{uuid}/audit")
    public java.util.List<MilkProcurementDtos.RateChartAuditResponse> rateChartAudit(@PathVariable UUID uuid) {
        return procurement.rateChartAudit(uuid);
    }

    @PostMapping("/supplies")
    public MilkProcurementDtos.SupplyResponse createSupply(@Valid @RequestBody MilkProcurementDtos.SupplyRequest request) {
        return procurement.createSupply(request);
    }
    @GetMapping("/supplies")
    public Page<MilkProcurementDtos.SupplyResponse> supplies(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) UUID buyerUuid,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return procurement.supplies(from,to,buyerUuid,page,size);
    }
    @GetMapping("/supplies/{uuid}")
    public MilkProcurementDtos.SupplyResponse supply(@PathVariable UUID uuid) { return procurement.supply(uuid); }
    @GetMapping("/supplies/{uuid}/audit")
    public java.util.List<MilkProcurementDtos.SupplyAuditResponse> supplyAudit(@PathVariable UUID uuid) {
        return procurement.supplyAudit(uuid);
    }
    @PatchMapping("/supplies/{uuid}/verification")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public MilkProcurementDtos.SupplyResponse verifySupply(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.SupplyVerificationRequest request) { return procurement.verifySupply(uuid,request); }
    @PatchMapping("/supplies/{uuid}/rate")
    @PreAuthorize("hasRole('ADMIN')")
    public MilkProcurementDtos.SupplyResponse overrideSupplyRate(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.SupplyRateOverrideRequest request) { return procurement.overrideSupplyRate(uuid,request); }

    @PostMapping("/settlements")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public MilkProcurementDtos.SettlementResponse createSettlement(@Valid @RequestBody MilkProcurementDtos.SettlementRequest request) {
        return procurement.createSettlement(request);
    }
    @GetMapping("/settlements")
    public Page<MilkProcurementDtos.SettlementResponse> settlements(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return procurement.settlements(page,size); }
    @GetMapping("/settlements/{uuid}")
    public MilkProcurementDtos.SettlementResponse settlement(@PathVariable UUID uuid) { return procurement.settlement(uuid); }
    @PostMapping("/settlements/{uuid}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public MilkProcurementDtos.SettlementResponse adjustment(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.AdjustmentRequest request) { return procurement.addAdjustment(uuid,request); }
    @PostMapping("/settlements/{uuid}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public MilkProcurementDtos.SettlementResponse payment(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.PaymentRequest request) { return procurement.addPayment(uuid,request); }
    @PatchMapping("/settlements/{uuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public MilkProcurementDtos.SettlementResponse settlementStatus(@PathVariable UUID uuid,
            @Valid @RequestBody MilkProcurementDtos.SettlementStateRequest request) { return procurement.settlementStatus(uuid,request); }

    @GetMapping("/reports")
    public java.util.List<MilkProcurementDtos.ReportRow> report(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return procurement.report(type,from,to);
    }
    @GetMapping(value="/reports.csv",produces="text/csv")
    public ResponseEntity<String> csv(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv=new StringBuilder("date,category,centre,session,quantity,fat,snf,rate,amount,status,reference\r\n");
        procurement.report(type,from,to).forEach(r->csv.append(r.date()).append(',')
                .append(escape(r.category())).append(',').append(escape(r.centre())).append(',')
                .append(escape(r.session())).append(',').append(value(r.quantity())).append(',')
                .append(value(r.fat())).append(',').append(value(r.snf())).append(',')
                .append(value(r.rate())).append(',').append(value(r.amount())).append(',')
                .append(escape(r.status())).append(',').append(escape(r.reference())).append("\r\n"));
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"aavin-"+type+"-report.csv\"").body(csv.toString());
    }
    private static String value(Object value) { return value==null?"":value.toString(); }
    private static String escape(String text) {
        if(text==null) return "";
        if(text.stripLeading().matches("^[=+@-].*")) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
}
