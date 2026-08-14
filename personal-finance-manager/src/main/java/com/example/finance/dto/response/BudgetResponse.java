package com.example.finance.dto.response;

import com.example.finance.entity.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal budgetAmount;
    private BigDecimal amountSpent;
    private BigDecimal remainingAmount;
    private BigDecimal percentageUsed;
    private BudgetStatus status;
    private Integer month;
    private Integer year;
}
