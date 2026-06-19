package com.tracker.service;

import com.tracker.dto.ExpenseDTO;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserService userService;
    @Mock private CategoryService categoryService;
    @Mock private BudgetRepository budgetRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private ExpenseService expenseService;

    private User user;
    private Category category;
    private Expense expense;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("test@example.com").role("USER").build();
        category = Category.builder().id(1L).name("Food").type(CategoryType.EXPENSE).user(user).build();
        expense = Expense.builder()
                .id(1L).amount(new BigDecimal("150.00"))
                .date(LocalDate.now()).description("Lunch")
                .category(category).user(user).build();
    }

    @Test
    @DisplayName("Should add expense successfully")
    void shouldAddExpenseSuccessfully() {
        ExpenseDTO dto = ExpenseDTO.builder()
                .amount(new BigDecimal("150.00"))
                .date(LocalDate.now())
                .description("Lunch")
                .categoryId(1L)
                .build();

        when(userService.getUserEntity(1L)).thenReturn(user);
        when(categoryService.getCategoryEntity(1L, 1L)).thenReturn(category);
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(budgetRepository.findByUserIdAndMonthAndYear(any(), any(), any())).thenReturn(java.util.List.of());

        ExpenseDTO result = expenseService.addExpense(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("150.00");
        assertThat(result.getCategoryName()).isEqualTo("Food");
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when expense not found")
    void shouldThrowExceptionWhenExpenseNotFound() {
        when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found");
    }

    @Test
    @DisplayName("Should return paginated expenses")
    void shouldReturnPaginatedExpenses() {
        Page<Expense> expensePage = new PageImpl<>(java.util.List.of(expense));
        when(userService.getUserEntity(1L)).thenReturn(user);
        when(expenseRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(expensePage);

        Page<ExpenseDTO> result = expenseService.getExpensesByUserId(1L, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Lunch");
    }
}
