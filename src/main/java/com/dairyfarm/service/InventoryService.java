package com.dairyfarm.service;

import com.dairyfarm.dto.InventoryDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private static final BigDecimal ZERO = new BigDecimal("0.000");
    private static final BigDecimal MONEY_ZERO = new BigDecimal("0.00");
    private final SupplierRepository suppliers;
    private final InventoryItemRepository items;
    private final FeedPurchaseRepository purchases;
    private final SupplierPaymentRepository supplierPayments;
    private final InventoryTransactionRepository transactions;
    private final FeedConsumptionRepository consumptions;
    private final FeedConsumptionAuditRepository consumptionAudits;
    private final FeedRationRepository rations;
    private final FarmRepository farms;
    private final AnimalRepository animals;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public InventoryService(SupplierRepository suppliers, InventoryItemRepository items, FeedPurchaseRepository purchases,
            SupplierPaymentRepository supplierPayments, InventoryTransactionRepository transactions,
            FeedConsumptionRepository consumptions, FeedConsumptionAuditRepository consumptionAudits,
            FeedRationRepository rations, FarmRepository farms, AnimalRepository animals,
            CurrentUserService currentUser, Clock clock) {
        this.suppliers=suppliers; this.items=items; this.purchases=purchases; this.supplierPayments=supplierPayments;
        this.transactions=transactions; this.consumptions=consumptions; this.consumptionAudits=consumptionAudits;
        this.rations=rations; this.farms=farms; this.animals=animals; this.currentUser=currentUser; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public List<InventoryDtos.SupplierResponse> suppliers() {
        return suppliers.findAllByFarm_UuidOrderByNameAsc(farmUuid()).stream().map(this::supplierResponse).toList();
    }
    @Transactional
    public InventoryDtos.SupplierResponse createSupplier(InventoryDtos.SupplierRequest request) {
        requireManager(); Farm farm=lockedFarm();
        if(suppliers.existsByFarm_UuidAndNameIgnoreCase(farm.getUuid(),request.name().trim()))
            throw new ConflictException("A supplier with this name already exists in this farm.");
        Supplier supplier=new Supplier(); supplier.setFarm(farm); applySupplier(supplier,request);
        return supplierResponse(suppliers.save(supplier));
    }
    @Transactional
    public InventoryDtos.SupplierResponse updateSupplier(UUID uuid,InventoryDtos.SupplierRequest request) {
        requireManager(); Farm farm=lockedFarm(); Supplier supplier=supplier(uuid);
        if(suppliers.existsByFarm_UuidAndNameIgnoreCaseAndUuidNot(farm.getUuid(),request.name().trim(),uuid))
            throw new ConflictException("A supplier with this name already exists in this farm.");
        applySupplier(supplier,request); return supplierResponse(suppliers.save(supplier));
    }

    @Transactional(readOnly=true)
    public List<InventoryDtos.ItemResponse> items(InventoryItemType type) {
        UUID farm=farmUuid();
        List<InventoryItem> rows=type==null?items.findAllByFarm_UuidOrderByNameAsc(farm):
                items.findAllByFarm_UuidAndTypeOrderByNameAsc(farm,type);
        return rows.stream().map(this::itemResponse).toList();
    }
    @Transactional
    public InventoryDtos.ItemResponse createItem(InventoryDtos.ItemRequest request) {
        requireManager(); Farm farm=lockedFarm(); validateItemSupplier(request);
        if(itemExists(farm.getUuid(),request.name(),request.batchNumber()))
            throw new ConflictException("An inventory item with this name and batch already exists.");
        InventoryItem item=new InventoryItem(); item.setFarm(farm); applyItem(item,request);
        item.setCurrentStock(ZERO);
        return itemResponse(items.save(item));
    }
    @Transactional
    public InventoryDtos.ItemResponse updateItem(UUID uuid,InventoryDtos.ItemRequest request) {
        requireManager(); lockedFarm(); InventoryItem item=item(uuid);
        validateItemSupplier(request);
        if(itemExistsExcept(item.getFarm().getUuid(),request.name(),request.batchNumber(),uuid))
            throw new ConflictException("An inventory item with this name and batch already exists.");
        if(item.getType()!=request.type()&&item.getCurrentStock().signum()>0)
            throw new ConflictException("An item with stock cannot be changed between feed and medicine.");
        applyItem(item,request);
        return itemResponse(items.save(item));
    }
    @Transactional(readOnly=true)
    public InventoryItem requireMedicineItem(UUID uuid) {
        InventoryItem item=item(uuid);
        if(item.getType()!=InventoryItemType.MEDICINE||item.getStatus()!=InventoryItemStatus.ACTIVE)
            throw new IllegalArgumentException("Select an active medicine inventory item.");
        return item;
    }

    @Transactional
    public InventoryDtos.PurchaseResponse purchase(InventoryDtos.PurchaseRequest request) {
        requireManagerOrAccountant(); Farm farm=lockedFarm(); Supplier supplier=supplier(request.supplierUuid());
        InventoryItem item=item(request.itemUuid());
        if(supplier.getStatus()!=SupplierStatus.ACTIVE||item.getStatus()!=InventoryItemStatus.ACTIVE)
            throw new ConflictException("Purchases require an active supplier and inventory item.");
        if(request.purchaseDate().isAfter(today())) throw new IllegalArgumentException("Purchase date cannot be in the future.");
        String invoice=request.invoiceNumber().trim();
        if(purchases.existsByFarm_UuidAndInvoiceNumberAndItem_Uuid(farm.getUuid(),invoice,item.getUuid()))
            throw new ConflictException("This invoice already contains this item.");
        BigDecimal qty=quantity(request.quantity()),unitPrice=money(request.unitPrice());
        BigDecimal total=money(qty.multiply(unitPrice)),paid=money(request.initialPayment());
        if(paid.compareTo(total)>0) throw new IllegalArgumentException("Initial payment cannot exceed the purchase total.");
        FeedPurchase purchase=new FeedPurchase(); purchase.setFarm(farm); purchase.setSupplier(supplier); purchase.setItem(item);
        purchase.setPurchaseDate(request.purchaseDate()); purchase.setQuantity(qty); purchase.setUnitPrice(unitPrice);
        purchase.setTotalAmount(total); purchase.setInvoiceNumber(invoice); purchase.setAmountPaid(paid);
        purchase.setPaymentStatus(paymentStatus(total,paid)); purchase.setPaymentMethod(clean(request.paymentMethod()));
        purchase.setNotes(clean(request.notes())); purchase.setCreatedBy(currentUser.get());
        purchase=purchases.save(purchase);
        item.setCurrentStock(quantity(item.getCurrentStock().add(qty)));
        item.setPurchasePrice(unitPrice); if(item.getSupplier()==null) item.setSupplier(supplier);
        items.save(item);
        recordTransaction(farm,item,InventoryTransactionType.PURCHASE,qty,unitPrice,request.purchaseDate(),
                purchase.getUuid(),null,"Purchase invoice "+invoice);
        if(paid.signum()>0) recordSupplierPayment(farm,purchase,request.purchaseDate(),paid,
                request.paymentMethod()==null?"CASH":request.paymentMethod(),null);
        return purchaseResponse(purchase);
    }
    @Transactional
    public InventoryDtos.PurchaseResponse payPurchase(UUID uuid,InventoryDtos.SupplierPaymentRequest request) {
        requireManagerOrAccountant(); Farm farm=lockedFarm(); FeedPurchase purchase=purchase(uuid);
        BigDecimal amount=money(request.amount());
        if(purchase.getAmountPaid().add(amount).compareTo(purchase.getTotalAmount())>0)
            throw new ConflictException("Payment exceeds the purchase's outstanding balance.");
        if(request.paymentDate().isAfter(today())) throw new IllegalArgumentException("Payment date cannot be in the future.");
        purchase.setAmountPaid(money(purchase.getAmountPaid().add(amount)));
        purchase.setPaymentStatus(paymentStatus(purchase.getTotalAmount(),purchase.getAmountPaid()));
        purchases.save(purchase);
        recordSupplierPayment(farm,purchase,request.paymentDate(),amount,request.paymentMethod().name(),
                clean(request.referenceNumber()));
        return purchaseResponse(purchase);
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.PurchaseResponse> purchases(LocalDate from,LocalDate to) {
        range(from,to);
        return purchases.findAllByFarm_UuidAndPurchaseDateBetweenOrderByPurchaseDateAsc(farmUuid(),from,to)
                .stream().map(this::purchaseResponse).toList();
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.SupplierPaymentResponse> supplierPayments(UUID purchaseUuid) {
        purchase(purchaseUuid);
        return supplierPayments.findAllByFarm_UuidAndPurchase_UuidOrderByPaymentDateAscCreatedAtAsc(farmUuid(),purchaseUuid)
                .stream().map(p->new InventoryDtos.SupplierPaymentResponse(p.getUuid(),p.getPaymentDate(),p.getAmount(),
                        p.getPaymentMethod(),p.getReferenceNumber(),p.getRecordedBy().getFullName(),p.getCreatedAt())).toList();
    }

    @Transactional
    public InventoryDtos.ConsumptionResponse consume(InventoryDtos.ConsumptionRequest request) {
        Farm farm=lockedFarm(); validateTarget(request.animalUuid(),request.targetGroup());
        InventoryItem item=item(request.itemUuid());
        if(item.getType()!=InventoryItemType.FEED||item.getStatus()!=InventoryItemStatus.ACTIVE)
            throw new IllegalArgumentException("Daily feed consumption requires an active feed item.");
        if(request.date().isAfter(today())) throw new IllegalArgumentException("Consumption date cannot be in the future.");
        BigDecimal qty=quantity(request.quantity()); requireStock(item,qty);
        FeedConsumption row=new FeedConsumption(); row.setFarm(farm); row.setItem(item);
        row.setAnimal(request.animalUuid()==null?null:animal(request.animalUuid()));
        row.setTargetGroup(request.targetGroup()); row.setDate(request.date()); row.setQuantity(qty);
        row.setUnitCost(money(item.getPurchasePrice())); row.setTotalCost(money(qty.multiply(item.getPurchasePrice())));
        row.setNotes(clean(request.notes())); row.setRecordedBy(currentUser.get()); row=consumptions.save(row);
        item.setCurrentStock(quantity(item.getCurrentStock().subtract(qty))); items.save(item);
        recordTransaction(farm,item,InventoryTransactionType.CONSUMPTION,qty.negate(),row.getUnitCost(),
                row.getDate(),row.getUuid(),row.getAnimal(),"Feed consumption");
        return consumptionResponse(row);
    }
    @Transactional
    public InventoryDtos.ConsumptionResponse correctConsumption(UUID uuid,InventoryDtos.ConsumptionCorrectionRequest request) {
        requireManager(); Farm farm=lockedFarm(); FeedConsumption row=consumption(uuid); InventoryItem item=row.getItem();
        BigDecimal previous=row.getQuantity(),next=quantity(request.quantity()),delta=next.subtract(previous);
        if(delta.signum()>0) requireStock(item,delta);
        item.setCurrentStock(quantity(item.getCurrentStock().subtract(delta))); items.save(item);
        FeedConsumptionAudit audit=new FeedConsumptionAudit(); audit.setFarm(farm); audit.setConsumption(row);
        audit.setEditedBy(currentUser.get()); audit.setPreviousQuantity(previous); audit.setNewQuantity(next);
        audit.setReason(request.reason().trim()); consumptionAudits.save(audit);
        row.setQuantity(next); row.setUnitCost(money(row.getUnitCost()));
        row.setTotalCost(money(next.multiply(row.getUnitCost()))); row=consumptions.save(row);
        if(delta.signum()!=0) recordTransaction(farm,item,delta.signum()>0?InventoryTransactionType.CORRECTION_OUT:
                InventoryTransactionType.CORRECTION_IN,delta.negate(),row.getUnitCost(),today(),row.getUuid(),
                row.getAnimal(),request.reason().trim());
        return consumptionResponse(row);
    }
    @Transactional(readOnly=true)
    public Page<InventoryDtos.ConsumptionResponse> consumptions(LocalDate from,LocalDate to,int page,int size) {
        range(from,to);
        return consumptions.findAllByFarm_UuidAndDateBetween(farmUuid(),from,to,
                PageRequest.of(Math.max(0,page),Math.clamp(size,1,100),Sort.by(Sort.Direction.DESC,"date")))
                .map(this::consumptionResponse);
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.ConsumptionAuditResponse> consumptionAudit(UUID uuid) {
        consumption(uuid);
        return consumptionAudits.findAllByFarm_UuidAndConsumption_UuidOrderByCreatedAtDesc(farmUuid(),uuid).stream()
                .map(a->new InventoryDtos.ConsumptionAuditResponse(a.getUuid(),a.getPreviousQuantity(),a.getNewQuantity(),
                        a.getReason(),a.getEditedBy().getFullName(),a.getCreatedAt())).toList();
    }

    @Transactional
    public InventoryDtos.RationResponse createRation(InventoryDtos.RationRequest request) {
        requireManager(); Farm farm=lockedFarm(); validateTarget(request.animalUuid(),request.targetGroup());
        validPeriod(request.effectiveFrom(),request.effectiveUntil());
        FeedRation ration=new FeedRation(); ration.setFarm(farm); ration.setName(request.name().trim());
        ration.setTargetGroup(request.targetGroup()); ration.setAnimal(request.animalUuid()==null?null:animal(request.animalUuid()));
        ration.setEffectiveFrom(request.effectiveFrom()); ration.setEffectiveUntil(request.effectiveUntil());
        List<FeedRationLine> lines=new ArrayList<>();
        for(InventoryDtos.RationLineRequest lineRequest:request.lines()) {
            InventoryItem item=item(lineRequest.itemUuid());
            if(item.getType()!=InventoryItemType.FEED||item.getStatus()!=InventoryItemStatus.ACTIVE)
                throw new IllegalArgumentException("Rations may include active feed items only.");
            if(lines.stream().anyMatch(line->line.getItem().getUuid().equals(item.getUuid())))
                throw new ConflictException("A feed item can appear only once in a ration.");
            FeedRationLine line=new FeedRationLine(); line.setRation(ration); line.setItem(item);
            line.setQuantityPerAnimalDay(quantity(lineRequest.quantityPerAnimalDay())); lines.add(line);
        }
        ration.setLines(lines); return rationResponse(rations.save(ration),today());
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.RationResponse> rations() {
        LocalDate date=today();
        return rations.findAllByFarm_UuidOrderByNameAsc(farmUuid()).stream().map(row->rationResponse(row,date)).toList();
    }

    @Transactional
    public InventoryDtos.ItemResponse adjustStock(UUID uuid,InventoryDtos.StockAdjustmentRequest request) {
        requireManager(); Farm farm=lockedFarm(); InventoryItem item=item(uuid);
        InventoryTransactionType type=request.type();
        if(!List.of(InventoryTransactionType.CORRECTION_IN,InventoryTransactionType.CORRECTION_OUT,
                InventoryTransactionType.WASTAGE,InventoryTransactionType.DISCREPANCY).contains(type))
            throw new IllegalArgumentException("Use CORRECTION_IN, CORRECTION_OUT, WASTAGE, or DISCREPANCY for a manual adjustment.");
        BigDecimal qty=quantity(request.quantity());
        boolean increase=type==InventoryTransactionType.CORRECTION_IN;
        BigDecimal change=increase?qty:qty.negate();
        if(!increase) requireStock(item,qty);
        item.setCurrentStock(quantity(item.getCurrentStock().add(change))); items.save(item);
        recordTransaction(farm,item,type,change,money(item.getPurchasePrice()),today(),null,null,request.reason().trim());
        return itemResponse(item);
    }
    @Transactional(readOnly=true)
    public Page<InventoryDtos.InventoryTransactionResponse> transactions(int page,int size) {
        UUID farm=farmUuid();
        return transactions.findAllByFarm_Uuid(farm,PageRequest.of(Math.max(0,page),Math.clamp(size,1,100),
                Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::transactionResponse);
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.AlertResponse> alerts() {
        LocalDate date=today(),expiryLimit=date.plusDays(30); List<InventoryDtos.AlertResponse> result=new ArrayList<>();
        for(InventoryItem item:items.findAllByFarm_UuidOrderByNameAsc(farmUuid())) {
            if(item.getCurrentStock().compareTo(item.getMinimumStock())<=0)
                result.add(alert(item,"LOW_STOCK"));
            if(item.getExpiryDate()!=null&&item.getExpiryDate().isBefore(date)&&item.getCurrentStock().signum()>0)
                result.add(alert(item,"EXPIRED"));
            else if(item.getExpiryDate()!=null&&!item.getExpiryDate().isAfter(expiryLimit)
                    &&item.getCurrentStock().signum()>0)
                result.add(alert(item,"EXPIRING"));
        }
        for(FeedPurchase purchase:purchases.findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(farmUuid()))
            if(purchase.getAmountPaid().compareTo(purchase.getTotalAmount())<0)
                result.add(alert(purchase.getItem(),"PENDING_PURCHASE"));
        return result;
    }
    @Transactional(readOnly=true)
    public InventoryDtos.SummaryResponse summary() {
        UUID farm=farmUuid(); BigDecimal feed=MONEY_ZERO,medicine=MONEY_ZERO,outstanding=MONEY_ZERO;
        for(InventoryItem item:items.findAllByFarm_UuidOrderByNameAsc(farm)) {
            BigDecimal value=money(item.getCurrentStock().multiply(item.getPurchasePrice()));
            if(item.getType()==InventoryItemType.FEED) feed=feed.add(value); else medicine=medicine.add(value);
        }
        for(FeedPurchase purchase:purchases.findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(farm))
            outstanding=outstanding.add(purchase.getTotalAmount().subtract(purchase.getAmountPaid()));
        List<InventoryDtos.AlertResponse> alerts=alerts();
        return new InventoryDtos.SummaryResponse(money(feed),money(medicine),
                suppliers.findAllByFarm_UuidOrderByNameAsc(farm).stream().filter(s->s.getStatus()==SupplierStatus.ACTIVE).count(),
                alerts.stream().filter(a->a.alertType().equals("LOW_STOCK")).count(),
                alerts.stream().filter(a->a.alertType().equals("EXPIRED")).count(),money(outstanding));
    }
    @Transactional(readOnly=true)
    public List<InventoryDtos.ReportRow> report(String type,LocalDate from,LocalDate to) {
        range(from,to); UUID farm=farmUuid(); List<InventoryDtos.ReportRow> result=new ArrayList<>();
        switch(type) {
            case "stock" -> items.findAllByFarm_UuidOrderByNameAsc(farm).forEach(item->result.add(
                    new InventoryDtos.ReportRow(today(),"STOCK",item.getName(),item.getCategory(),null,null,
                            item.getCurrentStock(),item.getUnit(),item.getPurchasePrice(),
                            money(item.getCurrentStock().multiply(item.getPurchasePrice())),item.getBatchNumber())));
            case "daily-consumption","animal-cost","group-cost" ->
                    consumptions.findAllByFarm_UuidAndDateBetween(farm,from,to).stream()
                            .filter(row->type.equals("daily-consumption")
                                    ||type.equals("animal-cost")&&row.getAnimal()!=null
                                    ||type.equals("group-cost")&&row.getTargetGroup()!=null)
                            .forEach(row->result.add(consumptionReportRow(row)));
            case "monthly-consumption" -> {
                Map<MonthlyConsumptionKey,MonthlyConsumptionTotal> totals=new LinkedHashMap<>();
                for(FeedConsumption row:consumptions.findAllByFarm_UuidAndDateBetween(farm,from,to)) {
                    MonthlyConsumptionKey key=new MonthlyConsumptionKey(YearMonth.from(row.getDate()),
                            row.getItem().getUuid(),row.getAnimal()==null?null:row.getAnimal().getUuid(),row.getTargetGroup());
                    MonthlyConsumptionTotal total=totals.computeIfAbsent(key,unused->new MonthlyConsumptionTotal());
                    if(total.exemplar==null) total.exemplar=row;
                    total.quantity=total.quantity.add(row.getQuantity());
                    total.cost=total.cost.add(row.getTotalCost());
                }
                totals.forEach((key,total)->{
                    FeedConsumption exemplar=total.exemplar;
                    BigDecimal averageCost=total.quantity.signum()==0?MONEY_ZERO:
                            total.cost.divide(total.quantity,2,RoundingMode.HALF_UP);
                    result.add(new InventoryDtos.ReportRow(key.month().atDay(1),"MONTHLY_CONSUMPTION",
                            exemplar.getItem().getName(),exemplar.getItem().getCategory(),
                            exemplar.getAnimal()==null?null:exemplar.getAnimal().getTagNumber(),
                            exemplar.getTargetGroup()==null?null:exemplar.getTargetGroup().name(),
                            total.quantity,exemplar.getItem().getUnit(),averageCost,total.cost,
                            "Month: "+key.month()));
                });
            }
            case "wastage","medicine-usage" ->
                    transactions.findAllByFarm_UuidAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(farm,from,to)
                            .stream().filter(tx->isReportTransaction(type,tx)).forEach(tx->result.add(
                                    new InventoryDtos.ReportRow(tx.getTransactionDate(),tx.getType().name(),tx.getItem().getName(),
                                            tx.getItem().getCategory(),tx.getAnimal()==null?null:tx.getAnimal().getTagNumber(),
                                            null,tx.getQuantityChange().abs(),tx.getItem().getUnit(),tx.getUnitCost(),
                                            money(tx.getQuantityChange().abs().multiply(tx.getUnitCost())),tx.getReason())));
            case "purchases" -> purchases.findAllByFarm_UuidAndPurchaseDateBetweenOrderByPurchaseDateAsc(farm,from,to)
                    .forEach(p->result.add(new InventoryDtos.ReportRow(p.getPurchaseDate(),"PURCHASE",p.getItem().getName(),
                            p.getItem().getCategory(),null,null,p.getQuantity(),p.getItem().getUnit(),p.getUnitPrice(),
                            p.getTotalAmount(),p.getSupplier().getName()+" / "+p.getInvoiceNumber()
                                    +" outstanding "+p.getTotalAmount().subtract(p.getAmountPaid()))));
            case "supplier-balances" -> {
                List<FeedPurchase> allPurchases=purchases.findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(farm);
                for(Supplier supplier:suppliers.findAllByFarm_UuidOrderByNameAsc(farm)) {
                    BigDecimal outstanding=MONEY_ZERO;
                    for(FeedPurchase purchase:allPurchases)
                        if(purchase.getSupplier().getUuid().equals(supplier.getUuid()))
                            outstanding=outstanding.add(purchase.getTotalAmount().subtract(purchase.getAmountPaid()));
                    result.add(new InventoryDtos.ReportRow(today(),"SUPPLIER_BALANCE",supplier.getName(),null,null,null,
                            null,null,null,money(outstanding),"Outstanding supplier balance"));
                }
            }
            default -> throw new IllegalArgumentException("Report type must be stock, daily-consumption, monthly-consumption, animal-cost, group-cost, medicine-usage, purchases, supplier-balances, or wastage.");
        }
        result.sort(Comparator.comparing(InventoryDtos.ReportRow::date)
                .thenComparing(InventoryDtos.ReportRow::item,Comparator.nullsFirst(String::compareTo)));
        return result;
    }

    private InventoryDtos.ReportRow consumptionReportRow(FeedConsumption row) {
        return new InventoryDtos.ReportRow(row.getDate(),"CONSUMPTION",row.getItem().getName(),row.getItem().getCategory(),
                row.getAnimal()==null?null:row.getAnimal().getTagNumber(),
                row.getTargetGroup()==null?null:row.getTargetGroup().name(),row.getQuantity(),row.getItem().getUnit(),
                row.getUnitCost(),row.getTotalCost(),row.getNotes());
    }

    private record MonthlyConsumptionKey(YearMonth month,UUID itemUuid,UUID animalUuid,RationTargetGroup targetGroup) {}
    private static final class MonthlyConsumptionTotal {
        private BigDecimal quantity=ZERO;
        private BigDecimal cost=MONEY_ZERO;
        private FeedConsumption exemplar;
    }

    @Transactional
    public void replaceMedicineUsage(String source,UUID reference,UUID previousItemUuid,BigDecimal previousQuantity,
            UUID nextItemUuid,BigDecimal nextQuantity,UUID animalUuid,LocalDate date) {
        if(previousItemUuid==null&&nextItemUuid==null) {
            if(previousQuantity!=null||nextQuantity!=null) throw new IllegalArgumentException("A medicine item is required for a medicine quantity.");
            return;
        }
        Farm farm=lockedFarm(); InventoryItem previous=previousItemUuid==null?null:item(previousItemUuid);
        InventoryItem next=nextItemUuid==null?null:item(nextItemUuid);
        BigDecimal oldQty=previousQuantity==null?ZERO:quantity(previousQuantity);
        BigDecimal newQty=nextQuantity==null?ZERO:quantity(nextQuantity);
        if(previous==null&&oldQty.signum()!=0||next==null&&newQty.signum()!=0)
            throw new IllegalArgumentException("Medicine quantities require an inventory item.");
        if(previous!=null&&previous.getType()!=InventoryItemType.MEDICINE)
            throw new IllegalArgumentException("The selected previous item is not medicine inventory.");
        if(next!=null&&(next.getType()!=InventoryItemType.MEDICINE||next.getStatus()!=InventoryItemStatus.ACTIVE))
            throw new IllegalArgumentException("Select an active medicine inventory item.");
        Animal animal=animalUuid==null?null:animal(animalUuid);
        if(previous!=null&&next!=null&&previous.getUuid().equals(next.getUuid())) {
            BigDecimal delta=newQty.subtract(oldQty);
            if(delta.signum()>0) requireStock(next,delta);
            if(delta.signum()!=0) {
                next.setCurrentStock(quantity(next.getCurrentStock().subtract(delta))); items.save(next);
                recordTransaction(farm,next,delta.signum()>0?InventoryTransactionType.CORRECTION_OUT:
                        InventoryTransactionType.CORRECTION_IN,delta.negate(),money(next.getPurchasePrice()),date,
                        reference,animal,source+" medicine usage update");
            }
            return;
        }
        if(previous!=null&&oldQty.signum()>0) {
            previous.setCurrentStock(quantity(previous.getCurrentStock().add(oldQty))); items.save(previous);
            recordTransaction(farm,previous,InventoryTransactionType.CONSUMPTION_REVERSAL,oldQty,
                    money(previous.getPurchasePrice()),date,reference,animal,source+" medicine usage replaced");
        }
        if(next!=null&&newQty.signum()>0) {
            requireStock(next,newQty); next.setCurrentStock(quantity(next.getCurrentStock().subtract(newQty))); items.save(next);
            recordTransaction(farm,next,source.equals("TREATMENT")?InventoryTransactionType.TREATMENT:
                    InventoryTransactionType.VACCINATION,newQty.negate(),money(next.getPurchasePrice()),date,
                    reference,animal,source+" medicine usage");
        }
    }

    private InventoryDtos.SupplierResponse supplierResponse(Supplier supplier) {
        BigDecimal total=MONEY_ZERO,paid=MONEY_ZERO;
        for(FeedPurchase purchase:purchases.findAllByFarm_UuidOrderByPurchaseDateDescCreatedAtDesc(supplier.getFarm().getUuid()))
            if(purchase.getSupplier().getUuid().equals(supplier.getUuid())) {
                total=total.add(purchase.getTotalAmount()); paid=paid.add(purchase.getAmountPaid());
            }
        return new InventoryDtos.SupplierResponse(supplier.getUuid(),supplier.getName(),supplier.getPhone(),
                supplier.getAddress(),supplier.getGstNumber(),supplier.getContactPerson(),supplier.getType(),
                supplier.getNotes(),supplier.getStatus(),money(total),money(paid),money(total.subtract(paid)));
    }
    private InventoryDtos.ItemResponse itemResponse(InventoryItem item) {
        return new InventoryDtos.ItemResponse(item.getUuid(),item.getName(),item.getType(),item.getCategory(),item.getUnit(),
                money(item.getPurchasePrice()),quantity(item.getCurrentStock()),quantity(item.getMinimumStock()),
                item.getSupplier()==null?null:item.getSupplier().getUuid(),item.getSupplier()==null?null:item.getSupplier().getName(),
                item.getBatchNumber(),item.getExpiryDate(),item.getNotes(),item.getStatus(),
                item.getCurrentStock().compareTo(item.getMinimumStock())<=0,
                item.getExpiryDate()!=null&&item.getExpiryDate().isBefore(today()));
    }
    private InventoryDtos.PurchaseResponse purchaseResponse(FeedPurchase p) {
        return new InventoryDtos.PurchaseResponse(p.getUuid(),p.getSupplier().getUuid(),p.getSupplier().getName(),
                p.getItem().getUuid(),p.getItem().getName(),p.getItem().getUnit(),p.getPurchaseDate(),p.getQuantity(),
                p.getUnitPrice(),p.getTotalAmount(),p.getInvoiceNumber(),p.getPaymentStatus(),p.getAmountPaid(),
                money(p.getTotalAmount().subtract(p.getAmountPaid())),p.getPaymentMethod(),p.getNotes(),p.getCreatedAt());
    }
    private InventoryDtos.ConsumptionResponse consumptionResponse(FeedConsumption row) {
        return new InventoryDtos.ConsumptionResponse(row.getUuid(),row.getItem().getUuid(),row.getItem().getName(),
                row.getItem().getUnit(),row.getAnimal()==null?null:row.getAnimal().getUuid(),
                row.getAnimal()==null?null:row.getAnimal().getTagNumber(),row.getTargetGroup(),row.getDate(),
                row.getQuantity(),row.getUnitCost(),row.getTotalCost(),row.getNotes(),row.getCreatedAt());
    }
    private InventoryDtos.RationResponse rationResponse(FeedRation ration,LocalDate date) {
        List<Animal> eligible=ration.getAnimal()==null?animals.findAllByFarm_UuidAndArchivedFalse(farmUuid()).stream()
                .filter(a->a.getStatus()==AnimalStatus.ACTIVE&&targetMatches(a,ration.getTargetGroup())).toList():List.of(ration.getAnimal());
        int count=ration.isActive()&&!date.isBefore(ration.getEffectiveFrom())
                &&(ration.getEffectiveUntil()==null||!date.isAfter(ration.getEffectiveUntil()))?eligible.size():0;
        BigDecimal dailyQty=ZERO,dailyCost=MONEY_ZERO; List<InventoryDtos.RationLineResponse> lines=new ArrayList<>();
        for(FeedRationLine line:ration.getLines()) {
            BigDecimal q=line.getQuantityPerAnimalDay(),cost=money(q.multiply(line.getItem().getPurchasePrice()));
            dailyQty=dailyQty.add(q); dailyCost=dailyCost.add(cost);
            lines.add(new InventoryDtos.RationLineResponse(line.getItem().getUuid(),line.getItem().getName(),
                    line.getItem().getUnit(),q,cost));
        }
        return new InventoryDtos.RationResponse(ration.getUuid(),ration.getName(),ration.getTargetGroup(),
                ration.getAnimal()==null?null:ration.getAnimal().getUuid(),
                ration.getAnimal()==null?null:ration.getAnimal().getTagNumber(),ration.getEffectiveFrom(),
                ration.getEffectiveUntil(),ration.isActive(),count,quantity(dailyQty),money(dailyCost),
                quantity(dailyQty.multiply(BigDecimal.valueOf(30))),money(dailyCost.multiply(BigDecimal.valueOf(30))),
                List.copyOf(lines));
    }
    private InventoryDtos.InventoryTransactionResponse transactionResponse(InventoryTransaction row) {
        return new InventoryDtos.InventoryTransactionResponse(row.getUuid(),row.getItem().getUuid(),row.getItem().getName(),
                row.getType(),row.getQuantityChange(),row.getItem().getUnit(),row.getUnitCost(),row.getTransactionDate(),
                row.getAnimal()==null?null:row.getAnimal().getUuid(),row.getAnimal()==null?null:row.getAnimal().getTagNumber(),
                row.getReferenceUuid(),row.getReason(),row.getRecordedBy().getFullName(),row.getCreatedAt());
    }
    private InventoryDtos.AlertResponse alert(InventoryItem item,String type) {
        return new InventoryDtos.AlertResponse(item.getUuid(),item.getName(),item.getType(),item.getCurrentStock(),
                item.getMinimumStock(),item.getUnit(),type,item.getExpiryDate());
    }
    private void applySupplier(Supplier s,InventoryDtos.SupplierRequest r) {
        s.setName(r.name().trim()); s.setPhone(clean(r.phone())); s.setAddress(clean(r.address()));
        s.setGstNumber(clean(r.gstNumber())); s.setContactPerson(clean(r.contactPerson()));
        s.setType(r.type()); s.setNotes(clean(r.notes())); s.setStatus(r.status());
    }
    private void applyItem(InventoryItem item,InventoryDtos.ItemRequest r) {
        item.setName(r.name().trim()); item.setType(r.type()); item.setCategory(r.category().trim().toUpperCase());
        item.setUnit(r.unit().trim().toUpperCase()); item.setPurchasePrice(money(r.purchasePrice()));
        item.setMinimumStock(quantity(r.minimumStock())); item.setSupplier(r.supplierUuid()==null?null:supplier(r.supplierUuid()));
        item.setBatchNumber(clean(r.batchNumber())); item.setExpiryDate(r.expiryDate());
        item.setNotes(clean(r.notes())); item.setStatus(r.status());
    }
    private void validateItemSupplier(InventoryDtos.ItemRequest request) {
        if(request.supplierUuid()!=null) supplier(request.supplierUuid());
    }
    private boolean itemExists(UUID farm,String name,String batch) {
        return batch==null?items.existsByFarm_UuidAndNameIgnoreCaseAndBatchNumberIsNull(farm,name.trim()):
                items.existsByFarm_UuidAndNameIgnoreCaseAndBatchNumberIgnoreCase(farm,name.trim(),batch.trim());
    }
    private boolean itemExistsExcept(UUID farm,String name,String batch,UUID uuid) {
        return items.findAllByFarm_UuidOrderByNameAsc(farm).stream().anyMatch(i->!i.getUuid().equals(uuid)
                &&i.getName().equalsIgnoreCase(name.trim())
                &&java.util.Objects.equals(clean(i.getBatchNumber()),clean(batch)));
    }
    private void recordSupplierPayment(Farm farm,FeedPurchase purchase,LocalDate date,BigDecimal amount,String method,String reference) {
        SupplierPayment payment=new SupplierPayment(); payment.setFarm(farm); payment.setPurchase(purchase);
        payment.setPaymentDate(date); payment.setAmount(money(amount)); payment.setPaymentMethod(method);
        payment.setReferenceNumber(reference); payment.setRecordedBy(currentUser.get()); supplierPayments.save(payment);
    }
    private void recordTransaction(Farm farm,InventoryItem item,InventoryTransactionType type,BigDecimal change,
            BigDecimal unitCost,LocalDate date,UUID reference,Animal animal,String reason) {
        InventoryTransaction tx=new InventoryTransaction(); tx.setFarm(farm); tx.setItem(item); tx.setType(type);
        tx.setQuantityChange(quantity(change)); tx.setUnitCost(money(unitCost)); tx.setTransactionDate(date);
        tx.setReferenceUuid(reference); tx.setAnimal(animal); tx.setReason(clean(reason)); tx.setRecordedBy(currentUser.get());
        transactions.save(tx);
    }
    private void requireStock(InventoryItem item,BigDecimal requested) {
        if(item.getCurrentStock().compareTo(requested)<0)
            throw new ConflictException("Insufficient "+item.getUnit()+" stock for "+item.getName()+".");
    }
    private void validateTarget(UUID animalUuid,RationTargetGroup group) {
        if((animalUuid==null)==(group==null))
            throw new IllegalArgumentException("Choose exactly one animal or animal group.");
    }
    private boolean targetMatches(Animal animal,RationTargetGroup group) {
        if(group==null) return false;
        return switch(group) {
            case LACTATING_COW -> animal.getType()==AnimalType.COW&&animal.getGender()==Gender.FEMALE&&animal.isLactating();
            case DRY_COW -> animal.getType()==AnimalType.COW&&animal.getGender()==Gender.FEMALE&&!animal.isLactating()
                    &&animal.getPregnancyStatus()!=PregnancyStatus.PREGNANT;
            case PREGNANT_COW -> animal.getType()==AnimalType.COW&&animal.getGender()==Gender.FEMALE
                    &&animal.getPregnancyStatus()==PregnancyStatus.PREGNANT;
            case HEIFER -> animal.getType()==AnimalType.HEIFER;
            case CALF -> animal.getType()==AnimalType.CALF;
            case BULL -> animal.getType()==AnimalType.BULL;
        };
    }
    private void validPeriod(LocalDate start,LocalDate end) {
        if(end!=null&&end.isBefore(start)) throw new IllegalArgumentException("Effective end date must be on or after start date.");
    }
    private void range(LocalDate from,LocalDate to) {
        if(from==null||to==null||to.isBefore(from)) throw new IllegalArgumentException("Provide a valid date range.");
    }
    private boolean isReportTransaction(String type,InventoryTransaction tx) {
        return switch(type) {
            case "wastage" -> tx.getType()==InventoryTransactionType.WASTAGE;
            case "medicine-usage" -> tx.getItem().getType()==InventoryItemType.MEDICINE
                    &&(tx.getType()==InventoryTransactionType.TREATMENT||tx.getType()==InventoryTransactionType.VACCINATION);
            default -> false;
        };
    }
    private Supplier supplier(UUID uuid) {
        return suppliers.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Supplier not found in this farm."));
    }
    private InventoryItem item(UUID uuid) {
        return items.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Inventory item not found in this farm."));
    }
    private FeedPurchase purchase(UUID uuid) {
        return purchases.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Purchase not found in this farm."));
    }
    private FeedConsumption consumption(UUID uuid) {
        return consumptions.findByUuidAndFarm_Uuid(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Consumption record not found in this farm."));
    }
    private Animal animal(UUID uuid) {
        return animals.findByUuidAndFarm_UuidAndArchivedFalse(uuid,farmUuid())
                .orElseThrow(()->new NotFoundException("Animal not found in this farm."));
    }
    private Farm lockedFarm() {
        return farms.findLockedByUuid(farmUuid()).orElseThrow(()->new NotFoundException("Farm not found."));
    }
    private UUID farmUuid() { return currentUser.farmUuid(); }
    private LocalDate today() { return LocalDate.now(clock); }
    private void requireManager() {
        AppUser user=currentUser.get();
        if(user.getRoles().stream().noneMatch(r->r.getName()==UserRole.ADMIN||r.getName()==UserRole.MANAGER))
            throw new AccessDeniedException("Administrator or manager access is required.");
    }
    private void requireManagerOrAccountant() {
        AppUser user=currentUser.get();
        if(user.getRoles().stream().noneMatch(r->r.getName()==UserRole.ADMIN||r.getName()==UserRole.MANAGER
                ||r.getName()==UserRole.ACCOUNTANT))
            throw new AccessDeniedException("Administrator, manager, or accountant access is required.");
    }
    private static SupplierPaymentStatus paymentStatus(BigDecimal total,BigDecimal paid) {
        if(paid.compareTo(total)>=0) return SupplierPaymentStatus.PAID;
        return paid.signum()>0?SupplierPaymentStatus.PARTIALLY_PAID:SupplierPaymentStatus.PENDING;
    }
    private static BigDecimal quantity(BigDecimal value) { return value.setScale(3,RoundingMode.HALF_UP); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
    private static String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
}
