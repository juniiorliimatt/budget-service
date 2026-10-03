package br.com.budget.models.dto;

import java.math.BigDecimal;

/**
 * Um mês da série anual pela regra 50/30/20 — valores <b>realizados</b> (não metas). Mesmos
 * números que {@code fifty-thirty-twenty} devolve pra aquele mês: {@code totalRevenue} respeita
 * {@code includeInMonthlyTotals} e as despesas são agrupadas pela categoria do tipo.
 *
 * @param month 1 a 12
 */
public record MonthlySeriesPointDTO(int month,
                                    BigDecimal totalRevenue,
                                    BigDecimal essential,
                                    BigDecimal personal,
                                    BigDecimal savings) {
}
