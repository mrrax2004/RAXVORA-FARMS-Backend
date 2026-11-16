package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_payments", indexes = @Index(name = "idx_payment_farm_customer_date", columnList = "farm_uuid,customer_uuid,payment_date"))
public class CustomerPayment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_uuid", nullable = false) private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_uuid", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_uuid") private MilkSale sale;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bill_uuid") private MilkBill bill;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 20) private PaymentMethod method;
    @Column(name = "transaction_reference", length = 160) private String transactionReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "received_by_uuid", nullable = false) private AppUser receivedBy;
    @Column(length = 1000) private String notes;
    public Farm getFarm() { return farm; } public void setFarm(Farm v) { farm = v; }
    public Customer getCustomer() { return customer; } public void setCustomer(Customer v) { customer = v; }
    public MilkSale getSale() { return sale; } public void setSale(MilkSale v) { sale = v; }
    public MilkBill getBill() { return bill; } public void setBill(MilkBill v) { bill = v; }
    public LocalDate getPaymentDate() { return paymentDate; } public void setPaymentDate(LocalDate v) { paymentDate = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
    public PaymentMethod getMethod() { return method; } public void setMethod(PaymentMethod v) { method = v; }
    public String getTransactionReference() { return transactionReference; } public void setTransactionReference(String v) { transactionReference = v; }
    public AppUser getReceivedBy() { return receivedBy; } public void setReceivedBy(AppUser v) { receivedBy = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
}
