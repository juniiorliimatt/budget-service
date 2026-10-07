package br.com.budget.models.dto;

import java.math.BigDecimal;

public record FiftyThirtyTwentyDTO(
        BigDecimal totalRevenue,
        BudgetBucketDTO essential,
        BudgetBucketDTO personal,
        BudgetBucketDTO savings) {
}
