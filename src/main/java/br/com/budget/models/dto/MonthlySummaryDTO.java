package br.com.budget.models.dto;

import java.math.BigDecimal;

/**
 * Resumo do mês (competência): {@code totalPending = totalSpending - totalPaid} e
 * {@code projectedBalance = totalRevenue - totalSpending} — a previsão assume que toda
 * despesa em aberto será paga dentro do próprio mês.
 */
public record MonthlySummaryDTO(
        BigDecimal totalRevenue,
        BigDecimal totalSpending,
        BigDecimal totalPaid,
        BigDecimal totalPending,
        BigDecimal projectedBalance) {
}
