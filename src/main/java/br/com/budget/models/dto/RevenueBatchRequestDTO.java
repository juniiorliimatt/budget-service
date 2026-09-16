package br.com.budget.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Lote de receitas pra inserir de uma vez — cada item validado individualmente. */
public record RevenueBatchRequestDTO(
        @NotEmpty @Size(max = 500) @Valid List<RevenueDTO> revenues) {
}
