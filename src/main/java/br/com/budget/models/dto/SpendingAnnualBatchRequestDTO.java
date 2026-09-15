package br.com.budget.models.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gera uma despesa por mês (competência mensal, tipo despesa que se repete todo ano —
 * luz, gás, internet) a partir de um único template, em vez de o client montar os 12
 * itens na mão como no {@code /batch} genérico. {@code startMonth} default 1 (janeiro)
 * quando omitido. {@code dayOfMonth} é ajustado (clamp) pro último dia de cada mês
 * quando o mês não tiver esse dia (ex.: 31 em fevereiro vira 28/29).
 */
public record SpendingAnnualBatchRequestDTO(
        @NotNull UUID typeId,
        @Size(min = 3, max = 250, message = "size must be between 3 and 250") String description,
        @NotNull @PositiveOrZero BigDecimal value,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @Min(1) @Max(12) Integer startMonth,
        @NotNull @Min(1) @Max(31) Integer dayOfMonth,
        @NotNull Boolean wasPaid) {
}
