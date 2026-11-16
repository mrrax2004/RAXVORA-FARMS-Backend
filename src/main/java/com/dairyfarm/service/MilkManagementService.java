package com.dairyfarm.service;

import com.dairyfarm.dto.PhaseThreeDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilkManagementService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final MilkProductionRepository production;
    private final MilkProductionAuditRepository audits;
    private final MilkCollectionRepository collections;
    private final MilkStockTransactionRepository stockTransactions;
    private final CustomerRepository customers;
    private final CustomerMilkRateRepository customerMilkRates;
    private final MilkSaleRepository sales;
    private final MilkSaleItemRepository saleItems;
    private final MilkSubscriptionRepository subscriptions;
    private final MilkDeliveryRepository deliveries;
    private final CustomerPaymentRepository payments;
    private final MilkBillRepository bills;
    private final MilkBillItemRepository billItems;
    private final MilkBillAdjustmentRepository billAdjustments;
    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    private final MilkStockService milkStock;
    private final Clock clock;

    public MilkManagementService(MilkProductionRepository production, MilkProductionAuditRepository audits,
            MilkCollectionRepository collections,
            MilkStockTransactionRepository stockTransactions, CustomerRepository customers, MilkSaleRepository sales,
            MilkSaleItemRepository saleItems, MilkSubscriptionRepository subscriptions, MilkDeliveryRepository deliveries,
            CustomerPaymentRepository payments, MilkBillRepository bills, MilkBillItemRepository billItems,
            MilkBillAdjustmentRepository billAdjustments,
            AnimalRepository animals, FarmRepository farms,
            CustomerMilkRateRepository customerMilkRates, CurrentUserService currentUser,
            MilkStockService milkStock, Clock clock) {
        this.production = production; this.audits = audits; this.collections = collections;
        this.stockTransactions = stockTransactions; this.customers = customers; this.customerMilkRates = customerMilkRates;
        this.sales = sales; this.saleItems = saleItems;
        this.subscriptions = subscriptions; this.deliveries = deliveries; this.payments = payments;
        this.bills = bills; this.billItems = billItems; this.billAdjustments = billAdjustments; this.animals = animals;
        this.farms = farms; this.currentUser = currentUser; this.milkStock = milkStock; this.clock = clock;
    }

    @Transactional
    public void lactating(UUID animalUuid, boolean lactating) {
        Animal animal = animal(animalUuid);
        if (lactating && (animal.getType() != AnimalType.COW || animal.getGender() != Gender.FEMALE
                || animal.getStatus() != AnimalStatus.ACTIVE))
            throw new IllegalArgumentException("Only active female cows can be marked as lactating.");
        animal.setLactating(lactating);
        animals.save(animal);
    }

    @Transactional
    public PhaseThreeDtos.ProductionResponse recordProduction(PhaseThreeDtos.ProductionRequest request) {
        Farm farm = lockFarm();
        Animal animal = animal(request.animalUuid());
        eligibleForMilk(animal);
        if (request.productionDate().isAfter(today())) throw new IllegalArgumentException("Production date cannot be in the future.");
        MilkProduction row = new MilkProduction();
        row.setFarm(farm); row.setAnimal(animal); row.setProductionDate(request.productionDate());
        row.setSession(request.session()); row.setQuantity(money(request.quantity())); row.setRecordedBy(currentUser.get());
        row.setNotes(request.notes());
        MilkProduction saved = production.saveAndFlush(row);
        if (saved.getQuantity().signum() > 0) {
            stock(farm, MilkStockTransactionType.PRODUCTION, saved.getQuantity(), null, null, null,
                    "Milk production " + saved.getProductionDate() + " · " + saved.getSession(), saved);
        }
        return productionResponse(saved);
    }

    @Transactional
    public java.util.List<PhaseThreeDtos.ProductionResponse> recordBatchProduction(PhaseThreeDtos.BatchProductionRequest request) {
        if (request.productionDate().isAfter(today())) throw new IllegalArgumentException("Production date cannot be in the future.");
        Farm farm = lockFarm();
        java.util.List<PhaseThreeDtos.ProductionResponse> responses = new java.util.ArrayList<>();
        BigDecimal totalBatchQuantity = BigDecimal.ZERO;
        java.util.Set<UUID> seenAnimals = new java.util.HashSet<>();
        for (PhaseThreeDtos.BatchProductionItem item : request.items()) {
            if (!seenAnimals.add(item.animalUuid())) {
                throw new ConflictException("Duplicate animal entry in the same session batch.");
            }
            Animal animal = animal(item.animalUuid());
            eligibleForMilk(animal);
            MilkProduction row = new MilkProduction();
            row.setFarm(farm); row.setAnimal(animal); row.setProductionDate(request.productionDate());
            row.setSession(request.session()); row.setQuantity(money(item.quantity())); row.setRecordedBy(currentUser.get());
            row.setNotes(item.notes());
            MilkProduction saved = production.saveAndFlush(row);
            if (saved.getQuantity().signum() > 0) {
                totalBatchQuantity = totalBatchQuantity.add(saved.getQuantity());
                stock(farm, MilkStockTransactionType.PRODUCTION, saved.getQuantity(), null, null, null,
                        "Milk production " + saved.getProductionDate() + " · " + saved.getSession() + " · " + animal.getTagNumber(), saved);
            }
            responses.add(productionResponse(saved));
        }
        return responses;
    }

    @Transactional
    public PhaseThreeDtos.ProductionResponse correctProduction(UUID uuid, PhaseThreeDtos.ProductionCorrection request) {
        Farm farm = lockFarm();
        MilkProduction row = production.findById(uuid).filter(p -> p.getFarm().getUuid().equals(farmUuid()))
                .orElseThrow(() -> new NotFoundException("Milk production record not found."));
        BigDecimal previous = row.getQuantity();
        row.setQuantity(money(request.quantity()));
        BigDecimal delta = row.getQuantity().subtract(previous);
        if (delta.signum() != 0 && stockTransactions.existsByProduction_UuidAndType(row.getUuid(), MilkStockTransactionType.PRODUCTION)) {
            if (delta.signum() < 0 && stockTransactions.stock(farm.getUuid()).compareTo(delta.abs()) < 0)
                throw new ConflictException("This correction would reduce milk stock below zero.");
            stock(farm, MilkStockTransactionType.CORRECTION, delta, null, null, null,
                    "Production correction " + row.getProductionDate() + " · " + request.correctionReason().trim(), row);
        }
        production.save(row);
        MilkProductionAudit audit = new MilkProductionAudit();
        audit.setFarm(row.getFarm()); audit.setProduction(row); audit.setEditedBy(currentUser.get());
        audit.setPreviousQuantity(previous); audit.setNewQuantity(row.getQuantity());
        audit.setCorrectionReason(request.correctionReason().trim()); audits.save(audit);
        return productionResponse(row);
    }

    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.ProductionResponse> production(LocalDate from, LocalDate to, UUID animalUuid, int page, int size) {
        range(from, to);
        Page<MilkProduction> rows = animalUuid == null
                ? production.findAllByFarm_UuidAndProductionDateBetween(farmUuid(), from, to, page(page, size))
                : production.findAllByFarm_UuidAndAnimal_UuidAndProductionDateBetween(farmUuid(), animalUuid, from, to, page(page, size));
        return rows.map(MilkManagementService::productionResponse);
    }
    @Transactional(readOnly = true)
    public List<PhaseThreeDtos.ProductionAuditResponse> productionAudit(UUID uuid) {
        MilkProduction row = production.findById(uuid).filter(p -> p.getFarm().getUuid().equals(farmUuid()))
                .orElseThrow(() -> new NotFoundException("Milk production record not found."));
        return audits.findAllByFarm_UuidAndProduction_UuidOrderByCreatedAtDesc(farmUuid(), row.getUuid()).stream()
                .map(a -> new PhaseThreeDtos.ProductionAuditResponse(a.getUuid(), a.getPreviousQuantity(), a.getNewQuantity(),
                        a.getCorrectionReason(), a.getEditedBy().getFullName(), a.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.CollectionResponse> collections(LocalDate from, LocalDate to, int page, int size) {
        range(from,to); return collections.findAllByFarm_UuidAndCollectionDateBetween(farmUuid(),from,to,page(page,size))
                .map(MilkManagementService::collectionResponse);
    }

    @Transactional
    public PhaseThreeDtos.StockResponse openingStock(PhaseThreeDtos.StockAdjustment request) {
        Farm farm = lockFarm();
        if (stockTransactions.existsByFarm_UuidAndType(farm.getUuid(), MilkStockTransactionType.OPENING))
            throw new ConflictException("Opening stock can only be recorded once.");
        stock(farm, MilkStockTransactionType.OPENING, money(request.quantity()), null, null, null, request.note().trim());
        return new PhaseThreeDtos.StockResponse(stockTransactions.stock(farm.getUuid()));
    }
    @Transactional
    public PhaseThreeDtos.StockResponse stockAdjustment(MilkStockTransactionType type, PhaseThreeDtos.StockAdjustment request) {
        if (type != MilkStockTransactionType.USED && type != MilkStockTransactionType.WASTE && type != MilkStockTransactionType.CORRECTION)
            throw new IllegalArgumentException("Use USED, WASTE, or CORRECTION for stock adjustments.");
        Farm farm = lockFarm();
        BigDecimal amount = money(request.quantity());
        BigDecimal change = type == MilkStockTransactionType.CORRECTION ? amount : amount.negate();
        if (stockTransactions.stock(farm.getUuid()).add(change).signum() < 0)
            throw new ConflictException("Milk stock cannot become negative.");
        stock(farm, type, change, null, null, null, request.note().trim());
        return new PhaseThreeDtos.StockResponse(stockTransactions.stock(farm.getUuid()));
    }
    @Transactional(readOnly = true)
    public PhaseThreeDtos.StockResponse stock() { return new PhaseThreeDtos.StockResponse(stockTransactions.stock(farmUuid())); }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.StockTransactionResponse> stockTransactions(int page, int size) {
        return stockTransactions.findAllByFarm_UuidOrderByCreatedAtDesc(farmUuid(), page(page,size))
                .map(t -> new PhaseThreeDtos.StockTransactionResponse(t.getUuid(),t.getType(),t.getQuantityChange(),
                        t.getCollection()==null?null:t.getCollection().getUuid(),
                        t.getProduction()==null?null:t.getProduction().getUuid(),t.getSale()==null?null:t.getSale().getUuid(),
                        t.getDelivery()==null?null:t.getDelivery().getUuid(),t.getSupply()==null?null:t.getSupply().getUuid(),
                        t.getReferenceNote(),t.getRecordedBy().getFullName(),t.getCreatedAt()));
    }

    @Transactional
    public PhaseThreeDtos.CustomerResponse createCustomer(PhaseThreeDtos.CustomerRequest request) {
        Customer customer = new Customer(); customer.setFarm(farm()); applyCustomer(customer,request);
        return customerResponse(customers.save(customer));
    }
    @Transactional
    public PhaseThreeDtos.CustomerResponse updateCustomer(UUID uuid, PhaseThreeDtos.CustomerRequest request) {
        Customer customer = customer(uuid); applyCustomer(customer,request); return customerResponse(customers.save(customer));
    }
    @Transactional(readOnly = true)
    public PhaseThreeDtos.CustomerResponse getCustomer(UUID uuid) { return customerResponse(customer(uuid)); }
    @Transactional(readOnly = true)
    public List<PhaseThreeDtos.CustomerMilkRateResponse> customerMilkRates(UUID customerUuid) {
        Customer customer = customer(customerUuid);
        return customerMilkRates.findAllByFarm_UuidAndCustomer_UuidOrderByEffectiveFromDescCreatedAtDesc(
                farmUuid(), customer.getUuid()).stream().map(MilkManagementService::customerMilkRateResponse).toList();
    }
    @Transactional(readOnly = true)
    public PhaseThreeDtos.MilkRateQuoteResponse milkRateQuote(UUID customerUuid, LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Rate date is required.");
        Customer customer = customer(customerUuid);
        RateSelection selection = selectRate(customer, date);
        return new PhaseThreeDtos.MilkRateQuoteResponse(customer.getUuid(), date, selection.rate(),
                selection.customerRate() == null ? "FARM_DEFAULT" : "CUSTOMER_FIXED",
                selection.customerRate() == null ? null : selection.customerRate().getUuid());
    }
    @Transactional
    public PhaseThreeDtos.CustomerMilkRateResponse createCustomerMilkRate(UUID customerUuid,
            PhaseThreeDtos.CustomerMilkRateRequest request) {
        Farm farm = lockFarm();
        Customer customer = customer(customerUuid);
        validateRatePeriod(request);
        validateRateOverlap(farm.getUuid(), customerUuid, null, request);
        CustomerMilkRate rate = new CustomerMilkRate();
        rate.setFarm(farm); rate.setCustomer(customer); rate.setCreatedBy(currentUser.get());
        applyRate(rate, request);
        return customerMilkRateResponse(customerMilkRates.save(rate));
    }
    @Transactional
    public PhaseThreeDtos.CustomerMilkRateResponse updateCustomerMilkRate(UUID customerUuid, UUID rateUuid,
            PhaseThreeDtos.CustomerMilkRateRequest request) {
        Farm farm = lockFarm();
        CustomerMilkRate rate = customerMilkRates.findByUuidAndFarm_UuidAndCustomer_Uuid(rateUuid, farm.getUuid(), customerUuid)
                .orElseThrow(() -> new NotFoundException("Customer milk rate not found."));
        validateRatePeriod(request);
        validateRateOverlap(farm.getUuid(), customerUuid, rateUuid, request);
        applyRate(rate, request);
        return customerMilkRateResponse(customerMilkRates.save(rate));
    }
    @Transactional
    public PhaseThreeDtos.CustomerMilkRateResponse customerMilkRateStatus(UUID customerUuid, UUID rateUuid,
            CustomerMilkRateStatus status) {
        Farm farm = lockFarm();
        CustomerMilkRate rate = customerMilkRates.findByUuidAndFarm_UuidAndCustomer_Uuid(rateUuid, farm.getUuid(), customerUuid)
                .orElseThrow(() -> new NotFoundException("Customer milk rate not found."));
        if (status == CustomerMilkRateStatus.ACTIVE) {
            PhaseThreeDtos.CustomerMilkRateRequest request = new PhaseThreeDtos.CustomerMilkRateRequest(
                    rate.getRatePerLitre(), rate.getEffectiveFrom(), rate.getEffectiveUntil(), status, rate.getNotes());
            validateRateOverlap(farm.getUuid(), customerUuid, rateUuid, request);
        }
        rate.setStatus(status);
        return customerMilkRateResponse(customerMilkRates.save(rate));
    }
    @Transactional(readOnly = true)
    public PhaseThreeDtos.DefaultMilkRateResponse defaultMilkRate() {
        Farm farm = farms.findById(farmUuid()).orElseThrow(() -> new NotFoundException("Farm not found."));
        return new PhaseThreeDtos.DefaultMilkRateResponse(zero(farm.getDefaultMilkRate()));
    }
    @Transactional
    public PhaseThreeDtos.DefaultMilkRateResponse updateDefaultMilkRate(PhaseThreeDtos.DefaultMilkRateRequest request) {
        Farm farm = lockFarm();
        farm.setDefaultMilkRate(money(request.ratePerLitre()));
        farms.save(farm);
        return new PhaseThreeDtos.DefaultMilkRateResponse(farm.getDefaultMilkRate());
    }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.CustomerResponse> customers(String search, CustomerStatus status, CustomerType type, int page, int size) {
        String query = search == null ? "" : search.trim();
        Page<Customer> result;
        if(status!=null && type!=null) result=customers.findAllByFarm_UuidAndNameContainingIgnoreCaseAndStatusAndType(farmUuid(),query,status,type,page(page,size));
        else if(status!=null) result=customers.findAllByFarm_UuidAndNameContainingIgnoreCaseAndStatus(farmUuid(),query,status,page(page,size));
        else if(type!=null) result=customers.findAllByFarm_UuidAndNameContainingIgnoreCaseAndType(farmUuid(),query,type,page(page,size));
        else result=customers.findAllByFarm_UuidAndNameContainingIgnoreCase(farmUuid(),query,page(page,size));
        return result.map(this::customerResponse);
    }

    @Transactional
    public PhaseThreeDtos.SaleResponse createSale(PhaseThreeDtos.SaleRequest request) {
        Farm farm = lockFarm();
        Customer customer = customer(request.customerUuid());
        if (customer.getStatus()!=CustomerStatus.ACTIVE) throw new ConflictException("Sales cannot be recorded for an inactive customer.");
        if (request.saleDate().isAfter(today())) throw new IllegalArgumentException("Sale date cannot be in the future.");
        RateSelection selectedRate = selectRate(customer, request.saleDate());
        BigDecimal originalRate = selectedRate.rate();
        if (originalRate == null)
            throw new ConflictException("No active milk rate is configured for this customer on the sale date.");
        boolean overridden = request.overrideRate() != null;
        if (overridden && !currentUser.isAdmin(currentUser.get()))
            throw new org.springframework.security.access.AccessDeniedException("Only administrators may override a sale rate.");
        if (overridden && (request.overrideReason() == null || request.overrideReason().isBlank()))
            throw new IllegalArgumentException("A reason is required when overriding the sale rate.");
        if (!overridden && request.overrideReason() != null && !request.overrideReason().isBlank())
            throw new IllegalArgumentException("An override reason requires an override rate.");
        BigDecimal appliedRate = overridden ? money(request.overrideRate()) : originalRate;
        BigDecimal amount = money(request.quantity().multiply(appliedRate).subtract(request.discount()));
        if (amount.signum()<0) throw new IllegalArgumentException("Discount cannot exceed the sale amount.");
        BigDecimal initial = money(request.initialPayment()==null?ZERO:request.initialPayment());
        if (initial.compareTo(amount)>0) throw new IllegalArgumentException("Initial payment cannot exceed invoice total.");
        BigDecimal currentStock=stockTransactions.stock(farm.getUuid());
        if(currentStock.compareTo(request.quantity())<0) throw new ConflictException("Insufficient milk stock for this sale.");
        MilkSale sale = new MilkSale(); sale.setFarm(farm); sale.setInvoiceNumber(request.invoiceNumber().trim());
        sale.setCustomer(customer); sale.setSaleDate(request.saleDate()); sale.setQuantity(money(request.quantity()));
        sale.setSaleTime(request.saleTime());
        sale.setUnitPrice(appliedRate); sale.setOriginalUnitPrice(originalRate);
        sale.setCustomerMilkRate(selectedRate.customerRate());
        if (overridden) {
            sale.setRateOverrideBy(currentUser.get());
            sale.setRateOverrideReason(request.overrideReason().trim());
        }
        sale.setDiscount(money(request.discount())); sale.setTotalAmount(amount);
        sale.setPaymentStatus(initial.compareTo(amount)>=0?PaymentStatus.PAID:initial.signum()>0?PaymentStatus.PARTIAL:PaymentStatus.UNPAID);
        sale.setPaymentMethod(request.paymentMethod()); sale.setCreatedBy(currentUser.get()); sale.setNotes(request.notes());
        MilkSale saved=sales.saveAndFlush(sale);
        MilkSaleItem item=new MilkSaleItem(); item.setSale(saved); item.setQuantity(saved.getQuantity()); item.setUnitPrice(saved.getUnitPrice());
        item.setDiscount(saved.getDiscount()); item.setLineTotal(saved.getTotalAmount()); saleItems.save(item);
        stock(farm,MilkStockTransactionType.SALE,saved.getQuantity().negate(),null,saved,null,"Invoice "+saved.getInvoiceNumber());
        if(initial.signum()>0) recordPayment(farm,customer,saved,null,request.saleDate(),initial,request.paymentMethod(),null,"Initial invoice payment");
        return saleResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.SaleResponse> sales(LocalDate from, LocalDate to, UUID customerUuid, int page, int size) {
        range(from,to);
        Page<MilkSale> result=customerUuid==null?sales.findAllByFarm_UuidAndSaleDateBetween(farmUuid(),from,to,page(page,size))
                :sales.findAllByFarm_UuidAndCustomer_UuidAndSaleDateBetween(farmUuid(),customerUuid,from,to,page(page,size));
        return result.map(this::saleResponse);
    }
    @Transactional(readOnly = true)
    public PhaseThreeDtos.SaleResponse sale(UUID uuid) {
        return saleResponse(sales.findByUuidAndFarm_Uuid(uuid,farmUuid()).orElseThrow(()->new NotFoundException("Invoice not found.")));
    }

    @Transactional
    public PhaseThreeDtos.PaymentResponse payment(PhaseThreeDtos.PaymentRequest request) {
        Farm farm=lockFarm(); Customer customer=customer(request.customerUuid());
        if(request.paymentDate().isAfter(today())) throw new IllegalArgumentException("Payment date cannot be in the future.");
        if(request.transactionReference()!=null && !request.transactionReference().isBlank()
                && payments.existsByFarm_UuidAndTransactionReference(farm.getUuid(),request.transactionReference().trim()))
            throw new ConflictException("This payment reference has already been recorded for this farm.");
        if (request.saleUuid() != null && request.billUuid() != null)
            throw new IllegalArgumentException("A payment can be applied to either a sale invoice or a subscription bill, not both.");
        MilkSale sale=null;
        MilkBill bill=null;
        if(request.saleUuid()!=null) {
            sale=sales.findByUuidAndFarm_Uuid(request.saleUuid(),farm.getUuid()).orElseThrow(()->new NotFoundException("Invoice not found."));
            if(!sale.getCustomer().getUuid().equals(customer.getUuid())) throw new IllegalArgumentException("Invoice does not belong to this customer.");
            BigDecimal paid=payments.paidForSale(sale.getUuid());
            if(paid.add(request.amount()).compareTo(sale.getTotalAmount())>0) throw new ConflictException("Payment exceeds the invoice's outstanding balance.");
        } else if (request.billUuid() != null) {
            bill = bills.findByUuidAndFarm_Uuid(request.billUuid(), farm.getUuid())
                    .orElseThrow(() -> new NotFoundException("Milk bill not found."));
            if (!bill.getCustomer().getUuid().equals(customer.getUuid()))
                throw new IllegalArgumentException("Milk bill does not belong to this customer.");
            BigDecimal paid = zero(payments.paidForBill(bill.getUuid()));
            if (paid.add(request.amount()).compareTo(bill.getTotalAmount().add(bill.getAdjustmentAmount())) > 0)
                throw new ConflictException("Payment exceeds the bill's outstanding balance.");
        } else if (payments.outstanding(customer.getUuid()).compareTo(request.amount())<0)
            throw new ConflictException("Payment exceeds this customer's outstanding balance.");
        CustomerPayment saved=recordPayment(farm,customer,sale,bill,request.paymentDate(),money(request.amount()),request.method(),
                request.transactionReference(),request.notes());
        if(sale!=null) {
            BigDecimal paid=payments.paidForSale(sale.getUuid());
            sale.setPaymentStatus(paid.compareTo(sale.getTotalAmount())>=0?PaymentStatus.PAID:PaymentStatus.PARTIAL);
            sales.save(sale);
        }
        return paymentResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.PaymentResponse> payments(LocalDate from,LocalDate to,UUID customerUuid,int page,int size) {
        range(from,to);
        Page<CustomerPayment> result=customerUuid==null?payments.findAllByFarm_UuidAndPaymentDateBetween(farmUuid(),from,to,page(page,size))
                :payments.findAllByFarm_UuidAndCustomer_UuidAndPaymentDateBetween(farmUuid(),customerUuid,from,to,page(page,size));
        return result.map(MilkManagementService::paymentResponse);
    }

    @Transactional
    public PhaseThreeDtos.MilkBillResponse generateMilkBill(PhaseThreeDtos.GenerateMilkBillRequest request) {
        Farm farm = lockFarm();
        Customer customer = customer(request.customerUuid());
        MilkBillingFrequency frequency = customer.getBillingFrequency();
        LocalDate periodStart;
        LocalDate periodEnd;
        if (frequency == MilkBillingFrequency.CUSTOM_PERIOD) {
            if (request.periodStart() == null || request.periodEnd() == null || request.periodEnd().isBefore(request.periodStart()))
                throw new IllegalArgumentException("A custom bill requires a valid period start and end date.");
            if (request.periodStart().isBefore(customer.getStartDate()))
                throw new IllegalArgumentException("A custom bill cannot start before the customer start date.");
            periodStart = request.periodStart();
            periodEnd = request.periodEnd();
        } else {
            if (request.periodDate() == null)
                throw new IllegalArgumentException("A period date is required for the selected billing frequency.");
            if (request.periodStart() != null || request.periodEnd() != null)
                throw new IllegalArgumentException("Custom period dates are accepted only for CUSTOM_PERIOD billing.");
            LocalDate referenceDate = request.periodDate();
            if (referenceDate.isBefore(customer.getStartDate()))
                throw new IllegalArgumentException("The billing date cannot be before the customer start date.");
            switch (frequency) {
                case DAILY -> { periodStart = referenceDate; periodEnd = referenceDate; }
                case MONTHLY -> {
                    java.time.YearMonth month = java.time.YearMonth.from(referenceDate);
                    periodStart = month.atDay(1);
                    periodEnd = month.atEndOfMonth();
                }
                case FIRST_HALF_MONTH -> {
                    if (referenceDate.getDayOfMonth() > 15)
                        throw new IllegalArgumentException("Choose a date from the first half of the month.");
                    periodStart = referenceDate.withDayOfMonth(1);
                    periodEnd = referenceDate.withDayOfMonth(15);
                }
                case SECOND_HALF_MONTH -> {
                    if (referenceDate.getDayOfMonth() < 16)
                        throw new IllegalArgumentException("Choose a date from the second half of the month.");
                    periodStart = referenceDate.withDayOfMonth(16);
                    periodEnd = java.time.YearMonth.from(referenceDate).atEndOfMonth();
                }
                case EVERY_7_DAYS, EVERY_15_DAYS, EVERY_30_DAYS -> {
                    int intervalDays = switch (frequency) {
                        case EVERY_7_DAYS -> 7;
                        case EVERY_15_DAYS -> 15;
                        default -> 30;
                    };
                    long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(customer.getStartDate(), referenceDate);
                    long intervalIndex = elapsedDays / intervalDays;
                    periodStart = customer.getStartDate().plusDays(intervalIndex * intervalDays);
                    periodEnd = periodStart.plusDays(intervalDays - 1L);
                }
                case CUSTOM_PERIOD -> throw new IllegalStateException("Custom periods are resolved separately.");
                default -> throw new IllegalArgumentException("Unsupported billing frequency.");
            }
        }
        if (periodEnd.isAfter(today()))
            throw new IllegalArgumentException("The selected billing period must have ended before a bill can be generated.");

        var existing = bills.findByFarm_UuidAndCustomer_UuidAndBillingFrequencyAndPeriodStartAndPeriodEnd(
                farm.getUuid(), customer.getUuid(), frequency, periodStart, periodEnd);
        if (existing.isPresent()) return milkBillResponse(existing.get());

        List<MilkDelivery> completedDeliveries = deliveries
                .findAllByFarm_UuidAndSubscription_Customer_UuidAndDeliveryDateBetweenAndStatusOrderByDeliveryDateAsc(
                        farm.getUuid(), customer.getUuid(), periodStart, periodEnd, MilkDeliveryStatus.DELIVERED)
                .stream().filter(delivery -> !billItems.existsByDelivery_Uuid(delivery.getUuid())).toList();
        if (completedDeliveries.isEmpty())
            throw new ConflictException("No unbilled completed subscription deliveries exist in this period.");

        MilkBill bill = new MilkBill();
        bill.setFarm(farm);
        bill.setCustomer(customer);
        bill.setBillingFrequency(frequency);
        bill.setPeriodStart(periodStart);
        bill.setPeriodEnd(periodEnd);
        bill.setPreviousOutstanding(zero(payments.outstanding(customer.getUuid())).max(ZERO));
        bill.setGeneratedBy(currentUser.get());
        bill.setInvoiceNumber("MB-" + periodStart.toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT));
        BigDecimal totalQuantity = completedDeliveries.stream().map(MilkDelivery::getDeliveredQuantity)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalAmount = completedDeliveries.stream()
                .map(delivery -> money(delivery.getDeliveredQuantity().multiply(delivery.getUnitPrice())))
                .reduce(ZERO, BigDecimal::add);
        bill.setTotalQuantity(money(totalQuantity));
        bill.setTotalAmount(money(totalAmount));
        MilkBill saved = bills.saveAndFlush(bill);
        for (MilkDelivery delivery : completedDeliveries) {
            MilkBillItem item = new MilkBillItem();
            item.setBill(saved);
            item.setDelivery(delivery);
            item.setQuantity(delivery.getDeliveredQuantity());
            item.setUnitPrice(delivery.getUnitPrice());
            item.setAmount(money(delivery.getDeliveredQuantity().multiply(delivery.getUnitPrice())));
            billItems.save(item);
        }
        return milkBillResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.MilkBillResponse> milkBills(LocalDate from, LocalDate to, UUID customerUuid, int page, int size) {
        range(from, to);
        Page<MilkBill> result = customerUuid == null
                ? bills.findAllByFarm_UuidAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                        farmUuid(), from, to, page(page, size))
                : bills.findAllByFarm_UuidAndCustomer_UuidAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                        farmUuid(), customerUuid, from, to, page(page, size));
        return result.map(this::milkBillResponse);
    }

    @Transactional(readOnly = true)
    public PhaseThreeDtos.MilkBillResponse milkBill(UUID uuid) {
        MilkBill bill = bills.findByUuidAndFarm_Uuid(uuid, farmUuid())
                .orElseThrow(() -> new NotFoundException("Milk bill not found."));
        return milkBillResponse(bill);
    }

    @Transactional
    public PhaseThreeDtos.BillAdjustmentResponse adjustMilkBill(UUID uuid, PhaseThreeDtos.BillAdjustmentRequest request) {
        Farm farm = lockFarm();
        MilkBill bill = bills.findByUuidAndFarm_Uuid(uuid, farm.getUuid())
                .orElseThrow(() -> new NotFoundException("Milk bill not found."));
        BigDecimal amount = money(request.amount());
        if (amount.signum() == 0) throw new IllegalArgumentException("An adjustment amount cannot be zero.");
        BigDecimal updatedAdjustment = bill.getAdjustmentAmount().add(amount);
        BigDecimal amountDue = bill.getTotalAmount().add(updatedAdjustment);
        BigDecimal paid = zero(payments.paidForBill(bill.getUuid()));
        if (amountDue.signum() < 0 || amountDue.compareTo(paid) < 0)
            throw new ConflictException("The adjustment cannot reduce the bill below its payments or below zero.");
        MilkBillAdjustment adjustment = new MilkBillAdjustment();
        adjustment.setFarm(farm); adjustment.setBill(bill); adjustment.setAmount(amount);
        adjustment.setReason(request.reason().trim()); adjustment.setAdjustedBy(currentUser.get());
        bill.setAdjustmentAmount(updatedAdjustment);
        bills.save(bill);
        return billAdjustmentResponse(billAdjustments.save(adjustment));
    }

    @Transactional
    public PhaseThreeDtos.SubscriptionResponse createSubscription(PhaseThreeDtos.SubscriptionRequest request) {
        Farm farm=lockFarm();
        if(request.endDate()!=null && request.endDate().isBefore(request.startDate())) throw new IllegalArgumentException("Subscription end date must be on or after start date.");
        Customer customer = customer(request.customerUuid());
        RateSelection selectedRate = selectRate(customer, request.startDate());
        MilkSubscription s=new MilkSubscription(); s.setFarm(farm); s.setCustomer(customer);
        s.setDailyQuantity(money(request.dailyQuantity())); s.setSession(request.session()); s.setStartDate(request.startDate());
        s.setEndDate(request.endDate()); s.setUnitPrice(selectedRate.rate()); s.setCreatedBy(currentUser.get());
        return subscriptionResponse(subscriptions.save(s));
    }
    @Transactional
    public PhaseThreeDtos.SubscriptionResponse updateSubscription(UUID uuid, PhaseThreeDtos.SubscriptionRequest request) {
        Farm farm=lockFarm();
        if(request.endDate()!=null && request.endDate().isBefore(request.startDate()))
            throw new IllegalArgumentException("Subscription end date must be on or after start date.");
        MilkSubscription s=subscriptions.findByUuidAndFarm_Uuid(uuid,farm.getUuid())
                .orElseThrow(()->new NotFoundException("Subscription not found."));
        Customer customer=customer(request.customerUuid());
        if (!s.getCustomer().getUuid().equals(customer.getUuid()) && deliveries.existsBySubscription_Uuid(s.getUuid()))
            throw new ConflictException("A subscription with delivery history cannot be reassigned to another customer.");
        s.setCustomer(customer); s.setDailyQuantity(money(request.dailyQuantity())); s.setSession(request.session());
        s.setStartDate(request.startDate()); s.setEndDate(request.endDate());
        s.setUnitPrice(selectRate(customer,request.startDate()).rate());
        return subscriptionResponse(subscriptions.save(s));
    }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.SubscriptionResponse> subscriptions(int page,int size) {
        return subscriptions.findAllByFarm_Uuid(farmUuid(),page(page,size)).map(MilkManagementService::subscriptionResponse);
    }
    @Transactional
    public PhaseThreeDtos.SubscriptionResponse subscriptionStatus(UUID uuid, SubscriptionStatus status) {
        MilkSubscription s=subscriptions.findByUuidAndFarm_Uuid(uuid,farmUuid()).orElseThrow(()->new NotFoundException("Subscription not found."));
        if(s.getStatus()==SubscriptionStatus.CANCELLED || s.getStatus()==SubscriptionStatus.COMPLETED)
            throw new ConflictException("A cancelled or completed subscription cannot be resumed.");
        s.setStatus(status); return subscriptionResponse(subscriptions.save(s));
    }
    @Transactional
    public int generateDeliveries(LocalDate date) {
        if (date == null) date = today();
        if(date.isAfter(today())) throw new IllegalArgumentException("Cannot generate deliveries for a future date.");
        return generateForFarm(farmUuid(),date);
    }
    @Transactional
    public int generateForFarm(UUID farmUuid, LocalDate date) {
        farms.findLockedByUuid(farmUuid).orElseThrow(()->new NotFoundException("Farm not found."));
        List<MilkSubscription> active=subscriptions.activeOnDate(farmUuid,SubscriptionStatus.ACTIVE,date);
        int created=0;
        for(MilkSubscription s:active) {
            if(s.getStartDate().isAfter(date) || (s.getEndDate()!=null && s.getEndDate().isBefore(date))) continue;
            if(deliveries.existsBySubscription_UuidAndDeliveryDate(s.getUuid(),date)) continue;
            MilkDelivery d=new MilkDelivery(); d.setFarm(s.getFarm()); d.setSubscription(s); d.setDeliveryDate(date);
            RateSelection selectedRate=selectRate(s.getCustomer(),date);
            d.setScheduledQuantity(s.getDailyQuantity()); d.setUnitPrice(selectedRate.rate());
            d.setTotalAmount(ZERO);
            d.setCustomerMilkRate(selectedRate.customerRate()); deliveries.save(d); created++;
        }
        return created;
    }
    @Scheduled(cron="0 5 0 * * *", zone="${app.farm.time-zone}")
    @Transactional
    public void generateTodayDeliveries() {
        LocalDate date=today();
        for(Farm farm:farms.findAll()) generateForFarm(farm.getUuid(),date);
    }
    @Transactional(readOnly = true)
    public Page<PhaseThreeDtos.DeliveryResponse> deliveries(LocalDate from,LocalDate to,int page,int size) {
        range(from,to); return deliveries.findAllByFarm_UuidAndDeliveryDateBetween(farmUuid(),from,to,page(page,size))
                .map(MilkManagementService::deliveryResponse);
    }
    @Transactional
    public PhaseThreeDtos.DeliveryResponse updateDelivery(UUID uuid,PhaseThreeDtos.DeliveryUpdate request) {
        Farm farm=lockFarm();
        MilkDelivery d=deliveries.findByUuidAndFarm_Uuid(uuid,farm.getUuid()).orElseThrow(()->new NotFoundException("Delivery not found."));
        if(d.getStatus()!=MilkDeliveryStatus.PENDING && d.getStatus()!=MilkDeliveryStatus.PARTIAL)
            throw new ConflictException("Only pending or partially delivered orders can be updated.");
        if(request.deliveredQuantity().compareTo(d.getScheduledQuantity())>0) throw new IllegalArgumentException("Delivered quantity cannot exceed the scheduled amount.");
        if(request.status()==MilkDeliveryStatus.PARTIAL && (request.deliveredQuantity().signum()==0
                || request.deliveredQuantity().compareTo(d.getScheduledQuantity())>=0))
            throw new IllegalArgumentException("A partial delivery must be greater than zero and less than scheduled quantity.");
        if(request.status()==MilkDeliveryStatus.DELIVERED && request.deliveredQuantity().compareTo(d.getScheduledQuantity())!=0)
            throw new IllegalArgumentException("Delivered quantity must match the scheduled quantity.");
        if(request.status()==MilkDeliveryStatus.CANCELLED && (d.getStatus()!=MilkDeliveryStatus.PENDING || request.deliveredQuantity().signum()!=0))
            throw new IllegalArgumentException("Only a pending, undelivered order can be cancelled.");
        BigDecimal qty=money(request.deliveredQuantity());
        BigDecimal delta=qty.subtract(d.getDeliveredQuantity());
        if(delta.signum()>0 && stockTransactions.stock(farm.getUuid()).compareTo(delta)<0) throw new ConflictException("Insufficient milk stock for delivery.");
        d.setStatus(request.status()); d.setDeliveredQuantity(qty); d.setNotes(request.notes());
        d.setTotalAmount(money(qty.multiply(d.getUnitPrice())));
        if(delta.signum()>0) stock(farm,MilkStockTransactionType.USED,delta.negate(),null,null,d,"Subscription delivery");
        if(delta.signum()<0) stock(farm,MilkStockTransactionType.CORRECTION,delta.negate(),null,null,d,"Delivery quantity correction");
        if(qty.signum()>0) d.setDeliveredBy(currentUser.get());
        return deliveryResponse(deliveries.save(d));
    }

    @Transactional(readOnly = true)
    public PhaseThreeDtos.DashboardResponse dashboard() {
        UUID farm=farmUuid(); LocalDate date=today(); BigDecimal morning=zero(production.sumByDateAndSession(farm,date,MilkingSession.MORNING));
        BigDecimal afternoon=zero(production.sumByDateAndSession(farm,date,MilkingSession.AFTERNOON));
        BigDecimal evening=zero(production.sumByDateAndSession(farm,date,MilkingSession.EVENING));
        List<PhaseThreeDtos.DailyProduction> productionTrend=new ArrayList<>();
        List<PhaseThreeDtos.DailySales> salesTrend=new ArrayList<>();
        for(int i=6;i>=0;i--) {
            LocalDate d=date.minusDays(i);
            List<MilkProduction> records=production.report(farm,d,d);
            BigDecimal m=records.stream().filter(p->p.getSession()==MilkingSession.MORNING).map(MilkProduction::getQuantity).reduce(ZERO,BigDecimal::add);
            BigDecimal a=records.stream().filter(p->p.getSession()==MilkingSession.AFTERNOON).map(MilkProduction::getQuantity).reduce(ZERO,BigDecimal::add);
            BigDecimal e=records.stream().filter(p->p.getSession()==MilkingSession.EVENING).map(MilkProduction::getQuantity).reduce(ZERO,BigDecimal::add);
            productionTrend.add(new PhaseThreeDtos.DailyProduction(d,m,a,e,m.add(a).add(e)));
            salesTrend.add(new PhaseThreeDtos.DailySales(d,zero(sales.soldQuantity(farm,d)),zero(sales.revenue(farm,d,d))));
        }

        BigDecimal todaySales=sales.soldQuantity(farm,date);
        return new PhaseThreeDtos.DashboardResponse(date,morning.add(afternoon).add(evening),morning,afternoon,evening,
                zero(stockTransactions.stock(farm)),todaySales,zero(sales.revenue(farm,date,date)),
                customers.countByFarm_UuidAndStatus(farm,CustomerStatus.ACTIVE),
                deliveries.countByFarm_UuidAndDeliveryDateAndStatus(farm,date,MilkDeliveryStatus.PENDING),
                zero(payments.farmOutstanding(farm)),productionTrend,salesTrend);
    }

    @Transactional(readOnly = true)
    public List<PhaseThreeDtos.DailyProduction> dailyProduction(LocalDate from, LocalDate to) {
        range(from,to);
        java.util.Map<LocalDate,java.util.EnumMap<MilkingSession,BigDecimal>> totals=new java.util.TreeMap<>();
        for(MilkProduction p:production.findAllByFarm_UuidAndProductionDateBetweenOrderByProductionDateAscSessionAsc(farmUuid(),from,to)) {
            java.util.EnumMap<MilkingSession,BigDecimal> day=totals.computeIfAbsent(p.getProductionDate(),d->new java.util.EnumMap<>(MilkingSession.class));
            day.merge(p.getSession(),p.getQuantity(),BigDecimal::add);
        }
        return totals.entrySet().stream().map(entry->{
            BigDecimal morning=entry.getValue().getOrDefault(MilkingSession.MORNING,ZERO);
            BigDecimal afternoon=entry.getValue().getOrDefault(MilkingSession.AFTERNOON,ZERO);
            BigDecimal evening=entry.getValue().getOrDefault(MilkingSession.EVENING,ZERO);
            return new PhaseThreeDtos.DailyProduction(entry.getKey(),morning,afternoon,evening,morning.add(afternoon).add(evening));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PhaseThreeDtos.ReportRow> report(String type,LocalDate from,LocalDate to) {
        range(from,to); UUID farm=farmUuid(); List<PhaseThreeDtos.ReportRow> rows=new ArrayList<>();
        switch(type) {
            case "production" -> production.report(farm,from,to).forEach(p->rows.add(new PhaseThreeDtos.ReportRow(p.getProductionDate(),
                    "PRODUCTION",p.getAnimal().getTagNumber()+" "+p.getSession(),p.getQuantity(),null,p.getNotes())));
            case "collections" -> collections.findAllByFarm_UuidAndCollectionDateBetween(farm,from,to)
                    .forEach(c->rows.add(new PhaseThreeDtos.ReportRow(c.getCollectionDate(),"COLLECTION",c.getSession().toString(),
                            c.getAcceptedQuantity(),null,c.getRejectionReason())));
            case "sales" -> sales.findAllByFarm_UuidAndSaleDateBetweenOrderBySaleDateAsc(farm,from,to)
                    .forEach(s->rows.add(new PhaseThreeDtos.ReportRow(s.getSaleDate(),"SALE",s.getInvoiceNumber(),s.getQuantity(),s.getTotalAmount(),s.getCustomer().getName())));
            case "payments" -> payments.findAllByFarm_UuidAndPaymentDateBetween(farm,from,to)
                    .forEach(p->rows.add(new PhaseThreeDtos.ReportRow(p.getPaymentDate(),"PAYMENT",p.getCustomer().getName(),null,p.getAmount(),p.getTransactionReference())));
            case "stock" -> stockTransactions.byCreatedRange(farm,
                    from.atStartOfDay(clock.getZone()).toInstant(),to.plusDays(1).atStartOfDay(clock.getZone()).toInstant(),PageRequest.of(0,10000)).getContent()
                    .forEach(t->rows.add(new PhaseThreeDtos.ReportRow(t.getCreatedAt().atZone(clock.getZone()).toLocalDate(),
                            "STOCK",t.getType().toString(),t.getQuantityChange(),null,t.getReferenceNote())));
            case "wastage" -> {
                collections.findAllByFarm_UuidAndCollectionDateBetweenOrderByCollectionDateAsc(farm,from,to).stream()
                        .filter(c->c.getRejectedQuantity().signum()>0)
                        .forEach(c->rows.add(new PhaseThreeDtos.ReportRow(c.getCollectionDate(),"REJECTED_MILK",c.getSession().toString(),
                                c.getRejectedQuantity(),null,c.getRejectionReason())));
                stockTransactions.byTypeAndCreatedRange(farm,MilkStockTransactionType.WASTE,
                                from.atStartOfDay(clock.getZone()).toInstant(),to.plusDays(1).atStartOfDay(clock.getZone()).toInstant())
                        .forEach(t->rows.add(new PhaseThreeDtos.ReportRow(t.getCreatedAt().atZone(clock.getZone()).toLocalDate(),
                                "WASTE",t.getReferenceNote(),t.getQuantityChange().abs(),null,t.getReferenceNote())));
            }
            case "balances" -> customers.findAllByFarm_Uuid(farm)
                    .forEach(c->rows.add(new PhaseThreeDtos.ReportRow(today(),"OUTSTANDING",c.getName(),null,
                            zero(payments.outstanding(c.getUuid())),c.getMobileNumber())));
            default -> throw new IllegalArgumentException("Report type must be production, collections, sales, payments, stock, wastage, or balances.");
        }
        return rows;
    }

    private CustomerPayment recordPayment(Farm farm,Customer customer,MilkSale sale,MilkBill bill,LocalDate date,BigDecimal amount,
                                           PaymentMethod method,String reference,String notes) {
        CustomerPayment payment=new CustomerPayment(); payment.setFarm(farm); payment.setCustomer(customer);
        payment.setSale(sale); payment.setBill(bill);
        payment.setPaymentDate(date); payment.setAmount(amount); payment.setMethod(method);
        payment.setReceivedBy(currentUser.get()); payment.setNotes(notes);
        payment.setTransactionReference(reference==null||reference.isBlank()?null:reference.trim());
        return payments.save(payment);
    }
    private void stock(Farm farm,MilkStockTransactionType type,BigDecimal change,MilkCollection collection,MilkSale sale,
                       MilkDelivery delivery,String note) {
        stock(farm, type, change, collection, sale, delivery, note, null);
    }
    private void stock(Farm farm,MilkStockTransactionType type,BigDecimal change,MilkCollection collection,MilkSale sale,
                       MilkDelivery delivery,String note,MilkProduction production) {
        milkStock.record(farm, type, change, collection, production, sale, delivery, null, note);
    }
    private Animal animal(UUID uuid) { return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid,farmUuid())
            .orElseThrow(()->new NotFoundException("Animal not found in this farm.")); }
    private void eligibleForMilk(Animal animal) {
        if(animal.getType()!=AnimalType.COW || animal.getGender()!=Gender.FEMALE || animal.getStatus()!=AnimalStatus.ACTIVE || !animal.isLactating())
            throw new IllegalArgumentException("Milk production requires an active female cow marked as lactating.");
    }
    private Customer customer(UUID uuid) { return customers.findByUuidAndFarm_Uuid(uuid,farmUuid())
            .orElseThrow(()->new NotFoundException("Customer not found in this farm.")); }
    private Farm farm() { return farms.findById(farmUuid()).orElseThrow(()->new NotFoundException("Farm not found.")); }
    private Farm lockFarm() { return farms.findLockedByUuid(farmUuid()).orElseThrow(()->new NotFoundException("Farm not found.")); }
    private RateSelection selectRate(Customer customer, LocalDate date) {
        List<CustomerMilkRate> matches = customerMilkRates
                .findAllByFarm_UuidAndCustomer_UuidAndStatusAndEffectiveFromLessThanEqual(
                        customer.getFarm().getUuid(), customer.getUuid(), CustomerMilkRateStatus.ACTIVE, date)
                .stream().filter(rate -> rate.getEffectiveUntil() == null || !rate.getEffectiveUntil().isBefore(date))
                .toList();
        if (matches.size() > 1)
            throw new ConflictException("Multiple active customer milk rates apply on " + date + ".");
        if (!matches.isEmpty()) return new RateSelection(matches.get(0).getRatePerLitre(), matches.get(0));
        return new RateSelection(zero(customer.getFarm().getDefaultMilkRate()), null);
    }
    private void validateRatePeriod(PhaseThreeDtos.CustomerMilkRateRequest request) {
        if (request.effectiveUntil() != null && request.effectiveUntil().isBefore(request.effectiveFrom()))
            throw new IllegalArgumentException("Rate effective-until date must be on or after effective-from date.");
    }
    private void validateRateOverlap(UUID farmUuid, UUID customerUuid, UUID exceptUuid,
            PhaseThreeDtos.CustomerMilkRateRequest request) {
        if (request.status() != CustomerMilkRateStatus.ACTIVE) return;
        boolean overlaps = customerMilkRates.findAllByFarm_UuidAndCustomer_UuidOrderByEffectiveFromDescCreatedAtDesc(
                        farmUuid, customerUuid).stream()
                .filter(existing -> existing.getStatus() == CustomerMilkRateStatus.ACTIVE)
                .filter(existing -> !existing.getUuid().equals(exceptUuid))
                .anyMatch(existing -> !startsAfter(request.effectiveFrom(), existing.getEffectiveUntil())
                        && !startsAfter(existing.getEffectiveFrom(), request.effectiveUntil()));
        if (overlaps) throw new ConflictException("Active milk rate periods for a customer cannot overlap.");
    }
    private static boolean startsAfter(LocalDate start, LocalDate end) {
        return end != null && start.isAfter(end);
    }
    private static void applyRate(CustomerMilkRate rate, PhaseThreeDtos.CustomerMilkRateRequest request) {
        rate.setRatePerLitre(money(request.ratePerLitre()));
        rate.setEffectiveFrom(request.effectiveFrom()); rate.setEffectiveUntil(request.effectiveUntil());
        rate.setStatus(request.status()); rate.setNotes(request.notes());
    }
    private UUID farmUuid() { return currentUser.farmUuid(); }
    private LocalDate today() { return LocalDate.now(clock); }
    private void range(LocalDate from,LocalDate to) { if(from==null||to==null||to.isBefore(from)) throw new IllegalArgumentException("Provide a valid date range."); }
    private Pageable page(int p,int s) { return PageRequest.of(Math.max(0,p),Math.clamp(s,1,100),Sort.by(Sort.Direction.DESC,"createdAt")); }
    private static BigDecimal money(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP); }
    private static BigDecimal zero(BigDecimal value) { return value==null?ZERO:value; }
    private record RateSelection(BigDecimal rate, CustomerMilkRate customerRate) {}
    private static void applyCustomer(Customer c,PhaseThreeDtos.CustomerRequest r) {
        c.setName(r.name().trim()); c.setMobileNumber(r.mobileNumber()); c.setAddress(r.address()); c.setType(r.type());
        c.setStartDate(r.startDate()); c.setStatus(r.status()); c.setNotes(r.notes()); c.setBillingFrequency(r.billingFrequency());
    }
    private static PhaseThreeDtos.ProductionResponse productionResponse(MilkProduction p) {
        return new PhaseThreeDtos.ProductionResponse(p.getUuid(),p.getAnimal().getUuid(),p.getAnimal().getTagNumber(),p.getAnimal().getName(),
                p.getProductionDate(),p.getSession(),p.getQuantity(),p.getRecordedBy().getFullName(),p.getNotes(),p.getCreatedAt(),p.getUpdatedAt());
    }
    private static PhaseThreeDtos.CollectionResponse collectionResponse(MilkCollection c) {
        return new PhaseThreeDtos.CollectionResponse(c.getUuid(),c.getCollectionDate(),c.getSession(),c.getTotalQuantity(),
                c.getAcceptedQuantity(),c.getRejectedQuantity(),c.getRejectionReason(),c.getQualityStatus(),c.getStorageLocation(),
                c.getRecordedBy().getFullName(),c.getCreatedAt());
    }
    private PhaseThreeDtos.CustomerResponse customerResponse(Customer c) {
        CustomerMilkRate current = currentCustomerRate(c, today());
        return new PhaseThreeDtos.CustomerResponse(c.getUuid(),c.getName(),c.getMobileNumber(),c.getAddress(),c.getType(),
                c.getStartDate(),c.getStatus(),zero(payments.outstanding(c.getUuid())),c.getNotes(),c.getCreatedAt(),c.getUpdatedAt(),
                current == null ? null : current.getRatePerLitre(), current == null ? null : current.getEffectiveFrom(),
                current == null ? null : current.getEffectiveUntil(), c.getBillingFrequency());
    }
    private PhaseThreeDtos.MilkBillResponse milkBillResponse(MilkBill bill) {
        BigDecimal paid = zero(payments.paidForBill(bill.getUuid()));
        BigDecimal amountDue = bill.getTotalAmount().add(bill.getAdjustmentAmount());
        PaymentStatus status = paid.compareTo(amountDue) >= 0 ? PaymentStatus.PAID
                : paid.signum() > 0 ? PaymentStatus.PARTIAL : PaymentStatus.UNPAID;
        List<PhaseThreeDtos.MilkBillItemResponse> items = billItems
                .findAllByBill_UuidOrderByDelivery_DeliveryDateAsc(bill.getUuid()).stream()
                .map(item -> new PhaseThreeDtos.MilkBillItemResponse(item.getUuid(), item.getDelivery().getUuid(),
                        item.getDelivery().getDeliveryDate(), item.getDelivery().getSubscription().getUuid(),
                        item.getDelivery().getSubscription().getSession(),
                        item.getQuantity(), item.getUnitPrice(), item.getAmount()))
                .toList();
        List<PhaseThreeDtos.BillAdjustmentResponse> adjustments = billAdjustments
                .findAllByBill_UuidOrderByCreatedAtAsc(bill.getUuid()).stream()
                .map(MilkManagementService::billAdjustmentResponse).toList();
        return new PhaseThreeDtos.MilkBillResponse(bill.getUuid(), bill.getInvoiceNumber(), bill.getCustomer().getUuid(),
                bill.getCustomer().getName(), bill.getBillingFrequency(), bill.getPeriodStart(), bill.getPeriodEnd(),
                bill.getTotalQuantity(), bill.getTotalAmount(), bill.getPreviousOutstanding(), bill.getAdjustmentAmount(),
                amountDue, paid, amountDue.subtract(paid).max(ZERO), status, bill.getCustomer().getMobileNumber(),
                bill.getGeneratedBy().getFullName(), bill.getCreatedAt(), items, adjustments);
    }
    private static PhaseThreeDtos.BillAdjustmentResponse billAdjustmentResponse(MilkBillAdjustment row) {
        return new PhaseThreeDtos.BillAdjustmentResponse(row.getUuid(), row.getAmount(), row.getReason(),
                row.getAdjustedBy().getFullName(), row.getCreatedAt());
    }
    private CustomerMilkRate currentCustomerRate(Customer customer, LocalDate date) {
        List<CustomerMilkRate> matches = customerMilkRates
                .findAllByFarm_UuidAndCustomer_UuidAndStatusAndEffectiveFromLessThanEqual(
                        customer.getFarm().getUuid(), customer.getUuid(), CustomerMilkRateStatus.ACTIVE, date)
                .stream().filter(rate -> rate.getEffectiveUntil() == null || !rate.getEffectiveUntil().isBefore(date))
                .toList();
        if (matches.size() > 1) throw new ConflictException("Multiple active customer milk rates apply on " + date + ".");
        return matches.isEmpty() ? null : matches.get(0);
    }
    private static PhaseThreeDtos.CustomerMilkRateResponse customerMilkRateResponse(CustomerMilkRate rate) {
        return new PhaseThreeDtos.CustomerMilkRateResponse(rate.getUuid(), rate.getCustomer().getUuid(),
                rate.getCustomer().getName(), rate.getRatePerLitre(), rate.getEffectiveFrom(), rate.getEffectiveUntil(),
                rate.getStatus(), rate.getNotes(), rate.getCreatedBy().getFullName(), rate.getCreatedAt(), rate.getUpdatedAt());
    }
    private PhaseThreeDtos.SaleResponse saleResponse(MilkSale s) {
        BigDecimal paid=zero(payments.paidForSale(s.getUuid()));
        return new PhaseThreeDtos.SaleResponse(s.getUuid(),s.getInvoiceNumber(),s.getCustomer().getUuid(),s.getCustomer().getName(),
                s.getSaleDate(),s.getSaleTime(),s.getQuantity(),s.getUnitPrice(),s.getDiscount(),s.getTotalAmount(),paid,
                s.getTotalAmount().subtract(paid).max(ZERO),s.getPaymentStatus(),s.getPaymentMethod(),s.getNotes(),s.getCreatedAt(),
                s.getOriginalUnitPrice(),s.getRateOverrideReason(),
                s.getRateOverrideBy() == null ? null : s.getRateOverrideBy().getFullName());
    }
    private static PhaseThreeDtos.PaymentResponse paymentResponse(CustomerPayment p) {
        return new PhaseThreeDtos.PaymentResponse(p.getUuid(),p.getCustomer().getUuid(),p.getCustomer().getName(),
                p.getSale()!=null?p.getSale().getUuid():null,
                p.getSale()!=null?p.getSale().getInvoiceNumber():p.getBill()!=null?p.getBill().getInvoiceNumber():null,
                p.getPaymentDate(),p.getAmount(),p.getMethod(),p.getTransactionReference(),p.getReceivedBy().getFullName(),
                p.getCreatedAt(),p.getBill()==null?null:p.getBill().getUuid());
    }
    private static PhaseThreeDtos.SubscriptionResponse subscriptionResponse(MilkSubscription s) {
        return new PhaseThreeDtos.SubscriptionResponse(s.getUuid(),s.getCustomer().getUuid(),s.getCustomer().getName(),
                s.getDailyQuantity(),s.getSession(),s.getStartDate(),s.getEndDate(),s.getUnitPrice(),s.getStatus(),s.getCreatedAt());
    }
    private static PhaseThreeDtos.DeliveryResponse deliveryResponse(MilkDelivery d) {
        return new PhaseThreeDtos.DeliveryResponse(d.getUuid(),d.getSubscription().getUuid(),d.getSubscription().getCustomer().getUuid(),
                d.getSubscription().getCustomer().getName(),d.getDeliveryDate(),d.getScheduledQuantity(),d.getDeliveredQuantity(),
                d.getStatus(),d.getNotes(),d.getUnitPrice(),d.getTotalAmount());
    }
}
