package br.com.budget.models.dto;

import java.math.BigDecimal;

/** Resumo do ano: {@code balance = totalRevenue - totalSpending}. */
public record YearlySummaryDTO(BigDecimal totalRevenue, BigDecimal totalSpending, BigDecimal balance) {
}
