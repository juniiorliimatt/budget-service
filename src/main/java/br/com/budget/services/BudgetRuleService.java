package br.com.budget.services;

import br.com.budget.models.dto.BudgetBucketDTO;
import br.com.budget.models.dto.FiftyThirtyTwentyDTO;
import br.com.budget.models.dto.MonthlySeriesPointDTO;
import br.com.budget.models.dto.MonthlySummaryDTO;
import br.com.budget.models.dto.YearlySummaryDTO;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.enums.SpendingCategory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra 50/30/20: de toda receita do mês, 50% deveria ir pra despesas essenciais, 30%
 * pra despesas pessoais e 20% pra economia. Cada {@code SpendingType} já carrega sua
 * categoria (ver {@link SpendingCategory}), então o gasto real de cada fatia é a soma
 * dos lançamentos cujo tipo pertence àquela categoria no período.
 */
@Service
public class BudgetRuleService {

    private static final BigDecimal ESSENTIAL_PERCENTAGE = new BigDecimal("0.50");
    private static final BigDecimal PERSONAL_PERCENTAGE = new BigDecimal("0.30");
    private static final BigDecimal SAVINGS_PERCENTAGE = new BigDecimal("0.20");

    private final RevenueService revenueService;
    private final SpendingService spendingService;
    private final EntityManager entityManager;

