package br.com.budget.services;

import br.com.budget.config.audit.CustomRevisionEntity;
import br.com.budget.models.dto.RevenueRevisionDTO;
import br.com.budget.models.dto.SpendingRevisionDTO;
import br.com.budget.models.entities.Revenue;
import br.com.budget.models.entities.Spending;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura do histórico de revisões gravado pelo Hibernate Envers (ver {@code @Audited}
 * em {@link Revenue}/{@link Spending}) — mesmo padrão do {@code AuditService} do
 * workbox-api. Diferente de lá (onde {@code /audit/**} é ADMIN-only, pois é histórico de
 * outros usuários), aqui cada lançamento pertence a um dono, então o histórico só é
 * liberado pro próprio dono - {@code revenueService.findById}/{@code spendingService.findById}
 * fazem essa checagem (404, não 403, se não for o dono) antes de consultar o Envers.
 */
@Service
public class AuditService {

    private final EntityManager entityManager;
    private final RevenueService revenueService;
    private final SpendingService spendingService;

    public AuditService(final EntityManager entityManager, final RevenueService revenueService,
                         final SpendingService spendingService) {
        this.entityManager = entityManager;
        this.revenueService = revenueService;
        this.spendingService = spendingService;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<RevenueRevisionDTO> findRevenueHistory(final UUID id, final String ownerUsername) {
        revenueService.findById(id, ownerUsername);

        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Revenue.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    final var snapshot = (Revenue) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new RevenueRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getType().getId(),
                            snapshot.getValue(),
                            snapshot.getDate(),
                            snapshot.getReferenceDate());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SpendingRevisionDTO> findSpendingHistory(final UUID id, final String ownerUsername) {
        spendingService.findById(id, ownerUsername);

        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Spending.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    final var snapshot = (Spending) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new SpendingRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getType().getId(),
                            snapshot.getDescription(),
                            snapshot.getValue(),
                            snapshot.getDate(),
                            snapshot.getReferenceDate(),
                            snapshot.getWasPaid());
                })
                .toList();
    }

    private LocalDateTime toLocalDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
