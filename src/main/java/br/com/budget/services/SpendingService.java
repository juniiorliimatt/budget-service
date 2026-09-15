package br.com.budget.services;

import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.SpendingAnnualBatchRequestDTO;
import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.entities.SpendingType;
import br.com.budget.repositories.SpendingRepository;
import br.com.budget.repositories.SpendingTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca de despesas é sempre por mês/ano + tipo (ambos opcionais, combináveis), sempre
 * restrita ao {@code ownerUsername} do usuário autenticado — mesmo padrão de
 * {@link RevenueService}: intervalo de datas em vez de função de banco tipo
 * {@code MONTH()}/{@code EXTRACT()}, sargável e portável entre H2/Postgres.
 */
@Service
public class SpendingService {

  private static final String SPENDING_NOT_FOUND = "Spending not found";
  private static final String SPENDING_TYPE_NOT_FOUND = "Spending type not found";

  private final SpendingRepository spendingRepository;
  private final SpendingTypeRepository spendingTypeRepository;
  private final EntityManager entityManager;

  public SpendingService(final SpendingRepository spendingRepository,
                          final SpendingTypeRepository spendingTypeRepository,
                          final EntityManager entityManager) {
    this.spendingRepository = spendingRepository;
    this.spendingTypeRepository = spendingTypeRepository;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public Page<SpendingDTO> search(final Integer month, final Integer year, final UUID typeId,
                                   final String ownerUsername, final Pageable pageable) {
    final Specification<Spending> spec = (root, query, cb) ->
            cb.and(buildPredicates(root, cb, month, year, typeId, ownerUsername).toArray(new Predicate[0]));
    return spendingRepository.findAll(spec, pageable).map(SpendingDTO::from);
  }

  @Transactional(readOnly = true)
  public SpendingDTO findById(final UUID id, final String ownerUsername) {
    return SpendingDTO.from(findEntityById(id, ownerUsername));
  }

  @Transactional
  public SpendingDTO save(final SpendingDTO dto, final String ownerUsername) {
    final var spending = Spending.builder()
            .type(requireType(dto.typeId()))
            .description(dto.description())
            .value(dto.value())
            .date(dto.date())
            .referenceDate(resolveReferenceDate(dto))
            .wasPaid(dto.wasPaid())
            .ownerUsername(ownerUsername)
            .build();
    return SpendingDTO.from(spendingRepository.save(spending));
  }

  @Transactional
  public SpendingDTO update(final UUID id, final SpendingDTO dto, final String ownerUsername) {
    final var spending = findEntityById(id, ownerUsername);
    spending.setType(requireType(dto.typeId()));
    spending.setDescription(dto.description());
    spending.setValue(dto.value());
    spending.setDate(dto.date());
    spending.setReferenceDate(resolveReferenceDate(dto));
    spending.setWasPaid(dto.wasPaid());
    return SpendingDTO.from(spendingRepository.save(spending));
  }

  /**
   * Insere o lote inteiro numa única transação — se um item falhar (ex.: typeId
   * inexistente), nenhum é persistido. Chamada a {@link #save} aqui é invocação
   * direta (mesma instância), não passa pelo proxy do Spring, então quem garante a
   * atomicidade é a transação desta própria chamada, não a de {@code save}.
   */
  @Transactional
  public List<SpendingDTO> saveAll(final List<SpendingDTO> dtos, final String ownerUsername) {
    return dtos.stream().map(dto -> save(dto, ownerUsername)).toList();
  }

  /**
   * Replica um template de despesa recorrente (luz, gás, internet) por todos os meses do
   * ano a partir de {@code startMonth} (default janeiro). Reaproveita {@link #saveAll}
   * pra manter a mesma atomicidade e validação de tipo do lote genérico.
   */
  @Transactional
  public List<SpendingDTO> saveAnnual(final SpendingAnnualBatchRequestDTO request, final String ownerUsername) {
    final var startMonth = request.startMonth() != null ? request.startMonth() : 1;
    final var dtos = IntStream.rangeClosed(startMonth, 12)
            .mapToObj(month -> {
              final var yearMonth = YearMonth.of(request.year(), month);
              final var day = Math.min(request.dayOfMonth(), yearMonth.lengthOfMonth());
              final var date = yearMonth.atDay(day);
              return new SpendingDTO(null, request.typeId(), null, request.description(), request.value(), date,
                      null, request.wasPaid());
            })
            .toList();
    return saveAll(dtos, ownerUsername);
  }

  /** Competência default = {@code date} quando o client não informa {@code referenceDate}. */
  private LocalDate resolveReferenceDate(final SpendingDTO dto) {
    return dto.referenceDate() != null ? dto.referenceDate() : dto.date();
  }

  @Transactional
  public void delete(final UUID id, final String ownerUsername) {
    spendingRepository.delete(findEntityById(id, ownerUsername));
  }

  /** Total por mês/ano e/ou tipo (mesmos filtros de {@link #search}, ambos opcionais). */
  @Transactional(readOnly = true)
  public TotalDTO total(final Integer month, final Integer year, final UUID typeId, final String ownerUsername) {
    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    final var query = cb.createQuery(BigDecimal.class);
    final Root<Spending> root = query.from(Spending.class);
    final var predicates = buildPredicates(root, cb, month, year, typeId, ownerUsername);
    query.select(cb.coalesce(cb.sum(root.get("value")), BigDecimal.ZERO));
    query.where(predicates.toArray(new Predicate[0]));
    return new TotalDTO(entityManager.createQuery(query).getSingleResult());
  }

  private List<Predicate> buildPredicates(final Root<Spending> root, final CriteriaBuilder cb,
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
    final var root = query.from(Spending.class);
    final var type = root.join("type");

    final var from = LocalDate.of(year, 1, 1);
    final var to = from.plusYears(1);
    final var total = cb.sum(root.get("value"));

    query.multiselect(type.get("id"), type.get("name"), total)
            .where(cb.equal(root.get("ownerUsername"), ownerUsername),
                    cb.greaterThanOrEqualTo(root.get("referenceDate"), from),
                    cb.lessThan(root.get("referenceDate"), to))
            .groupBy(type.get("id"), type.get("name"))
            .orderBy(cb.desc(total));

    return entityManager.createQuery(query).getResultList().stream()
            .map(row -> new TypeTotalDTO((UUID) row[0], (String) row[1], (BigDecimal) row[2]))
            .toList();
  }

  private Spending findEntityById(final UUID id, final String ownerUsername) {
    return spendingRepository.findByIdAndOwnerUsername(id, ownerUsername)
            .orElseThrow(() -> new ResourceNotFoundException(SPENDING_NOT_FOUND));
  }

  private SpendingType requireType(final UUID typeId) {
    return spendingTypeRepository.findById(typeId).orElseThrow(() -> new ResourceNotFoundException(SPENDING_TYPE_NOT_FOUND));
  }
}
