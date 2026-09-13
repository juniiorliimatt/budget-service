package br.com.budget.models.dto;

import br.com.budget.models.entities.RevenueType;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * {@code includeInTotals} é opcional no insert/update — se omitido, assume {@code true}.
 * Quando {@code false}, o tipo some da soma agrupada por tipo (endpoints
 * {@code /revenues/by-type}), mas continua valendo pro total geral e pro CRUD normal.
 */
public record RevenueTypeDTO(UUID id, @NotBlank String name, Boolean includeInTotals) {
    public static RevenueTypeDTO from(RevenueType entity) {
        return new RevenueTypeDTO(entity.getId(), entity.getName(), entity.getIncludeInTotals());
    }
}
