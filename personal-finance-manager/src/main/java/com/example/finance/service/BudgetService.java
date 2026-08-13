package com.example.finance.service;

import com.example.finance.dto.request.BudgetRequest;
import com.example.finance.dto.response.BudgetResponse;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetStatus;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.exception.DuplicateResourceException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    /** Once spending crosses this fraction of the budget, status becomes NEAR_LIMIT. */
    private static final BigDecimal NEAR_LIMIT_THRESHOLD = new BigDecimal("0.80");

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BudgetResponse createBudget(Long userId, BudgetRequest request) {
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear())) {
            throw new DuplicateResourceException(
                    "A budget for this category already exists for " + request.getMonth() + "/" + request.getYear());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Category category = categoryService.getCategoryEntity(userId, request.getCategoryId());

        Budget budget = Budget.builder()
                .category(category)
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .user(user)
                .build();

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    public List<BudgetResponse> getAllBudgets(Long userId, Integer month, Integer year) {
        List<Budget> budgets = (month != null && year != null)
                ? budgetRepository.findAllByUserIdAndMonthAndYear(userId, month, year)
                : budgetRepository.findAllByUserId(userId);

        return budgets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BudgetResponse getBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Budget", budgetId));
        return toResponse(budget);
    }

    private BudgetResponse toResponse(Budget budget) {
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal spent = transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                budget.getUser().getId(), budget.getCategory().getId(), startDate, endDate);

        BigDecimal budgetAmount = budget.getAmount();
        BigDecimal remaining = budgetAmount.subtract(spent);

        BigDecimal percentageUsed = budgetAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : spent.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        BudgetStatus status = determineStatus(spent, budgetAmount);

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .budgetAmount(budgetAmount)
                .amountSpent(spent.setScale(2, RoundingMode.HALF_UP))
                .remainingAmount(remaining.setScale(2, RoundingMode.HALF_UP))
                .percentageUsed(percentageUsed)
                .status(status)
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();
    }

    private BudgetStatus determineStatus(BigDecimal spent, BigDecimal budgetAmount) {
        if (spent.compareTo(budgetAmount) > 0) {
            return BudgetStatus.OVER_BUDGET;
        }
        BigDecimal nearLimitThresholdAmount = budgetAmount.multiply(NEAR_LIMIT_THRESHOLD);
        if (spent.compareTo(nearLimitThresholdAmount) >= 0) {
            return BudgetStatus.NEAR_LIMIT;
        }
        return BudgetStatus.WITHIN_LIMIT;
    }
}