    public BudgetRuleService(final RevenueService revenueService, final SpendingService spendingService,
                              final EntityManager entityManager) {
        this.revenueService = revenueService;
        this.spendingService = spendingService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public FiftyThirtyTwentyDTO cinquentaTrintaVinte(final int month, final int year, final String ownerUsername) {
        final var totalRevenue = revenueService.total(month, year, null, ownerUsername).getTotal();

        final var essentialTarget = totalRevenue.multiply(ESSENTIAL_PERCENTAGE);
        final var personalTarget = totalRevenue.multiply(PERSONAL_PERCENTAGE);
        final var savingsTarget = totalRevenue.multiply(SAVINGS_PERCENTAGE);

        final var essentialActual = sumByCategory(month, year, SpendingCategory.ESSENTIAL, ownerUsername);
        final var personalActual = sumByCategory(month, year, SpendingCategory.PERSONAL, ownerUsername);
        final var savingsActual = sumByCategory(month, year, SpendingCategory.SAVINGS, ownerUsername);

        return new FiftyThirtyTwentyDTO(
                totalRevenue,
                BudgetBucketDTO.of(essentialTarget, essentialActual),
                BudgetBucketDTO.of(personalTarget, personalActual),
                BudgetBucketDTO.of(savingsTarget, savingsActual));
    }

    private BigDecimal sumByCategory(final int month, final int year, final SpendingCategory category,
                                      final String ownerUsername) {
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery(BigDecimal.class);
        final var root = query.from(Spending.class);

        final var from = LocalDate.of(year, month, 1);
        final var to = from.plusMonths(1);

        query.select(cb.coalesce(cb.sum(root.get("value")), BigDecimal.ZERO))
                .where(cb.equal(root.get("ownerUsername"), ownerUsername),
                        cb.equal(root.get("type").get("category"), category),
                        cb.greaterThanOrEqualTo(root.get("referenceDate"), from),
                        cb.lessThan(root.get("referenceDate"), to));

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * {@code totalPending = totalSpending - totalPaid} (evita uma terceira query) e
     * {@code projectedBalance = totalRevenue - totalSpending} — previsão de saldo do
     * mês assumindo que toda despesa em aberto será quitada dentro do próprio mês.
     */
    @Transactional(readOnly = true)
    public MonthlySummaryDTO resumoMensal(final int month, final int year, final String ownerUsername) {
        final var totalRevenue = revenueService.total(month, year, null, ownerUsername).getTotal();
        final var totalSpending = spendingService.total(month, year, null, ownerUsername).getTotal();
        final var totalPaid = sumByPaidStatus(month, year, ownerUsername, true);
        final var totalPending = totalSpending.subtract(totalPaid);
        final var projectedBalance = totalRevenue.subtract(totalSpending);

        return new MonthlySummaryDTO(totalRevenue, totalSpending, totalPaid, totalPending, projectedBalance);
    }

    /** Resumo do ano inteiro (competência) — base da tela de metas. */
    @Transactional(readOnly = true)
    public YearlySummaryDTO resumoAnual(final int year, final String ownerUsername) {
        final var totalRevenue = revenueService.total(null, year, null, ownerUsername).getTotal();
        final var totalSpending = spendingService.total(null, year, null, ownerUsername).getTotal();
        return new YearlySummaryDTO(totalRevenue, totalSpending, totalRevenue.subtract(totalSpending));
    }

    /**
     * Série do ano (12 pontos, sempre todos os meses, zeros onde não há lançamento) em duas
     * queries agrupadas — substitui 12 chamadas de {@link #cinquentaTrintaVinte}. Mesmas regras
     * dele: competência ({@code referenceDate}), dono e {@code includeInMonthlyTotals} nas receitas.
     */
    @Transactional(readOnly = true)
    public List<MonthlySeriesPointDTO> serieMensal(final int year, final String ownerUsername) {
        final var from = LocalDate.of(year, 1, 1);
        final var to = from.plusYears(1);

        final BigDecimal[] revenue = zeros();
        final List<Object[]> revenueRows = entityManager.createQuery(
                        "select extract(month from r.referenceDate), sum(r.value) from Revenue r "
                                + "where r.ownerUsername = :owner and r.referenceDate >= :from and r.referenceDate < :to "
                                + "and r.type.includeInMonthlyTotals = true "
                                + "group by extract(month from r.referenceDate)", Object[].class)
                .setParameter("owner", ownerUsername).setParameter("from", from).setParameter("to", to)
                .getResultList();
        for (final Object[] row : revenueRows) {
            revenue[((Number) row[0]).intValue() - 1] = (BigDecimal) row[1];
        }

        final var byCategory = new java.util.EnumMap<SpendingCategory, BigDecimal[]>(SpendingCategory.class);
        for (final SpendingCategory category : SpendingCategory.values()) {
            byCategory.put(category, zeros());
        }
        final List<Object[]> spendingRows = entityManager.createQuery(
                        "select extract(month from s.referenceDate), s.type.category, sum(s.value) from Spending s "
                                + "where s.ownerUsername = :owner and s.referenceDate >= :from and s.referenceDate < :to "
                                + "group by extract(month from s.referenceDate), s.type.category", Object[].class)
                .setParameter("owner", ownerUsername).setParameter("from", from).setParameter("to", to)
                .getResultList();
        for (final Object[] row : spendingRows) {
            byCategory.get((SpendingCategory) row[1])[((Number) row[0]).intValue() - 1] = (BigDecimal) row[2];
        }

        final List<MonthlySeriesPointDTO> series = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            series.add(new MonthlySeriesPointDTO(i + 1, revenue[i],
                    byCategory.get(SpendingCategory.ESSENTIAL)[i],
                    byCategory.get(SpendingCategory.PERSONAL)[i],
                    byCategory.get(SpendingCategory.SAVINGS)[i]));
        }
        return series;
    }

    private static BigDecimal[] zeros() {
        final BigDecimal[] values = new BigDecimal[12];
        java.util.Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private BigDecimal sumByPaidStatus(final int month, final int year, final String ownerUsername, final boolean wasPaid) {
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery(BigDecimal.class);
        final var root = query.from(Spending.class);

        final var from = LocalDate.of(year, month, 1);
        final var to = from.plusMonths(1);

        query.select(cb.coalesce(cb.sum(root.get("value")), BigDecimal.ZERO))
                .where(cb.equal(root.get("ownerUsername"), ownerUsername),
                        cb.equal(root.get("wasPaid"), wasPaid),
                        cb.greaterThanOrEqualTo(root.get("referenceDate"), from),
                        cb.lessThan(root.get("referenceDate"), to));

        return entityManager.createQuery(query).getSingleResult();
    }
}
