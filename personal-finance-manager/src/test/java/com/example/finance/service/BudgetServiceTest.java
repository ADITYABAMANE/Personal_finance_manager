package com.example.finance.service;

import com.example.finance.dto.request.BudgetRequest;
import com.example.finance.dto.response.BudgetResponse;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetStatus;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.exception.DuplicateResourceException;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("jane@example.com").build();
        category = Category.builder().id(10L).name("Food").user(user).build();
    }

    @Test
    void createBudget_shouldThrow_whenBudgetAlreadyExistsForCategoryAndMonth() {
        BudgetRequest request = new BudgetRequest(10L, new BigDecimal("8000"), 8, 2026);
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(1L, 10L, 8, 2026)).thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudget_shouldPersist_whenNoDuplicateExists() {
        BudgetRequest request = new BudgetRequest(10L, new BigDecimal("8000"), 8, 2026);
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(1L, 10L, 8, 2026)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryService.getCategoryEntity(1L, 10L)).thenReturn(category);

        Budget saved = Budget.builder()
                .id(50L).category(category).amount(new BigDecimal("8000"))
                .month(8).year(2026).user(user).build();
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                eq(1L), eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        BudgetResponse response = budgetService.createBudget(1L, request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getCategoryName()).isEqualTo("Food");
        assertThat(response.getStatus()).isEqualTo(BudgetStatus.WITHIN_LIMIT);
    }

    @Test
    void getBudget_shouldReturnOverBudgetStatus_whenSpendingExceedsBudget() {
        Budget budget = Budget.builder()
                .id(50L).category(category).amount(new BigDecimal("1000"))
                .month(8).year(2026).user(user).build();
        when(budgetRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                eq(1L), eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1200"));

        BudgetResponse response = budgetService.getBudget(1L, 50L);

        assertThat(response.getStatus()).isEqualTo(BudgetStatus.OVER_BUDGET);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("-200.00");
    }

    @Test
    void getBudget_shouldReturnNearLimitStatus_whenSpendingAt80PercentOrMore() {
        Budget budget = Budget.builder()
                .id(50L).category(category).amount(new BigDecimal("1000"))
                .month(8).year(2026).user(user).build();
        when(budgetRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                eq(1L), eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("850"));

        BudgetResponse response = budgetService.getBudget(1L, 50L);

        assertThat(response.getStatus()).isEqualTo(BudgetStatus.NEAR_LIMIT);
        assertThat(response.getPercentageUsed()).isEqualByComparingTo("85.00");
    }
}
