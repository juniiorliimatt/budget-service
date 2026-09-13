package br.com.budget.services;

import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.models.entities.Revenue;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.repositories.RevenueRepository;
import br.com.budget.repositories.RevenueTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca de receitas é sempre por mês/ano + tipo (ambos opcionais, combináveis), sempre
 * restrita ao {@code ownerUsername} do usuário autenticado — cada Revenue pertence a um
 * único dono, nunca compartilhado entre usuários. Mês/ano vira um intervalo
 * {@code [primeiro dia do mês, primeiro dia do mês seguinte)} — comparação simples de
 * coluna (sargável, portável entre H2/Postgres), evitando funções tipo
 * {@code MONTH()}/{@code EXTRACT()} que ou não existem em algum dos dois bancos ou
 * impedem o uso de índice.
 */
@Service
public class RevenueService {

    private static final String REVENUE_NOT_FOUND = "Revenue not found";
    private static final String REVENUE_TYPE_NOT_FOUND = "Revenue type not found";

    private final RevenueRepository revenueRepository;
    private final RevenueTypeRepository revenueTypeRepository;
    private final EntityManager entityManager;

    public RevenueService(final RevenueRepository revenueRepository,
                           final RevenueTypeRepository revenueTypeRepository,
                           final EntityManager entityManager) {
        this.revenueRepository = revenueRepository;
        this.revenueTypeRepository = revenueTypeRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<RevenueDTO> search(final Integer month, final Integer year, final UUID typeId,
                                    final String ownerUsername, final Pageable pageable) {
        final Specification<Revenue> spec = (root, query, cb) ->
                cb.and(buildPredicates(root, cb, month, year, typeId, ownerUsername).toArray(new Predicate[0]));
        return revenueRepository.findAll(spec, pageable).map(RevenueDTO::from);
    }

    @Transactional(readOnly = true)
    public RevenueDTO findById(final UUID id, final String ownerUsername) {
        return RevenueDTO.from(findEntityById(id, ownerUsername));
    }

    @Transactional
    public RevenueDTO save(final RevenueDTO dto, final String ownerUsername) {
        final var revenue = Revenue.builder()
                .type(requireType(dto.typeId()))
                .value(dto.value())
                .date(dto.date())
                .referenceDate(resolveReferenceDate(dto))
                .ownerUsername(ownerUsername)
                .build();
        return RevenueDTO.from(revenueRepository.save(revenue));
    }

    @Transactional
    public RevenueDTO update(final UUID id, final RevenueDTO dto, final String ownerUsername) {
        final var revenue = findEntityById(id, ownerUsername);
        revenue.setType(requireType(dto.typeId()));
        revenue.setValue(dto.value());
        revenue.setDate(dto.date());
        revenue.setReferenceDate(resolveReferenceDate(dto));
        return RevenueDTO.from(revenueRepository.save(revenue));
    }

    /**
     * Insere o lote inteiro numa única transação — se um item falhar (ex.: typeId
     * inexistente), nenhum é persistido. Chamada a {@link #save} aqui é invocação
     * direta (mesma instância), não passa pelo proxy do Spring, então quem garante a
     * atomicidade é a transação desta própria chamada, não a de {@code save}.
     */
    @Transactional
    public List<RevenueDTO> saveAll(final List<RevenueDTO> dtos, final String ownerUsername) {
        return dtos.stream().map(dto -> save(dto, ownerUsername)).toList();
    }

    /** Competência default = {@code date} quando o client não informa {@code referenceDate}. */
    private LocalDate resolveReferenceDate(final RevenueDTO dto) {
        return dto.referenceDate() != null ? dto.referenceDate() : dto.date();
    }

    @Transactional
    public void delete(final UUID id, final String ownerUsername) {
        revenueRepository.delete(findEntityById(id, ownerUsername));
    }

    /** Total por mês/ano e/ou tipo (mesmos filtros de {@link #search}, ambos opcionais). */
    @Transactional(readOnly = true)
    public TotalDTO total(final Integer month, final Integer year, final UUID typeId, final String ownerUsername) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery(BigDecimal.class);
        final Root<Revenue> root = query.from(Revenue.class);
        final var predicates = buildPredicates(root, cb, month, year, typeId, ownerUsername);
        query.select(cb.coalesce(cb.sum(root.get("value")), BigDecimal.ZERO));
        query.where(predicates.toArray(new Predicate[0]));
        return new TotalDTO(entityManager.createQuery(query).getSingleResult());
    }

    private List<Predicate> buildPredicates(final Root<Revenue> root, final CriteriaBuilder cb,
                                             final Integer month, final Integer year, final UUID typeId,
                                             final String ownerUsername) {
        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("ownerUsername"), ownerUsername));
        if (month != null && year != null) {
            final var from = LocalDate.of(year, month, 1);
            final var to = from.plusMonths(1);
            predicates.add(cb.greaterThanOrEqualTo(root.get("referenceDate"), from));
            predicates.add(cb.lessThan(root.get("referenceDate"), to));
        } else if (year != null) {
            final var from = LocalDate.of(year, 1, 1);
            final var to = from.plusYears(1);
            predicates.add(cb.greaterThanOrEqualTo(root.get("referenceDate"), from));
            predicates.add(cb.lessThan(root.get("referenceDate"), to));
        }
        if (typeId != null) {
            predicates.add(cb.equal(root.get("type").get("id"), typeId));
        }
        return predicates;
    }

    /** Soma agrupada por tipo no ano inteiro (competência) — base da tela de metas. */
    @Transactional(readOnly = true)
    public List<TypeTotalDTO> totalByType(final int year, final String ownerUsername) {
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery(Object[].class);
        final var root = query.from(Revenue.class);
        final var type = root.join("type");

        final var from = LocalDate.of(year, 1, 1);
        final var to = from.plusYears(1);

        query.multiselect(type.get("id"), type.get("name"), cb.sum(root.get("value")))
                .where(cb.equal(root.get("ownerUsername"), ownerUsername),
                        cb.greaterThanOrEqualTo(root.get("referenceDate"), from),
                        cb.lessThan(root.get("referenceDate"), to))
                .groupBy(type.get("id"), type.get("name"))
                .orderBy(cb.asc(type.get("name")));

        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new TypeTotalDTO((UUID) row[0], (String) row[1], (BigDecimal) row[2]))
                .toList();
    }

    private Revenue findEntityById(final UUID id, final String ownerUsername) {
        return revenueRepository.findByIdAndOwnerUsername(id, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException(REVENUE_NOT_FOUND));
    }

    private RevenueType requireType(final UUID typeId) {
        return revenueTypeRepository.findById(typeId).orElseThrow(() -> new ResourceNotFoundException(REVENUE_TYPE_NOT_FOUND));
    }
}
