package br.com.budget.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * Lote de receitas recorrentes — cada item de {@code revenues} é replicado por mês em vez
 * de inserido uma única vez, como no {@code /batch} genérico. {@code months} seleciona os
 * meses de destino (default: todos os 12, quando omitido ou vazio). Ano + dia do mês de
 * cada réplica vêm do próprio {@code date} do item (ajustado — clamp — pro último dia de
 * cada mês quando o mês não tiver esse dia, ex.: 31 em fevereiro vira 28/29); {@code id} e
 * {@code referenceDate} de cada item são ignorados.
 */
public record RevenueAnnualBatchRequestDTO(
        @NotEmpty @Size(max = 500) @Valid List<RevenueDTO> revenues,
        Set<@Min(1) @Max(12) Integer> months) {
}
