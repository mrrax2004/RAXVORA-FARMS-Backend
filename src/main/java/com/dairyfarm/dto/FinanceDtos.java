package com.dairyfarm.dto;

import com.dairyfarm.model.FinanceAccountType;
import com.dairyfarm.model.FinanceCategory;
import com.dairyfarm.model.FinanceFrequency;
import com.dairyfarm.model.FinanceTemplateStatus;
import com.dairyfarm.model.FinanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() {}
    public record AccountRequest(@NotBlank @Size(max=120) String name, @NotNull FinanceAccountType type,
            @NotNull @Digits(integer=12,fraction=2) BigDecimal openingBalance) {}
    public record AccountResponse(UUID uuid, String name, FinanceAccountType type,
            BigDecimal openingBalance, BigDecimal balance, boolean active) {}
    public record TransactionRequest(@NotNull UUID accountUuid, @NotNull FinanceType type,
            @NotNull FinanceCategory category, @NotNull LocalDate date,
            @NotNull @Positive @Digits(integer=12,fraction=2) BigDecimal amount,
            @NotBlank @Size(max=200) String description, @Size(max=1000) String notes) {}
    public record TransactionResponse(UUID uuid, UUID accountUuid, String accountName, FinanceType type,
            FinanceCategory category, LocalDate date, BigDecimal amount, String description, String notes,
            String source, Instant createdAt) {}
    public record RecurringRequest(@NotBlank @Size(max=160) String name, @NotNull UUID accountUuid,
            @NotNull FinanceType type, @NotNull FinanceCategory category, @NotNull FinanceFrequency frequency,
            @NotNull @Positive @Digits(integer=12,fraction=2) BigDecimal amount,
            @NotNull LocalDate startDate, LocalDate endDate) {}
    public record RecurringResponse(UUID uuid, String name, UUID accountUuid, String accountName,
            FinanceType type, FinanceCategory category, FinanceFrequency frequency, BigDecimal amount,
            LocalDate startDate, LocalDate endDate, LocalDate nextDate, FinanceTemplateStatus status) {}
    public record StatusRequest(@NotNull FinanceTemplateStatus status) {}
    public record AccountStatusRequest(boolean active) {}
    public record ReportRow(LocalDate date, String type, String category, String description,
            String account, BigDecimal income, BigDecimal expense, String source) {}
    public record SummaryResponse(BigDecimal income, BigDecimal expenses, BigDecimal operatingResult,
            BigDecimal customerReceivables, BigDecimal aavinReceivables, BigDecimal supplierPayables,
            BigDecimal accountBalances) {}
    public record GenerationResponse(int generated, LocalDate generatedThrough) {}
}
