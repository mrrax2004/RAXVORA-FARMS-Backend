package com.dairyfarm.controller;

import com.dairyfarm.dto.FinanceDtos;
import com.dairyfarm.model.FinanceCategory;
import com.dairyfarm.service.FinanceService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
public class FinanceController {
    private final FinanceService finance;
    public FinanceController(FinanceService finance) { this.finance=finance; }

    @GetMapping("/summary")
    public FinanceDtos.SummaryResponse summary() { return finance.summary(); }
    @GetMapping("/categories")
    public FinanceCategory[] categories() { return FinanceCategory.values(); }
    @GetMapping("/accounts")
    public java.util.List<FinanceDtos.AccountResponse> accounts() { return finance.accounts(); }
    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public FinanceDtos.AccountResponse createAccount(@Valid @RequestBody FinanceDtos.AccountRequest request) {
        return finance.createAccount(request);
    }
    @PatchMapping("/accounts/{uuid}/status")
    public FinanceDtos.AccountResponse updateAccountStatus(@PathVariable UUID uuid,
            @Valid @RequestBody FinanceDtos.AccountStatusRequest request) {
        return finance.updateAccountStatus(uuid,request);
    }
    @GetMapping("/transactions")
    public java.util.List<FinanceDtos.TransactionResponse> transactions(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return finance.transactions(from,to);
    }
    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public FinanceDtos.TransactionResponse createTransaction(@Valid @RequestBody FinanceDtos.TransactionRequest request) {
        return finance.createTransaction(request);
    }
    @GetMapping("/recurring")
    public java.util.List<FinanceDtos.RecurringResponse> recurring() { return finance.recurring(); }
    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public FinanceDtos.RecurringResponse createRecurring(@Valid @RequestBody FinanceDtos.RecurringRequest request) {
        return finance.createRecurring(request);
    }
    @PatchMapping("/recurring/{uuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public FinanceDtos.RecurringResponse updateRecurringStatus(@PathVariable UUID uuid,
            @Valid @RequestBody FinanceDtos.StatusRequest request) {
        return finance.updateRecurringStatus(uuid,request);
    }
    @PostMapping("/recurring/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public FinanceDtos.GenerationResponse generateDue() { return finance.generateDue(); }
    @GetMapping("/reports")
    public java.util.List<FinanceDtos.ReportRow> report(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return finance.report(from,to);
    }
    @GetMapping(value="/reports.csv",produces="text/csv")
    public ResponseEntity<String> csv(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv=new StringBuilder("date,type,category,description,account,income,expense,source\r\n");
        finance.report(from,to).forEach(row->csv.append(row.date()).append(',')
                .append(escape(row.type())).append(',').append(escape(row.category())).append(',')
                .append(escape(row.description())).append(',').append(escape(row.account())).append(',')
                .append(value(row.income())).append(',').append(value(row.expense())).append(',')
                .append(escape(row.source())).append("\r\n"));
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"farm-finance-report.csv\"")
                .body(csv.toString());
    }
    private static String value(Object value) { return value==null?"":value.toString(); }
    private static String escape(String text) {
        if(text==null) return "";
        return "\""+text.replace("\"","\"\"")+"\"";
    }
}
