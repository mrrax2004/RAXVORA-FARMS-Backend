package com.dairyfarm.service;

import com.dairyfarm.exception.ConflictException;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.*;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.MilkStockTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The sole writer for the append-only farm milk stock ledger. */
@Service
public class MilkStockService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final MilkStockTransactionRepository transactions;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;

    public MilkStockService(MilkStockTransactionRepository transactions, FarmRepository farms,
                            CurrentUserService currentUser) {
        this.transactions = transactions;
        this.farms = farms;
        this.currentUser = currentUser;
    }

    @Transactional
    public void record(Farm farm, MilkStockTransactionType type, BigDecimal change,
                       MilkCollection collection, MilkProduction production, MilkSale sale,
                       MilkDelivery delivery, AavinMilkSupply supply, String note) {
        Farm locked = farms.findLockedByUuid(farm.getUuid())
                .orElseThrow(() -> new NotFoundException("Farm not found."));
        currentUser.requireFarmAccess(locked.getUuid());
        if (change == null || change.signum() == 0)
            throw new IllegalArgumentException("A milk stock movement must change stock by a non-zero quantity.");
        if (transactions.stock(locked.getUuid()).add(change).signum() < 0)
            throw new ConflictException("Milk stock cannot become negative.");

        MilkStockTransaction row = new MilkStockTransaction();
        row.setFarm(locked);
        row.setType(type);
        row.setQuantityChange(change);
        row.setCollection(collection);
        row.setProduction(production);
        row.setSale(sale);
        row.setDelivery(delivery);
        row.setSupply(supply);
        row.setRecordedBy(currentUser.get());
        row.setReferenceNote(note);
        transactions.save(row);
    }

    @Transactional(readOnly = true)
    public BigDecimal available(UUID farmUuid) {
        currentUser.requireFarmAccess(farmUuid);
        return transactions.stock(farmUuid);
    }

    @Transactional(readOnly = true)
    public BigDecimal supplyMovement(UUID farmUuid, UUID supplyUuid) {
        currentUser.requireFarmAccess(farmUuid);
        return transactions.sumByFarmAndSupply(farmUuid, supplyUuid);
    }
}
