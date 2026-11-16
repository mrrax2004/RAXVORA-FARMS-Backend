package com.dairyfarm.model;

public enum FinanceCategory {
    LIVESTOCK_SALE(FinanceType.INCOME), MANURE_SALE(FinanceType.INCOME),
    SUBSIDY(FinanceType.INCOME), OTHER_INCOME(FinanceType.INCOME),
    LABOUR(FinanceType.EXPENSE), VETERINARY(FinanceType.EXPENSE),
    UTILITIES(FinanceType.EXPENSE), MAINTENANCE(FinanceType.EXPENSE),
    TRANSPORT(FinanceType.EXPENSE), EQUIPMENT(FinanceType.EXPENSE),
    OTHER_EXPENSE(FinanceType.EXPENSE);

    private final FinanceType type;
    FinanceCategory(FinanceType type) { this.type = type; }
    public FinanceType getType() { return type; }
}
