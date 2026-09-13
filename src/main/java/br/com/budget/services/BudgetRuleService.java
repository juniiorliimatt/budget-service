package br.com.budget.services;

import br.com.budget.models.dto.BudgetBucketDTO;
import br.com.budget.models.dto.FiftyThirtyTwentyDTO;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.enums.SpendingCategory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final EntityManager entityManager;

    public BudgetRuleService(final RevenueService revenueService, final EntityManager entityManager) {
        this.revenueService = revenueService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public FiftyThirtyTwentyDTO fiftyThirtyTwenty(final int month, final int year, final String ownerUsername) {
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
}
