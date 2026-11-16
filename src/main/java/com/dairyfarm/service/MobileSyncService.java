package com.dairyfarm.service;

import com.dairyfarm.dto.InventoryDtos;
import com.dairyfarm.dto.MobileDtos;
import com.dairyfarm.dto.PhaseThreeDtos;
import com.dairyfarm.dto.MilkProcurementDtos;
import com.dairyfarm.dto.ProcurementDtos;
import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileSyncService {
    private final MobileSyncRecordRepository syncRecords;
    private final FarmRepository farms;
    private final AnimalRepository animals;
    private final MilkManagementService milk;
    private final MilkProcurementService procurement;
    private final ProcurementDirectoryService directory;
    private final InventoryService inventory;
    private final CustomerPaymentRepository payments;
    private final MilkSaleRepository sales;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public MobileSyncService(MobileSyncRecordRepository syncRecords,FarmRepository farms,AnimalRepository animals,
            MilkManagementService milk,MilkProcurementService procurement,ProcurementDirectoryService directory,
            InventoryService inventory,CustomerPaymentRepository payments,MilkSaleRepository sales,
            CurrentUserService currentUser,Clock clock) {
        this.syncRecords=syncRecords; this.farms=farms; this.animals=animals; this.milk=milk;
        this.procurement=procurement; this.directory=directory; this.inventory=inventory;
        this.payments=payments; this.sales=sales; this.currentUser=currentUser; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public MobileDtos.SnapshotResponse snapshot() {
        UUID farm=currentUser.farmUuid();
        List<MobileDtos.MobileAnimal> animalList=animals.findAllByFarm_UuidAndArchivedFalse(farm).stream()
                .filter(row->row.getStatus()==AnimalStatus.ACTIVE)
                .map(row->new MobileDtos.MobileAnimal(row.getUuid(),row.getTagNumber(),row.getName(),
                        row.getType().name(),row.isLactating())).toList();
        List<InventoryDtos.ItemResponse> feedItems=inventory.items(InventoryItemType.FEED).stream()
                .filter(row->row.status()==InventoryItemStatus.ACTIVE).toList();
        LocalDate date=LocalDate.now(clock);
        List<MobileDtos.MobileCustomer> customerList=new ArrayList<>();
        var customerPage=milk.customers("",null,null,0,100);
        for(int page=0;page<customerPage.getTotalPages();page++) {
            if(page>0) customerPage=milk.customers("",null,null,page,100);
            for(PhaseThreeDtos.CustomerResponse customer:customerPage.getContent()) {
                PhaseThreeDtos.MilkRateQuoteResponse quote=milk.milkRateQuote(customer.uuid(),date);
                customerList.add(new MobileDtos.MobileCustomer(customer.uuid(),customer.name(),customer.mobileNumber(),
                        customer.status().name(),
                        quote.ratePerLitre(),quote.source(),customer.outstandingBalance(),quote.date(),
                        customer.rateEffectiveFrom(),customer.rateEffectiveUntil()));
            }
        }
        List<ProcurementDtos.CompanyResponse> companyRows=directory.companies();
        List<ProcurementDtos.CentreResponse> centreRows=directory.centres(null);
        List<MobileDtos.MobilePricing> pricingRows=new ArrayList<>();
        for(ProcurementDtos.CompanyResponse company:companyRows) {
            for(ProcurementDtos.PricingResponse row:directory.pricing(company.uuid()))
                pricingRows.add(new MobileDtos.MobilePricing(row.uuid(),row.companyUuid(),row.pricingMethod().name(),
                        row.effectiveFrom(),row.effectiveUntil(),row.baseFatPercentage(),row.baseRate(),
                        row.fatRatePerPercentage(),row.snfRatePerPercentage(),row.fixedRatePerLitre()));
        }
        List<MobileDtos.MobileRateChart> chartRows=new ArrayList<>();
        for(ProcurementDtos.CentreResponse centre:centreRows) {
            for(MilkProcurementDtos.RateChartResponse chart:procurement.rateCharts(centre.legacyBuyerUuid()))
                chartRows.add(new MobileDtos.MobileRateChart(chart.uuid(),chart.buyerUuid(),chart.effectiveFrom(),
                        chart.effectiveUntil(),chart.slabs().stream().map(s->new MobileDtos.MobileRateSlab(
                                s.fatPercentage(),s.snfPercentage(),s.ratePerLitre())).toList()));
        }
        List<MobileDtos.MobileCompany> companies=companyRows.stream().map(c->new MobileDtos.MobileCompany(
                c.uuid(),c.companyName(),c.companyCode(),c.status().name())).toList();
        List<MobileDtos.MobileCentre> centres=centreRows.stream().map(c->new MobileDtos.MobileCentre(
                c.uuid(),c.legacyBuyerUuid(),c.companyUuid(),c.companyName(),c.centreName(),c.centreCode(),c.status().name())).toList();
        return new MobileDtos.SnapshotResponse(farm,animalList,feedItems,date,milk.defaultMilkRate().ratePerLitre(),
                customerList,companies,centres,pricingRows,chartRows);
    }

    @Transactional(readOnly=true)
    public MobileDtos.DashboardResponse dashboard() {
        LocalDate date=LocalDate.now(clock);
        PhaseThreeDtos.DashboardResponse milkDashboard=milk.dashboard();
        MilkProcurementDtos.DashboardResponse buyerDashboard=procurement.dashboard();
        var saleRows=sales.findAllByFarm_UuidAndSaleDateBetweenOrderBySaleDateAsc(currentUser.farmUuid(),date,date);
        BigDecimal credit=BigDecimal.ZERO;
        for(MilkSale sale:saleRows) {
            if(sale.getPaymentMethod()==PaymentMethod.CREDIT) {
                BigDecimal paid=payments.paidForSale(sale.getUuid());
                credit=credit.add(sale.getTotalAmount().subtract(paid).max(BigDecimal.ZERO));
            }
        }
        BigDecimal cash=payments.findAllByFarm_UuidAndPaymentDateBetween(currentUser.farmUuid(),date,date).stream()
                .filter(p->p.getMethod()==PaymentMethod.CASH).map(CustomerPayment::getAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal supplyValue=BigDecimal.ZERO;
        long pending=0;
        var supplyPage=procurement.supplies(date,date,null,0,100);
        for(int page=0;page<supplyPage.getTotalPages();page++) {
            if(page>0) supplyPage=procurement.supplies(date,date,null,page,100);
            for(MilkProcurementDtos.SupplyResponse row:supplyPage.getContent()) {
                BigDecimal amount=row.finalValue()!=null?row.finalValue():row.estimatedValue();
                if(amount!=null) supplyValue=supplyValue.add(amount);
                if(row.status()==AavinVerificationStatus.PENDING_VERIFICATION
                        ||row.status()==AavinVerificationStatus.RATE_UNAVAILABLE) pending++;
            }
        }
        return new MobileDtos.DashboardResponse(date,milkDashboard.todayProduction(),milkDashboard.todaySales(),
                buyerDashboard.todaySupply(),milkDashboard.availableStock(),milkDashboard.todayRevenue(),
                supplyValue,cash,credit,pending,milkDashboard.outstandingPayments());
    }

    @Transactional
    public PhaseThreeDtos.CustomerResponse createCustomer(PhaseThreeDtos.CustomerRequest request) {
        return milk.createCustomer(request);
    }

    @Transactional
    public MobileDtos.SyncResponse sync(MobileDtos.SyncRequest request) {
        UUID farmUuid=currentUser.farmUuid();
        Farm farm=farms.findLockedByUuid(farmUuid).orElseThrow(()->new com.dairyfarm.exception.NotFoundException("Farm not found."));
        List<MobileDtos.SyncResult> results=new ArrayList<>();
        for(MobileDtos.SyncItem item:request.records()) {
            if(item.createdByUuid()!=null&&!item.createdByUuid().equals(currentUser.get().getUuid()))
                throw new org.springframework.security.access.AccessDeniedException(
                        "Pending records must be synchronized by the user who created them.");
            String hash=payloadHash(item);
            var existing=syncRecords.findByFarm_UuidAndDeviceUuidAndClientRecordUuid(
                    farmUuid,request.deviceUuid(),item.clientRecordUuid());
            if(existing.isPresent()) {
                MobileSyncRecord saved=existing.get();
                if(!saved.getPayloadHash().equals(hash))
                    throw new ConflictException("A client record UUID cannot be reused with changed content.");
                results.add(new MobileDtos.SyncResult(item.clientRecordUuid(),saved.getServerRecordUuid(),
                        "DUPLICATE","Already synchronized."));
                continue;
            }
            UUID serverUuid=process(item);
            MobileSyncRecord saved=new MobileSyncRecord(); saved.setFarm(farm); saved.setDeviceUuid(request.deviceUuid());
            saved.setClientRecordUuid(item.clientRecordUuid()); saved.setPayloadHash(hash); saved.setType(item.type());
            saved.setServerRecordUuid(serverUuid); saved.setSyncedBy(currentUser.get());
            saved.setClientEstimatedRate(item.clientEstimatedRate());
            saved.setClientEstimatedAmount(item.clientEstimatedAmount());
            syncRecords.save(saved);
            results.add(new MobileDtos.SyncResult(item.clientRecordUuid(),serverUuid,"CREATED","Synchronized."));
        }
        return new MobileDtos.SyncResponse(results);
    }

    private UUID process(MobileDtos.SyncItem item) {
        if(item.date().isAfter(LocalDate.now(clock)))
            throw new IllegalArgumentException("Offline record date cannot be in the future.");
        if(item.type()==MobileSyncType.MILK_PRODUCTION) {
            if(item.animalUuid()==null||item.session()==null||item.itemUuid()!=null||item.targetGroup()!=null)
                throw new IllegalArgumentException("Milk production requires an animal and milking session only.");
            if(item.quantity()==null) throw new IllegalArgumentException("Milk quantity is required.");
            return milk.recordProduction(new PhaseThreeDtos.ProductionRequest(item.animalUuid(),item.date(),
                    item.session(),item.quantity(),item.notes())).uuid();
        }
        if(item.type()==MobileSyncType.FEED_CONSUMPTION) {
            if(item.itemUuid()==null||item.session()!=null||item.quantity()==null
                    ||((item.animalUuid()==null)==(item.targetGroup()==null)))
                throw new IllegalArgumentException("Feed consumption requires an item and exactly one animal or group.");
            return inventory.consume(new InventoryDtos.ConsumptionRequest(item.itemUuid(),item.animalUuid(),
                    item.targetGroup(),item.date(),item.quantity(),item.notes())).uuid();
        }
        if(item.type()==MobileSyncType.MILK_SALE) {
            if(item.customerUuid()==null||item.paymentMethod()==null||item.animalUuid()!=null||item.itemUuid()!=null
                    ||item.targetGroup()!=null||item.session()!=null)
                throw new IllegalArgumentException("A mobile sale requires a customer and payment method only.");
            String invoice=item.invoiceNumber()==null||item.invoiceNumber().isBlank()
                    ?"MOB-"+item.clientRecordUuid():item.invoiceNumber().trim();
            LocalTime saleTime=item.saleTime()==null||item.saleTime().isBlank()?null:LocalTime.parse(item.saleTime());
            BigDecimal amountReceived=item.amountReceived()==null?BigDecimal.ZERO:item.amountReceived();
            return milk.createSale(new PhaseThreeDtos.SaleRequest(invoice,item.customerUuid(),item.date(),
                    item.quantity(),null,BigDecimal.ZERO,item.paymentMethod(),amountReceived,item.notes(),
                    null,null,saleTime)).uuid();
        }
        if(item.type()==MobileSyncType.MILK_PAYMENT) {
            if(item.customerUuid()==null||item.paymentMethod()==null||item.amountReceived()==null
                    ||item.amountReceived().signum()<=0||item.animalUuid()!=null||item.itemUuid()!=null
                    ||item.targetGroup()!=null||item.session()!=null)
                throw new IllegalArgumentException("A mobile payment requires a customer, positive amount, and payment method.");
            return milk.payment(new PhaseThreeDtos.PaymentRequest(item.customerUuid(),null,item.date(),
                    item.amountReceived(),item.paymentMethod(),item.transactionReference(),item.notes(),null)).uuid();
        }
        if(item.type()==MobileSyncType.MILK_SUPPLY) {
            if(item.buyerUuid()==null||item.session()==null
                    ||item.animalUuid()!=null||item.itemUuid()!=null||item.targetGroup()!=null)
                throw new IllegalArgumentException("A company supply requires a collection centre and session.");
            MilkProcurementDtos.SupplyResponse supply=procurement.createSupply(new MilkProcurementDtos.SupplyRequest(
                    item.buyerUuid(),item.date(),AavinSession.valueOf(item.session().name()),item.quantity(),
                    item.farmFat(),item.farmSnf(),item.density(),item.sampleReference(),item.receiptNumber(),
                    item.notes(),item.buyerFat(),item.buyerSnf(),item.acceptedQuantity(),item.rejectedQuantity(),
                    item.rejectionReason()));
            return supply.uuid();
        }
        throw new IllegalArgumentException("Unsupported mobile sync record type.");
    }

    private String payloadHash(MobileDtos.SyncItem item) {
        String data=String.join("|",item.clientRecordUuid().toString(),item.type().name(),item.date().toString(),
                value(item.animalUuid()),value(item.itemUuid()),value(item.session()),value(item.quantity()),
                value(item.targetGroup()),item.notes()==null?"":item.notes().trim(),value(item.createdByUuid()),
                value(item.customerUuid()),value(item.invoiceNumber()),value(item.saleTime()),value(item.paymentMethod()),
                value(item.amountReceived()),value(item.transactionReference()),value(item.clientEstimatedRate()),value(item.clientEstimatedAmount()),
                value(item.buyerUuid()),value(item.farmFat()),value(item.farmSnf()),value(item.density()),
                value(item.sampleReference()),value(item.receiptNumber()),value(item.buyerFat()),value(item.buyerSnf()),
                value(item.acceptedQuantity()),value(item.rejectedQuantity()),value(item.rejectionReason()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.",e);
        }
    }
    private String value(Object value) { return value==null?"":value.toString(); }
}
