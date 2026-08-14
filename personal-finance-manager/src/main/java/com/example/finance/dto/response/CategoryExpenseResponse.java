package com.example.finance.dto.response;

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
public class CategoryExpenseResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal totalSpent;
    private BigDecimal percentageOfTotalExpenses;
}
