package com.dairyfarm.service;

import com.dairyfarm.dto.FinanceDtos;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final FinanceAccountRepository accounts;
    private final FinanceTransactionRepository transactions;
    private final RecurringFinanceTemplateRepository templates;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    private final MilkSaleRepository sales;
    private final CustomerPaymentRepository customerPayments;
    private final MilkBillRepository milkBills;
    private final FeedPurchaseRepository purchases;
    private final AavinSettlementRepository settlements;
    private final AavinPaymentRepository aavinPayments;
    private final Clock clock;

    public FinanceService(FinanceAccountRepository accounts, FinanceTransactionRepository transactions,
            RecurringFinanceTemplateRepository templates, FarmRepository farms, CurrentUserService currentUser,
            MilkSaleRepository sales, CustomerPaymentRepository customerPayments, MilkBillRepository milkBills,
            FeedPurchaseRepository purchases,
            AavinSettlementRepository settlements, AavinPaymentRepository aavinPayments, Clock clock) {
        this.accounts=accounts; this.transactions=transactions; this.templates=templates; this.farms=farms;
        this.currentUser=currentUser; this.sales=sales; this.customerPayments=customerPayments; this.milkBills=milkBills;
        this.purchases=purchases; this.settlements=settlements; this.aavinPayments=aavinPayments; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public List<FinanceDtos.AccountResponse> accounts() {
        UUID farm=farmUuid();
        return accounts.findAllByFarm_UuidOrderByNameAsc(farm).stream().map(this::accountResponse).toList();
    }

    @Transactional
    public FinanceDtos.AccountResponse createAccount(FinanceDtos.AccountRequest request) {
        requireFinanceRole(); Farm farm=lockedFarm();
        String name=request.name().trim();
        if(accounts.existsByFarm_UuidAndNameIgnoreCase(farm.getUuid(),name))
            throw new ConflictException("A finance account with this name already exists.");
        FinanceAccount account=new FinanceAccount(); account.setFarm(farm); account.setName(name);
        account.setType(request.type()); account.setOpeningBalance(money(request.openingBalance()));
        return accountResponse(accounts.save(account));
    }

    @Transactional
    public FinanceDtos.AccountResponse updateAccountStatus(UUID uuid,FinanceDtos.AccountStatusRequest request) {
        requireFinanceRole(); lockedFarm(); FinanceAccount account=account(uuid);
        account.setStatus(request.active());
        return accountResponse(accounts.save(account));
    }

    @Transactional(readOnly=true)
    public List<FinanceDtos.TransactionResponse> transactions(LocalDate from,LocalDate to) {
        validateRange(from,to);
        return transactions.findAllByFarm_UuidAndDateBetweenOrderByDateDescCreatedAtDesc(farmUuid(),from,to)
                .stream().map(this::transactionResponse).toList();
    }

    @Transactional
    public FinanceDtos.TransactionResponse createTransaction(FinanceDtos.TransactionRequest request) {
        requireFinanceRole(); Farm farm=lockedFarm(); FinanceAccount account=account(request.accountUuid());
        validateCategory(request.type(),request.category());
        if(!account.isStatus()) throw new IllegalArgumentException("Select an active finance account.");
        if(request.date().isAfter(today())) throw new IllegalArgumentException("Finance transaction date cannot be in the future.");
        FinanceTransaction row=new FinanceTransaction(); row.setFarm(farm); row.setAccount(account);
        row.setType(request.type()); row.setCategory(request.category()); row.setDate(request.date());
        row.setAmount(money(request.amount())); row.setDescription(request.description().trim());
        row.setNotes(clean(request.notes())); row.setRecordedBy(currentUser.get());
        return transactionResponse(transactions.save(row));
    }

    @Transactional(readOnly=true)
    public List<FinanceDtos.RecurringResponse> recurring() {
        return templates.findAllByFarm_UuidOrderByNextDateAsc(farmUuid()).stream().map(this::templateResponse).toList();
    }

    @Transactional
    public FinanceDtos.RecurringResponse createRecurring(FinanceDtos.RecurringRequest request) {
        requireFinanceRole(); Farm farm=lockedFarm(); FinanceAccount account=account(request.accountUuid());
        validateCategory(request.type(),request.category());
        if(request.endDate()!=null&&request.endDate().isBefore(request.startDate()))
            throw new IllegalArgumentException("Recurring end date must be on or after start date.");
        if(!account.isStatus()) throw new IllegalArgumentException("Select an active finance account.");
        RecurringFinanceTemplate row=new RecurringFinanceTemplate(); row.setFarm(farm); row.setName(request.name().trim());
        row.setAccount(account); row.setType(request.type()); row.setCategory(request.category());
        row.setFrequency(request.frequency()); row.setAmount(money(request.amount()));
        row.setStartDate(request.startDate()); row.setEndDate(request.endDate());
        row.setNextDate(request.startDate()); row.setStatus(FinanceTemplateStatus.ACTIVE);
        row.setCreatedBy(currentUser.get());
        return templateResponse(templates.save(row));
    }

    @Transactional
    public FinanceDtos.RecurringResponse updateRecurringStatus(UUID uuid,FinanceDtos.StatusRequest request) {
        requireFinanceRole(); lockedFarm(); RecurringFinanceTemplate row=template(uuid);
        if(row.getStatus()==FinanceTemplateStatus.COMPLETED&&request.status()!=FinanceTemplateStatus.COMPLETED)
            throw new ConflictException("A completed recurring transaction cannot be reactivated.");
        row.setStatus(request.status());
        return templateResponse(templates.save(row));
    }

    @Transactional
    public FinanceDtos.GenerationResponse generateDue() {
        requireFinanceRole();
        return generateForFarm(farmUuid(),today());
    }

    @Transactional
    public void generateDueForAllFarms() {
        LocalDate date=today();
        templates.findAllByStatusAndNextDateLessThanEqualOrderByNextDateAsc(FinanceTemplateStatus.ACTIVE,date)
                .stream().map(row->row.getFarm().getUuid()).distinct().forEach(farm->generateForFarm(farm,date));
    }

    @Transactional(readOnly=true)
    public FinanceDtos.SummaryResponse summary() {
        UUID farm=farmUuid(); BigDecimal income=ZERO,expenses=ZERO,accountBalances=ZERO;
        for(FinanceTransaction row:transactions.findAllByFarm_UuidOrderByDateAscCreatedAtAsc(farm)) {
            if(row.getType()==FinanceType.INCOME) income=income.add(row.getAmount());
            else expenses=expenses.add(row.getAmount());
        }
        for(MilkSale sale:sales.findAllByFarm_UuidOrderBySaleDateAsc(farm))
            income=income.add(sale.getTotalAmount());
        for(MilkBill bill:milkBills.findAllByFarm_Uuid(farm))
            income=income.add(bill.getTotalAmount()).add(bill.getAdjustmentAmount());
        for(AavinSettlement settlement:settlements.findAllByFarm_Uuid(farm))
            income=income.add(settlement.getNetPayable());
        BigDecimal customerReceivables=zero(customerPayments.farmOutstanding(farm));
        BigDecimal aavinReceivables=ZERO;
        for(AavinSettlement settlement:settlements.findAllByFarm_Uuid(farm))
            aavinReceivables=aavinReceivables.add(settlement.getNetPayable()
                    .subtract(zero(aavinPayments.totalForSettlement(farm,settlement.getUuid()))).max(ZERO));
        BigDecimal supplierPayables=ZERO;
        for(FeedPurchase purchase:purchases.findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(farm)) {
            expenses=expenses.add(purchase.getTotalAmount());
            supplierPayables=supplierPayables.add(purchase.getTotalAmount().subtract(purchase.getAmountPaid()).max(ZERO));
        }
        for(FinanceAccount account:accounts.findAllByFarm_UuidOrderByNameAsc(farm))
            accountBalances=accountBalances.add(accountBalance(account));
        income=money(income); expenses=money(expenses);
        return new FinanceDtos.SummaryResponse(income,expenses,money(income.subtract(expenses)),
                money(customerReceivables),money(aavinReceivables),money(supplierPayables),money(accountBalances));
    }

    @Transactional(readOnly=true)
    public List<FinanceDtos.ReportRow> report(LocalDate from,LocalDate to) {
        validateRange(from,to); UUID farm=farmUuid(); List<FinanceDtos.ReportRow> rows=new ArrayList<>();
        for(FinanceTransaction row:transactions.findAllByFarm_UuidAndDateBetweenOrderByDateDescCreatedAtDesc(farm,from,to))
            rows.add(new FinanceDtos.ReportRow(row.getDate(),row.getType().name(),row.getCategory().name(),
                    row.getDescription(),row.getAccount().getName(),
                    row.getType()==FinanceType.INCOME?row.getAmount():ZERO,
                    row.getType()==FinanceType.EXPENSE?row.getAmount():ZERO,
                    row.getRecurringTemplate()==null?"MANUAL":"RECURRING"));
        for(MilkSale sale:sales.findAllByFarm_UuidAndSaleDateBetweenOrderBySaleDateAsc(farm,from,to))
            rows.add(new FinanceDtos.ReportRow(sale.getSaleDate(),"INCOME","MILK_SALES",
                    "Invoice "+sale.getInvoiceNumber()+" · "+sale.getCustomer().getName(),null,
                    sale.getTotalAmount(),ZERO,"MILK_SALES"));
        for(MilkBill bill:milkBills.findAllByFarm_Uuid(farm))
            if(!bill.getPeriodEnd().isBefore(from)&&!bill.getPeriodEnd().isAfter(to))
                rows.add(new FinanceDtos.ReportRow(bill.getPeriodEnd(),"INCOME","SUBSCRIPTION_MILK",
                        "Invoice "+bill.getInvoiceNumber()+" / "+bill.getCustomer().getName(),null,
                        bill.getTotalAmount().add(bill.getAdjustmentAmount()),ZERO,"MILK_BILL"));
        for(FeedPurchase purchase:purchases.findAllByFarm_UuidAndPurchaseDateBetweenOrderByPurchaseDateAsc(farm,from,to))
            rows.add(new FinanceDtos.ReportRow(purchase.getPurchaseDate(),"EXPENSE","FEED_MEDICINE_PURCHASE",
                    purchase.getSupplier().getName()+" · "+purchase.getInvoiceNumber(),null,
                    ZERO,purchase.getTotalAmount(),"INVENTORY_PURCHASE"));
        for(AavinSettlement settlement:settlements.findAllByFarm_Uuid(farm))
            if(!settlement.getPeriodEnd().isBefore(from)&&!settlement.getPeriodEnd().isAfter(to))
                rows.add(new FinanceDtos.ReportRow(settlement.getPeriodEnd(),"INCOME","AAVIN_MILK",
                        "Settlement "+settlement.getSettlementNumber(),null,settlement.getNetPayable(),ZERO,"AAVIN_SETTLEMENT"));
        rows.sort((a,b)->b.date().compareTo(a.date()));
        return rows;
    }

    private FinanceDtos.GenerationResponse generateForFarm(UUID farmUuid,LocalDate through) {
        Farm farm=farms.findLockedByUuid(farmUuid).orElseThrow(()->new NotFoundException("Farm not found."));
        int generated=0;
        for(RecurringFinanceTemplate row:templates.findAllByFarm_UuidOrderByNextDateAsc(farmUuid)) {
            if(row.getStatus()!=FinanceTemplateStatus.ACTIVE) continue;
            int limit=0;
            while(!row.getNextDate().isAfter(through)&&limit<366) {
                LocalDate occurrence=row.getNextDate();
                if(row.getEndDate()!=null&&occurrence.isAfter(row.getEndDate())) {
                    row.setStatus(FinanceTemplateStatus.COMPLETED); break;
                }
                if(!transactions.existsByRecurringTemplate_UuidAndOccurrenceDate(row.getUuid(),occurrence)) {
                    FinanceTransaction transaction=new FinanceTransaction(); transaction.setFarm(farm);
                    transaction.setAccount(row.getAccount()); transaction.setType(row.getType());
                    transaction.setCategory(row.getCategory()); transaction.setDate(occurrence);
                    transaction.setAmount(row.getAmount()); transaction.setDescription(row.getName());
                    transaction.setRecurringTemplate(row); transaction.setOccurrenceDate(occurrence);
                    transaction.setRecordedBy(row.getCreatedBy()); transactions.save(transaction); generated++;
                }
                row.setNextDate(nextDate(occurrence,row.getFrequency())); limit++;
                if(row.getEndDate()!=null&&row.getNextDate().isAfter(row.getEndDate()))
                    row.setStatus(FinanceTemplateStatus.COMPLETED);
            }
            templates.save(row);
        }
        return new FinanceDtos.GenerationResponse(generated,through);
    }

    private LocalDate nextDate(LocalDate date,FinanceFrequency frequency) {
        return switch(frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    private FinanceDtos.AccountResponse accountResponse(FinanceAccount account) {
        return new FinanceDtos.AccountResponse(account.getUuid(),account.getName(),account.getType(),
                money(account.getOpeningBalance()),accountBalance(account),account.isStatus());
    }
    private BigDecimal accountBalance(FinanceAccount account) {
        BigDecimal balance=account.getOpeningBalance();
        for(FinanceTransaction transaction:transactions.findAllByFarm_UuidAndAccount_Uuid(
                account.getFarm().getUuid(),account.getUuid()))
            balance=balance.add(transaction.getType()==FinanceType.INCOME
                    ?transaction.getAmount():transaction.getAmount().negate());
        return money(balance);
    }
    private FinanceDtos.TransactionResponse transactionResponse(FinanceTransaction row) {
        return new FinanceDtos.TransactionResponse(row.getUuid(),row.getAccount().getUuid(),row.getAccount().getName(),
                row.getType(),row.getCategory(),row.getDate(),row.getAmount(),row.getDescription(),row.getNotes(),
                row.getRecurringTemplate()==null?"MANUAL":"RECURRING",row.getCreatedAt());
    }
    private FinanceDtos.RecurringResponse templateResponse(RecurringFinanceTemplate row) {
        return new FinanceDtos.RecurringResponse(row.getUuid(),row.getName(),row.getAccount().getUuid(),
                row.getAccount().getName(),row.getType(),row.getCategory(),row.getFrequency(),row.getAmount(),
                row.getStartDate(),row.getEndDate(),row.getNextDate(),row.getStatus());
    }
    private void validateCategory(FinanceType type,FinanceCategory category) {
        if(category.getType()!=type) throw new IllegalArgumentException("The selected category does not match the transaction type.");
    }
    private FinanceAccount account(UUID uuid) {
        return accounts.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Finance account not found in this farm."));
    }
    private RecurringFinanceTemplate template(UUID uuid) {
        return templates.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Recurring transaction not found in this farm."));
    }
    private Farm lockedFarm() {
        return farms.findLockedByUuid(farmUuid()).orElseThrow(()->new NotFoundException("Farm not found."));
    }
    private UUID farmUuid() { return currentUser.farmUuid(); }
    private LocalDate today() { return LocalDate.now(clock); }
    private void validateRange(LocalDate from,LocalDate to) {
        if(from==null||to==null||to.isBefore(from)) throw new IllegalArgumentException("Provide a valid date range.");
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
    private BigDecimal zero(BigDecimal value) { return value==null?ZERO:value; }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private void requireFinanceRole() {
        if(currentUser.get().getRoles().stream().noneMatch(role->role.getName()==UserRole.ADMIN
                ||role.getName()==UserRole.MANAGER||role.getName()==UserRole.ACCOUNTANT))
            throw new AccessDeniedException("Administrator, manager, or accountant access is required.");
    }
}
