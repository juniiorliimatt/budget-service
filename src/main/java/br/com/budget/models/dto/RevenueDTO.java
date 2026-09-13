package br.com.budget.models.dto;

import br.com.budget.models.entities.Revenue;

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
public record RevenueDTO(
    UUID id,
    @NotNull UUID typeId,
    String typeName,
    @NotNull @Positive BigDecimal value,
    @NotNull LocalDate date,
    LocalDate referenceDate) {

    public static RevenueDTO from(Revenue revenue) {
        return new RevenueDTO(revenue.getId(), revenue.getType().getId(), revenue.getType().getName(),
                revenue.getValue(), revenue.getDate(), revenue.getReferenceDate());
    }
}
