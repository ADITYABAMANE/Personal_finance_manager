package com.example.finance.controller;

import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.service.ReportService;
import com.example.finance.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reports", description = "Monthly and category-wise financial reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(summary = "Get income, expenses and savings for a given month")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2000) int year) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(reportService.getMonthlyReport(userId, month, year));
    }

    @GetMapping("/category-wise")
    @Operation(summary = "Get expense breakdown by category for a given month")
    public ResponseEntity<List<CategoryExpenseResponse>> getCategoryWiseReport(
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2000) int year) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(reportService.getCategoryWiseReport(userId, month, year));
    }
}
