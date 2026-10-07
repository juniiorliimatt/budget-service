package br.com.budget.config;

/** Nomes dos caches do Spring (Caffeine) — ver {@link CacheConfig}. */
public final class CacheNames {

    /** Leituras agregadas (totais, por tipo, regra 50/30/20, resumos e série): derivam de receitas, despesas e tipos. */
    public static final String AGREGADOS = "budget-aggregates";

    /** Catálogos de tipos de receita/despesa (leitura frequente, muda raramente). */
    public static final String TIPOS = "budget-types";

    private CacheNames() {
    }
}
