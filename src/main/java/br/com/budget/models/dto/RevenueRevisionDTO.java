package br.com.budget.models.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma revisão do histórico de {@code Revenue} (Hibernate Envers) — mesmo padrão do
 * {@code UserApiRevisionDTO} do workbox-api. {@code typeId} é o id do tipo no momento
 * daquela revisão (não resolve o nome atual do tipo, que pode ter mudado ou sido
 * apagado desde então). {@code changedBy} vem do {@code SecurityContext} no momento da
 * mudança.
 */
public record RevenueRevisionDTO(
        int revision,
        LocalDateTime changedAt,
        String changedBy,
        String revisionType,
        UUID id,
        UUID typeId,
        BigDecimal value,
        LocalDate date,
        LocalDate referenceDate) {
}
