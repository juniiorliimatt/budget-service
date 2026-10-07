package br.com.budget.models.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Uma revisão do histórico de {@code Spending} (Hibernate Envers) — ver {@link RevenueRevisionDTO}. */
public record SpendingRevisionDTO(
        int revision,
        LocalDateTime changedAt,
        String changedBy,
        String revisionType,
        UUID id,
        UUID typeId,
        String description,
        BigDecimal value,
        LocalDate date,
        LocalDate referenceDate,
        Boolean wasPaid) {
}
