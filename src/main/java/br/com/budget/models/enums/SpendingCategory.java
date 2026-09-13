package br.com.budget.models.enums;

/**
 * Classificação da regra 50/30/20 — pertence ao {@code SpendingType} (o tipo/categoria
 * de despesa, ex.: "Mercado"), não ao lançamento individual: uma vez que o tipo é
 * essencial, todo lançamento daquele tipo é essencial, sem precisar reclassificar cada
 * gasto.
 */
public enum SpendingCategory {

    ESSENTIAL("Essencial"),
    PERSONAL("Pessoal"),
    SAVINGS("Economia");

    private final String label;

    SpendingCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
