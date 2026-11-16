package com.dairyfarm.controller;

import com.dairyfarm.dto.PhaseThreeDtos;
import com.dairyfarm.model.CustomerStatus;
import com.dairyfarm.model.CustomerMilkRateStatus;
import com.dairyfarm.model.CustomerType;
import com.dairyfarm.model.MilkStockTransactionType;
import com.dairyfarm.model.SubscriptionStatus;
import com.dairyfarm.service.MilkManagementService;
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
@RequestMapping("/api/milk")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER','ACCOUNTANT')")
public class MilkController {
    private final MilkManagementService milk;
    public MilkController(MilkManagementService milk) { this.milk = milk; }

    @GetMapping("/dashboard")
    public PhaseThreeDtos.DashboardResponse dashboard() { return milk.dashboard(); }

    @PatchMapping("/animals/{uuid}/lactation")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void lactation(@PathVariable UUID uuid, @Valid @RequestBody LactationRequest request) {
        milk.lactating(uuid, request.lactating());
    }

    @PostMapping("/production")
    public PhaseThreeDtos.ProductionResponse production(@Valid @RequestBody PhaseThreeDtos.ProductionRequest request) {
        return milk.recordProduction(request);
    }
    @PostMapping("/production/batch")
    public java.util.List<PhaseThreeDtos.ProductionResponse> batchProduction(@Valid @RequestBody PhaseThreeDtos.BatchProductionRequest request) {
        return milk.recordBatchProduction(request);
    }
    @GetMapping("/production")
    public Page<PhaseThreeDtos.ProductionResponse> production(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) UUID animalUuid, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return milk.production(from,to,animalUuid,page,size); }
    @PatchMapping("/production/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.ProductionResponse correctProduction(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseThreeDtos.ProductionCorrection request) { return milk.correctProduction(uuid,request); }
    @GetMapping("/production/{uuid}/audit")
    public java.util.List<PhaseThreeDtos.ProductionAuditResponse> productionAudit(@PathVariable UUID uuid) { return milk.productionAudit(uuid); }
    @GetMapping("/production/daily")
    public java.util.List<PhaseThreeDtos.DailyProduction> productionReport(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return milk.dailyProduction(from,to);
    }

    // Legacy collection rows remain readable; production entries are now the only farm stock intake.
    @GetMapping("/collections")
    public Page<PhaseThreeDtos.CollectionResponse> collections(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return milk.collections(from,to,page,size);
    }
    @GetMapping("/stock")
    public PhaseThreeDtos.StockResponse stock() { return milk.stock(); }
    @GetMapping("/stock/transactions")
    public Page<PhaseThreeDtos.StockTransactionResponse> stockTransactions(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return milk.stockTransactions(page,size); }
    @PostMapping("/stock/opening")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.StockResponse openingStock(@Valid @RequestBody PhaseThreeDtos.StockAdjustment request) { return milk.openingStock(request); }
    @PostMapping("/stock/adjustments/{type}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.StockResponse stockAdjustment(@PathVariable MilkStockTransactionType type,
            @Valid @RequestBody PhaseThreeDtos.StockAdjustment request) { return milk.stockAdjustment(type,request); }

    @PostMapping("/customers")
    public PhaseThreeDtos.CustomerResponse createCustomer(@Valid @RequestBody PhaseThreeDtos.CustomerRequest request) { return milk.createCustomer(request); }
    @GetMapping("/customers")
    public Page<PhaseThreeDtos.CustomerResponse> customers(@RequestParam(required=false) String search,
            @RequestParam(required=false) CustomerStatus status, @RequestParam(required=false) CustomerType type,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return milk.customers(search,status,type,page,size);
    }
    @GetMapping("/customers/{uuid}")
    public PhaseThreeDtos.CustomerResponse customer(@PathVariable UUID uuid) { return milk.getCustomer(uuid); }
    @org.springframework.web.bind.annotation.PutMapping("/customers/{uuid}")
    public PhaseThreeDtos.CustomerResponse updateCustomer(@PathVariable UUID uuid,@Valid @RequestBody PhaseThreeDtos.CustomerRequest request) {
        return milk.updateCustomer(uuid,request);
    }
    @GetMapping("/default-rate")
    public PhaseThreeDtos.DefaultMilkRateResponse defaultMilkRate() { return milk.defaultMilkRate(); }
    @PutMapping("/default-rate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.DefaultMilkRateResponse updateDefaultMilkRate(
            @Valid @RequestBody PhaseThreeDtos.DefaultMilkRateRequest request) { return milk.updateDefaultMilkRate(request); }
    @GetMapping("/customers/{uuid}/rates")
    public java.util.List<PhaseThreeDtos.CustomerMilkRateResponse> customerMilkRates(@PathVariable UUID uuid) {
        return milk.customerMilkRates(uuid);
    }
    @GetMapping("/customers/{uuid}/rates/applicable")
    public PhaseThreeDtos.MilkRateQuoteResponse milkRateQuote(@PathVariable UUID uuid,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return milk.milkRateQuote(uuid,date);
    }
    @PostMapping("/customers/{uuid}/rates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.CustomerMilkRateResponse createCustomerMilkRate(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseThreeDtos.CustomerMilkRateRequest request) {
        return milk.createCustomerMilkRate(uuid,request);
    }
    @PutMapping("/customers/{uuid}/rates/{rateUuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.CustomerMilkRateResponse updateCustomerMilkRate(@PathVariable UUID uuid,
            @PathVariable UUID rateUuid, @Valid @RequestBody PhaseThreeDtos.CustomerMilkRateRequest request) {
        return milk.updateCustomerMilkRate(uuid,rateUuid,request);
    }
    @PatchMapping("/customers/{uuid}/rates/{rateUuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PhaseThreeDtos.CustomerMilkRateResponse customerMilkRateStatus(@PathVariable UUID uuid,
            @PathVariable UUID rateUuid, @Valid @RequestBody PhaseThreeDtos.CustomerMilkRateStatusRequest request) {
        return milk.customerMilkRateStatus(uuid,rateUuid,request.status());
    }

    @PostMapping("/sales")
    public PhaseThreeDtos.SaleResponse createSale(@Valid @RequestBody PhaseThreeDtos.SaleRequest request) { return milk.createSale(request); }
    @GetMapping("/sales")
    public Page<PhaseThreeDtos.SaleResponse> sales(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) UUID customerUuid,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return milk.sales(from,to,customerUuid,page,size);
    }
    @GetMapping("/sales/{uuid}")
    public PhaseThreeDtos.SaleResponse sale(@PathVariable UUID uuid) { return milk.sale(uuid); }
    @GetMapping("/sales/{uuid}/print")
    public PhaseThreeDtos.SaleResponse printInvoice(@PathVariable UUID uuid) { return milk.sale(uuid); }

    @PostMapping("/bills/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public PhaseThreeDtos.MilkBillResponse generateMilkBill(
            @Valid @RequestBody PhaseThreeDtos.GenerateMilkBillRequest request) {
        return milk.generateMilkBill(request);
    }
    @GetMapping("/bills")
    public Page<PhaseThreeDtos.MilkBillResponse> milkBills(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) UUID customerUuid, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return milk.milkBills(from,to,customerUuid,page,size);
    }
    @GetMapping("/bills/{uuid}")
    public PhaseThreeDtos.MilkBillResponse milkBill(@PathVariable UUID uuid) { return milk.milkBill(uuid); }
    @PostMapping("/bills/{uuid}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public PhaseThreeDtos.BillAdjustmentResponse adjustMilkBill(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseThreeDtos.BillAdjustmentRequest request) {
        return milk.adjustMilkBill(uuid, request);
    }

    @PostMapping("/subscriptions")
    public PhaseThreeDtos.SubscriptionResponse createSubscription(@Valid @RequestBody PhaseThreeDtos.SubscriptionRequest request) {
        return milk.createSubscription(request);
    }
    @org.springframework.web.bind.annotation.PutMapping("/subscriptions/{uuid}")
    public PhaseThreeDtos.SubscriptionResponse updateSubscription(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseThreeDtos.SubscriptionRequest request) { return milk.updateSubscription(uuid,request); }
    @GetMapping("/subscriptions")
    public Page<PhaseThreeDtos.SubscriptionResponse> subscriptions(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return milk.subscriptions(page,size); }
    @PatchMapping("/subscriptions/{uuid}/status")
    public PhaseThreeDtos.SubscriptionResponse subscriptionStatus(@PathVariable UUID uuid,
            @Valid @RequestBody PhaseThreeDtos.SubscriptionStatusRequest request) { return milk.subscriptionStatus(uuid,request.status()); }
    @PostMapping("/deliveries/generate")
    public GeneratedDeliveries generate(@Valid @RequestBody PhaseThreeDtos.GenerateDeliveriesRequest request) {
        return new GeneratedDeliveries(milk.generateDeliveries(request.date()));
    }
    @GetMapping("/deliveries")
    public Page<PhaseThreeDtos.DeliveryResponse> deliveries(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return milk.deliveries(from,to,page,size); }
    @PatchMapping("/deliveries/{uuid}")
    public PhaseThreeDtos.DeliveryResponse updateDelivery(@PathVariable UUID uuid,@Valid @RequestBody PhaseThreeDtos.DeliveryUpdate request) {
        return milk.updateDelivery(uuid,request);
    }

    @PostMapping("/payments")
    public PhaseThreeDtos.PaymentResponse payment(@Valid @RequestBody PhaseThreeDtos.PaymentRequest request) { return milk.payment(request); }
    @GetMapping("/payments")
    public Page<PhaseThreeDtos.PaymentResponse> payments(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) UUID customerUuid,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return milk.payments(from,to,customerUuid,page,size);
    }

    @GetMapping("/reports")
    public java.util.List<PhaseThreeDtos.ReportRow> report(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) { return milk.report(type,from,to); }
    @GetMapping(value="/reports.csv",produces="text/csv")
    public ResponseEntity<String> csv(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv=new StringBuilder("date,category,label,quantity,amount,details\r\n");
        milk.report(type,from,to).forEach(row->csv.append(row.date()).append(',')
                .append(escape(row.category())).append(',').append(escape(row.label())).append(',')
                .append(row.quantity()==null?"":row.quantity()).append(',')
                .append(row.amount()==null?"":row.amount()).append(',').append(escape(row.details())).append("\r\n"));
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"milk-"+type+"-report.csv\"").body(csv.toString());
    }
    private static String escape(String text) {
        if(text==null) return "";
        if(text.stripLeading().matches("^[=+@-].*")) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    public record LactationRequest(@jakarta.validation.constraints.NotNull Boolean lactating) {}
    public record GeneratedDeliveries(int generated) {}
}
