package com.example.finance.service;

import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Category category;
    private TransactionRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("jane@example.com").build();
        category = Category.builder().id(10L).name("Food").user(user).build();

        request = new TransactionRequest();
        request.setAmount(new BigDecimal("250.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(10L);
        request.setDescription("Groceries");
        request.setTransactionDate(LocalDate.of(2026, 8, 5));
    }

    @Test
    void createTransaction_shouldPersistAndReturnResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryService.getCategoryEntity(1L, 10L)).thenReturn(category);

        Transaction saved = Transaction.builder()
                .id(100L)
                .amount(request.getAmount())
                .type(request.getType())
                .category(category)
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .user(user)
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(1L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getCategoryName()).isEqualTo("Food");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void updateTransaction_shouldThrow_whenTransactionNotOwnedByUser() {
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, 100L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_shouldRemoveTransaction_whenOwnedByUser() {
        Transaction existing = Transaction.builder().id(100L).user(user).category(category).build();
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));

        transactionService.deleteTransaction(1L, 100L);

        verify(transactionRepository).delete(existing);
    }

    @Test
    void deleteTransaction_shouldThrow_whenNotFoundForUser() {
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTransaction_shouldReturnTransaction_whenOwnedByUser() {
        Transaction existing = Transaction.builder()
                .id(100L)
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.INCOME)
                .category(category)
                .user(user)
                .transactionDate(LocalDate.now())
                .build();
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));

        TransactionResponse response = transactionService.getTransaction(1L, 100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
    }
}
