package br.com.budget.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Gera uma receita por mês (competência mensal, tipo receita que se repete todo ano) a
 * partir de um único template, em vez de o client montar os 12 itens na mão como no
 * {@code /batch} genérico. Reaproveita o próprio {@link RevenueDTO} como template —
 * {@code typeId}/{@code value} valem como estão, e {@code date} fornece ano + dia do mês
 * (ajustado/clamp pro último dia de cada mês quando o mês não tiver esse dia, ex.: 31 em
 * fevereiro vira 28/29). {@code id} e {@code referenceDate} do template são ignorados.
 * {@code startMonth} default 1 (janeiro) quando omitido.
 */
public record RevenueAnnualBatchRequestDTO(
        @NotNull @Valid RevenueDTO revenue,
        @Min(1) @Max(12) Integer startMonth) {
}
