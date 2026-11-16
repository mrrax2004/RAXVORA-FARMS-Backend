package com.dairyfarm.service;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringFinanceScheduler {
    private final FinanceService finance;
    public RecurringFinanceScheduler(FinanceService finance) { this.finance=finance; }
    @Scheduled(cron="0 10 0 * * *",zone="${app.farm.time-zone}")
    public void generateDueTransactions() {
        finance.generateDueForAllFarms();
    }
}
