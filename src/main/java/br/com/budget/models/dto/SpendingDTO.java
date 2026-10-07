package br.com.budget.models.dto;

import br.com.budget.models.entities.Spending;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code typeName} é só leitura — no insert/update quem manda é {@code typeId}.
 * {@code referenceDate} (competência) é opcional no insert/update — se omitido, assume
 * {@code date}. {@code description} é opcional, mas quando informado precisa ter entre 3
 * e 250 caracteres — mesma regra da entidade, validada aqui pra falhar com 422 em vez de
 * estourar exceção de persistência.
 */
public record SpendingDTO(
    UUID id,
    @NotNull UUID typeId,
    String typeName,
    @Size(min = 3, max = 250, message = "{validacao.descricaoTamanho}") String description,
    @NotNull @PositiveOrZero BigDecimal value,
    @NotNull LocalDate date,
    LocalDate referenceDate,
    @NotNull Boolean wasPaid) {

    public static SpendingDTO from(final Spending spending) {
        return new SpendingDTO(spending.getId(), spending.getType().getId(), spending.getType().getName(),
                spending.getDescription(), spending.getValue(), spending.getDate(), spending.getReferenceDate(),
                spending.getWasPaid());
    }
}
