package com.example.finance.service;

import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getMonthlyReport_shouldCalculateSavingsAndHighestCategory() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(1L), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("60000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(1L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("35000"));
        when(transactionRepository.sumExpensesGroupedByCategory(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[]{10L, "Food", new BigDecimal("20000")},
                        new Object[]{20L, "Travel", new BigDecimal("15000")}
                ));

        MonthlyReportResponse response = reportService.getMonthlyReport(1L, 8, 2026);

        assertThat(response.getIncome()).isEqualByComparingTo("60000.00");
        assertThat(response.getExpenses()).isEqualByComparingTo("35000.00");
        assertThat(response.getSavings()).isEqualByComparingTo("25000.00");
        assertThat(response.getHighestSpendingCategory()).isEqualTo("Food");
        assertThat(response.getHighestSpendingAmount()).isEqualByComparingTo("20000.00");
    }

    @Test
    void getCategoryWiseReport_shouldCalculatePercentagesCorrectly() {
        when(transactionRepository.sumExpensesGroupedByCategory(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[]{10L, "Food", new BigDecimal("300")},
                        new Object[]{20L, "Travel", new BigDecimal("100")}
                ));

        List<CategoryExpenseResponse> report = reportService.getCategoryWiseReport(1L, 8, 2026);

        assertThat(report).hasSize(2);
        assertThat(report.get(0).getCategoryName()).isEqualTo("Food");
        assertThat(report.get(0).getPercentageOfTotalExpenses()).isEqualByComparingTo("75.00");
        assertThat(report.get(1).getPercentageOfTotalExpenses()).isEqualByComparingTo("25.00");
    }
}
