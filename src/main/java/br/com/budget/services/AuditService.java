package br.com.budget.services;

import br.com.budget.config.audit.CustomRevisionEntity;
import br.com.budget.models.dto.RevenueRevisionDTO;
import br.com.budget.models.dto.RevenueTypeRevisionDTO;
import br.com.budget.models.dto.SpendingRevisionDTO;
import br.com.budget.models.dto.SpendingTypeRevisionDTO;
import br.com.budget.models.entities.Revenue;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.entities.SpendingType;
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
 * em {@link Revenue}/{@link Spending}/{@link RevenueType}/{@link SpendingType}) — mesmo
 * padrão do {@code AuditService} do workbox-api. Diferente de lá (onde {@code /audit/**}
 * é ADMIN-only, pois é histórico de outros usuários), aqui Revenue/Spending pertencem a
 * um dono, então o histórico só é liberado pro próprio dono -
 * {@code revenueService.findById}/{@code spendingService.findById} fazem essa checagem
 * (404, não 403, se não for o dono) antes de consultar o Envers. RevenueType/SpendingType
 * são catálogos globais (sem dono) - histórico liberado pra qualquer usuário autenticado,
 * só checando que o tipo existe.
 */
@Service
public class AuditService {

    private final EntityManager entityManager;
    private final RevenueService revenueService;
    private final SpendingService spendingService;
    private final RevenueTypeService revenueTypeService;
    private final SpendingTypeService spendingTypeService;

    public AuditService(final EntityManager entityManager, final RevenueService revenueService,
                         final SpendingService spendingService, final RevenueTypeService revenueTypeService,
                         final SpendingTypeService spendingTypeService) {
        this.entityManager = entityManager;
        this.revenueService = revenueService;
        this.spendingService = spendingService;
        this.revenueTypeService = revenueTypeService;
        this.spendingTypeService = spendingTypeService;
    }

    /** {@code revenueService.findById} não é descartável: é a checagem de dono/existência (404 se falhar) antes de consultar o Envers. */
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

    /** {@code spendingService.findById} não é descartável: é a checagem de dono/existência (404 se falhar) antes de consultar o Envers. */
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

    /** {@code revenueTypeService.findById} só existe pra dar 404 antes de consultar o Envers — catálogo global, sem checagem de dono. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<RevenueTypeRevisionDTO> findRevenueTypeHistory(final UUID id) {
        revenueTypeService.findById(id);

        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(RevenueType.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    final var snapshot = (RevenueType) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new RevenueTypeRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getName(),
                            snapshot.getIncludeInTotals(),
                            snapshot.getIncludeInMonthlyTotals());
                })
                .toList();
    }

    /** {@code spendingTypeService.findById} só existe pra dar 404 antes de consultar o Envers — catálogo global, sem checagem de dono. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SpendingTypeRevisionDTO> findSpendingTypeHistory(final UUID id) {
        spendingTypeService.findById(id);

        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(SpendingType.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    final var snapshot = (SpendingType) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new SpendingTypeRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getName(),
                            snapshot.getCategory());
                })
                .toList();
    }

    private LocalDateTime toLocalDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
