package br.com.budget.models.dto;

import br.com.budget.models.entities.Spending;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code typeName} é só leitura — no insert/update quem manda é {@code typeId}.
 * {@code referenceDate} (competência) é opcional no insert/update — se omitido, assume
 * {@code date}.
 */
public record SpendingDTO(
    UUID id,
    @NotNull UUID typeId,
    String typeName,
    String description,
    @NotNull @Positive BigDecimal value,
    @NotNull LocalDate date,
    LocalDate referenceDate,
    @NotNull Boolean wasPaid) {

    public static SpendingDTO from(Spending spending) {
        return new SpendingDTO(spending.getId(), spending.getType().getId(), spending.getType().getName(),
                spending.getDescription(), spending.getValue(), spending.getDate(), spending.getReferenceDate(),
                spending.getWasPaid());
    }
}
