package br.com.budget.models.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma revisão do histórico de {@code RevenueType} (Hibernate Envers) — ver {@link RevenueRevisionDTO}. */
public record RevenueTypeRevisionDTO(
        int revision,
        LocalDateTime changedAt,
        String changedBy,
        String revisionType,
        UUID id,
        String name,
        Boolean includeInTotals,
        Boolean includeInMonthlyTotals) {
}
