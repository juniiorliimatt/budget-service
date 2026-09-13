package br.com.budget.models.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Soma agregada de um tipo de receita/despesa num período (ex.: total de "Condomínio" no ano). */
public record TypeTotalDTO(UUID typeId, String typeName, BigDecimal total) {
}
