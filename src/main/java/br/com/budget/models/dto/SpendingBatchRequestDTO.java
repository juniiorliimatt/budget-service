package br.com.budget.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Lote de despesas pra inserir de uma vez — cada item validado individualmente. */
public record SpendingBatchRequestDTO(
        @NotEmpty @Size(max = 500) @Valid List<SpendingDTO> spendings) {
}
