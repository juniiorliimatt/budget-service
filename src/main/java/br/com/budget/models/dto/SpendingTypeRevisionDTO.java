package br.com.budget.models.dto;

import br.com.budget.models.enums.SpendingCategory;
import java.time.LocalDateTime;
import java.util.UUID;

/** Uma revisão do histórico de {@code SpendingType} (Hibernate Envers) — ver {@link RevenueRevisionDTO}. */
public record SpendingTypeRevisionDTO(
        int revision,
        LocalDateTime changedAt,
        String changedBy,
        String revisionType,
        UUID id,
        String name,
        SpendingCategory category) {
}
