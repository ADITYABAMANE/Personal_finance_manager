package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
            AND (:categoryId IS NULL OR t.category.id = :categoryId)
            AND (:type IS NULL OR t.type = :type)
            AND (:startDate IS NULL OR t.transactionDate >= :startDate)
            AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            """)
    Page<Transaction> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user.id = :userId AND t.type = :type
            AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumByUserAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.type = 'EXPENSE'
            AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumExpensesByUserAndCategoryAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT t.category.id, t.category.name, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId AND t.type = 'EXPENSE'
            AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category.id, t.category.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumExpensesGroupedByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
