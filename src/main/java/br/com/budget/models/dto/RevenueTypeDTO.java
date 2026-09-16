package br.com.budget.models.dto;

import br.com.budget.models.entities.RevenueType;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * {@code includeInTotals}/{@code includeInMonthlyTotals} são opcionais no insert/update
 * — se omitidos, assumem {@code true}. {@code includeInTotals=false} tira o tipo do
 * total anual "de tudo" e do agrupamento por tipo ({@code /revenues/by-type}). Ex.:
 * "Caixinha" (sobra de salário do mês anterior, já contada no "Salário" original).
 * {@code includeInMonthlyTotals=false} tira do total mensal "de tudo" (resumo mensal,
 * regra 50/30/20). Ex.: saldo de dezembro lançado em janeiro pra fechar o ano. Nenhum
 * dos dois afeta o total de um tipo específico nem o CRUD normal.
 */
public record RevenueTypeDTO(UUID id, @NotBlank String name, Boolean includeInTotals, Boolean includeInMonthlyTotals) {
    public static RevenueTypeDTO from(final RevenueType entity) {
        return new RevenueTypeDTO(entity.getId(), entity.getName(), entity.getIncludeInTotals(), entity.getIncludeInMonthlyTotals());
    }
}
