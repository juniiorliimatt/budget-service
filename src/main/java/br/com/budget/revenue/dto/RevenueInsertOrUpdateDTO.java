package br.com.budget.revenue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueInsertOrUpdateDTO(
        @NotBlank String name,
        @NotNull @Positive BigDecimal value,
        @NotNull LocalDate date) {
}
