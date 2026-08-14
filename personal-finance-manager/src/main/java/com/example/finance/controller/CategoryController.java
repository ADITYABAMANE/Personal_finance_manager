package com.example.finance.controller;

import com.example.finance.dto.request.CategoryRequest;
import com.example.finance.dto.response.CategoryResponse;
import com.example.finance.service.CategoryService;
import com.example.finance.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Manage expense and income categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a custom category")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        CategoryResponse response = categoryService.createCategory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all categories for the current user")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getAllCategories(userId));
    }
}
