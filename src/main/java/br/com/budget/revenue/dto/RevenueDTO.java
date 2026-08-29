package br.com.budget.revenue.dto;

import br.com.budget.revenue.entities.Revenue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RevenueDTO(UUID id, String name, BigDecimal value, LocalDate date) {

    public static RevenueDTO from(Revenue revenue) {
        return new RevenueDTO(revenue.getId(), revenue.getName(), revenue.getValue(), revenue.getDate());
    }
}
