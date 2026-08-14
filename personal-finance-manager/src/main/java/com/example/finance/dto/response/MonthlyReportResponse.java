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
public class MonthlyReportResponse {

    private Integer month;
    private Integer year;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal savings;
    private String highestSpendingCategory;
    private BigDecimal highestSpendingAmount;
}
