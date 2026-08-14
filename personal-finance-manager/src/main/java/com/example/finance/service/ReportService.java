package com.example.finance.service;

import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    public MonthlyReportResponse getMonthlyReport(Long userId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal income = transactionRepository.sumByUserAndTypeAndDateRange(
                userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal expenses = transactionRepository.sumByUserAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate);
        BigDecimal savings = income.subtract(expenses);

        List<Object[]> grouped = transactionRepository.sumExpensesGroupedByCategory(userId, startDate, endDate);

        String highestCategoryName = null;
        BigDecimal highestAmount = BigDecimal.ZERO;
        if (!grouped.isEmpty()) {
            Object[] top = grouped.get(0);
            highestCategoryName = (String) top[1];
            highestAmount = (BigDecimal) top[2];
        }

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .income(income.setScale(2, RoundingMode.HALF_UP))
                .expenses(expenses.setScale(2, RoundingMode.HALF_UP))
                .savings(savings.setScale(2, RoundingMode.HALF_UP))
                .highestSpendingCategory(highestCategoryName)
                .highestSpendingAmount(highestAmount.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    public List<CategoryExpenseResponse> getCategoryWiseReport(Long userId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Object[]> grouped = transactionRepository.sumExpensesGroupedByCategory(userId, startDate, endDate);

        BigDecimal totalExpenses = grouped.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return grouped.stream()
                .map(row -> {
                    Long categoryId = (Long) row[0];
                    String categoryName = (String) row[1];
                    BigDecimal totalSpent = (BigDecimal) row[2];

                    BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : totalSpent.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);

                    return CategoryExpenseResponse.builder()
                            .categoryId(categoryId)
                            .categoryName(categoryName)
                            .totalSpent(totalSpent.setScale(2, RoundingMode.HALF_UP))
                            .percentageOfTotalExpenses(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
