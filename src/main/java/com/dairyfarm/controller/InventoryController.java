package com.dairyfarm.controller;

import com.dairyfarm.dto.InventoryDtos;
import com.dairyfarm.model.InventoryItemType;
import com.dairyfarm.service.InventoryService;
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
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WORKER','ACCOUNTANT')")
public class InventoryController {
    private final InventoryService inventory;
    public InventoryController(InventoryService inventory) { this.inventory=inventory; }

    @GetMapping("/summary")
    public InventoryDtos.SummaryResponse summary() { return inventory.summary(); }
    @GetMapping("/alerts")
    public java.util.List<InventoryDtos.AlertResponse> alerts() { return inventory.alerts(); }

    @GetMapping("/suppliers")
    public java.util.List<InventoryDtos.SupplierResponse> suppliers() { return inventory.suppliers(); }
    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.SupplierResponse createSupplier(@Valid @RequestBody InventoryDtos.SupplierRequest request) {
        return inventory.createSupplier(request);
    }
    @PutMapping("/suppliers/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.SupplierResponse updateSupplier(@PathVariable UUID uuid,
            @Valid @RequestBody InventoryDtos.SupplierRequest request) { return inventory.updateSupplier(uuid,request); }

    @GetMapping("/items")
    public java.util.List<InventoryDtos.ItemResponse> items(@RequestParam(required=false) InventoryItemType type) {
        return inventory.items(type);
    }
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.ItemResponse createItem(@Valid @RequestBody InventoryDtos.ItemRequest request) {
        return inventory.createItem(request);
    }
    @PutMapping("/items/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.ItemResponse updateItem(@PathVariable UUID uuid,
            @Valid @RequestBody InventoryDtos.ItemRequest request) { return inventory.updateItem(uuid,request); }
    @PostMapping("/items/{uuid}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.ItemResponse adjustStock(@PathVariable UUID uuid,
            @Valid @RequestBody InventoryDtos.StockAdjustmentRequest request) { return inventory.adjustStock(uuid,request); }

    @PostMapping("/purchases")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public InventoryDtos.PurchaseResponse purchase(@Valid @RequestBody InventoryDtos.PurchaseRequest request) {
        return inventory.purchase(request);
    }
    @GetMapping("/purchases")
    public java.util.List<InventoryDtos.PurchaseResponse> purchases(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return inventory.purchases(from,to);
    }
    @PostMapping("/purchases/{uuid}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public InventoryDtos.PurchaseResponse supplierPayment(@PathVariable UUID uuid,
            @Valid @RequestBody InventoryDtos.SupplierPaymentRequest request) {
        return inventory.payPurchase(uuid,request);
    }
    @GetMapping("/purchases/{uuid}/payments")
    public java.util.List<InventoryDtos.SupplierPaymentResponse> supplierPayments(@PathVariable UUID uuid) {
        return inventory.supplierPayments(uuid);
    }

    @PostMapping("/consumptions")
    public InventoryDtos.ConsumptionResponse consume(@Valid @RequestBody InventoryDtos.ConsumptionRequest request) {
        return inventory.consume(request);
    }
    @GetMapping("/consumptions")
    public Page<InventoryDtos.ConsumptionResponse> consumptions(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return inventory.consumptions(from,to,page,size);
    }
    @PatchMapping("/consumptions/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.ConsumptionResponse correctConsumption(@PathVariable UUID uuid,
            @Valid @RequestBody InventoryDtos.ConsumptionCorrectionRequest request) {
        return inventory.correctConsumption(uuid,request);
    }
    @GetMapping("/consumptions/{uuid}/audit")
    public java.util.List<InventoryDtos.ConsumptionAuditResponse> consumptionAudit(@PathVariable UUID uuid) {
        return inventory.consumptionAudit(uuid);
    }

    @GetMapping("/rations")
    public java.util.List<InventoryDtos.RationResponse> rations() { return inventory.rations(); }
    @PostMapping("/rations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryDtos.RationResponse createRation(@Valid @RequestBody InventoryDtos.RationRequest request) {
        return inventory.createRation(request);
    }

    @GetMapping("/transactions")
    public Page<InventoryDtos.InventoryTransactionResponse> transactions(
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return inventory.transactions(page,size);
    }
    @GetMapping("/reports")
    public java.util.List<InventoryDtos.ReportRow> report(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return inventory.report(type,from,to);
    }
    @GetMapping(value="/reports.csv",produces="text/csv")
    public ResponseEntity<String> csv(@RequestParam String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv=new StringBuilder("date,type,item,category,animal,group,quantity,unit,unit_cost,total_cost,reference\r\n");
        inventory.report(type,from,to).forEach(row->csv.append(row.date()).append(',')
                .append(escape(row.type())).append(',').append(escape(row.item())).append(',')
                .append(escape(row.category())).append(',').append(escape(row.animal())).append(',')
                .append(escape(row.group())).append(',').append(value(row.quantity())).append(',')
                .append(escape(row.unit())).append(',').append(value(row.unitCost())).append(',')
                .append(value(row.totalCost())).append(',').append(escape(row.reference())).append("\r\n"));
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"inventory-"+type+"-report.csv\"")
                .body(csv.toString());
    }
    private static String value(Object value) { return value==null?"":value.toString(); }
    private static String escape(String text) {
        if(text==null) return "";
        if(text.stripLeading().matches("^[=+@-].*")) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
}
