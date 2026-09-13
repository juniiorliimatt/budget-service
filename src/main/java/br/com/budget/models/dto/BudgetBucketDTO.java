package br.com.budget.models.dto;

import java.math.BigDecimal;

/**
 * @param target quanto deveria ser gasto nessa fatia (percentual da receita do período)
 * @param actual quanto foi gasto de fato nessa fatia no período
 * @param difference {@code actual - target} — positivo é estouro do orçamento, negativo é sobra
 */
public record BudgetBucketDTO(BigDecimal target, BigDecimal actual, BigDecimal difference) {

    public static BudgetBucketDTO of(BigDecimal target, BigDecimal actual) {
        return new BudgetBucketDTO(target, actual, actual.subtract(target));
    }
}
