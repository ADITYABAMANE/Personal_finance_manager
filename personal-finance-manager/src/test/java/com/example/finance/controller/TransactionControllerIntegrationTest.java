package com.example.finance.controller;

import com.example.finance.dto.request.RegisterRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.response.AuthResponse;
import com.example.finance.dto.response.CategoryResponse;
import com.example.finance.entity.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail(email);
        request.setPassword("securePass123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getAccessToken();
    }

    private Long getFoodCategoryId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        List<CategoryResponse> categories = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryResponse.class));

        return categories.stream()
                .filter(c -> c.getName().equals("Food"))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    void createTransaction_shouldSucceed_forAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("txn.create@example.com");
        Long categoryId = getFoodCategoryId(token);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(categoryId);
        request.setDescription("Dinner");
        request.setTransactionDate(LocalDate.of(2026, 8, 10));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void createTransaction_shouldReturn400_forNegativeAmount() throws Exception {
        String token = registerAndGetToken("txn.negative@example.com");
        Long categoryId = getFoodCategoryId(token);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("-50.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(categoryId);
        request.setTransactionDate(LocalDate.of(2026, 8, 10));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_shouldReturn404_whenAccessingAnotherUsersTransaction() throws Exception {
        String ownerToken = registerAndGetToken("owner@example.com");
        Long categoryId = getFoodCategoryId(ownerToken);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("300.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(categoryId);
        request.setTransactionDate(LocalDate.of(2026, 8, 10));

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long transactionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        String intruderToken = registerAndGetToken("intruder@example.com");

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransaction_shouldRemoveTransaction_forOwner() throws Exception {
        String token = registerAndGetToken("txn.delete@example.com");
        Long categoryId = getFoodCategoryId(token);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(categoryId);
        request.setTransactionDate(LocalDate.of(2026, 8, 10));

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long transactionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
