package com.dairyfarm.repository;
import com.dairyfarm.model.CustomerPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID> {
    boolean existsByFarm_UuidAndTransactionReference(UUID farm, String transactionReference);
    Page<CustomerPayment> findAllByFarm_UuidAndPaymentDateBetween(UUID farm, LocalDate from, LocalDate to, Pageable pageable);
    java.util.List<CustomerPayment> findAllByFarm_UuidAndPaymentDateBetween(UUID farm, LocalDate from, LocalDate to);
    Page<CustomerPayment> findAllByFarm_UuidAndCustomer_UuidAndPaymentDateBetween(UUID farm, UUID customer, LocalDate from, LocalDate to, Pageable pageable);
    @Query("select coalesce(sum(p.amount),0) from CustomerPayment p where p.sale.uuid=:sale")
    BigDecimal paidForSale(@Param("sale") UUID sale);
    @Query("select coalesce(sum(p.amount),0) from CustomerPayment p where p.bill.uuid=:bill")
    BigDecimal paidForBill(@Param("bill") UUID bill);
    @Query("select coalesce((select sum(s.totalAmount) from MilkSale s where s.customer.uuid=:customer),0) + " +
            "coalesce((select sum(b.totalAmount + b.adjustmentAmount) from MilkBill b where b.customer.uuid=:customer),0) - " +
            "coalesce((select sum(p.amount) from CustomerPayment p where p.customer.uuid=:customer),0)")
    BigDecimal outstanding(@Param("customer") UUID customer);
    @Query("select coalesce((select sum(s.totalAmount) from MilkSale s where s.farm.uuid=:farm),0) + " +
            "coalesce((select sum(b.totalAmount + b.adjustmentAmount) from MilkBill b where b.farm.uuid=:farm),0) - " +
            "coalesce((select sum(p.amount) from CustomerPayment p where p.farm.uuid=:farm),0)")
    BigDecimal farmOutstanding(@Param("farm") UUID farm);
}
