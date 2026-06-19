package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseDTO {
    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
