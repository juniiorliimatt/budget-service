package br.com.budget.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.budget.services.RevenueService;
import br.com.budget.services.SpendingService;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A chave default do Spring ignora método e classe — receita e despesa com os mesmos parâmetros colidiriam. */
class BudgetKeyGeneratorTest {

    private final BudgetKeyGenerator generator = new BudgetKeyGenerator();

    private static Method method(final Class<?> type, final String name) {
        for (final Method candidate : type.getMethods()) {
            if (candidate.getName().equals(name)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Test
    void sameMethodAndSameParameters_produceEqualKeys() {
        final var total = method(RevenueService.class, "total");

        final var a = generator.generate(null, total, 3, 2026, null, "qa@x");
        final var b = generator.generate(null, total, 3, 2026, null, "qa@x");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void differentOwner_orDifferentParameter_produceDifferentKeys() {
        final var total = method(RevenueService.class, "total");
        final var typeId = UUID.randomUUID();

        final var base = generator.generate(null, total, 3, 2026, null, "qa@x");

        assertThat(generator.generate(null, total, 3, 2026, null, "outro@x")).isNotEqualTo(base);
        assertThat(generator.generate(null, total, 4, 2026, null, "qa@x")).isNotEqualTo(base);
        assertThat(generator.generate(null, total, 3, 2026, typeId, "qa@x")).isNotEqualTo(base);
    }

    @Test
    void sameParametersOnDifferentServices_produceDifferentKeys() {
        final var revenue = generator.generate(null, method(RevenueService.class, "total"), 3, 2026, null, "qa@x");
        final var spending = generator.generate(null, method(SpendingService.class, "total"), 3, 2026, null, "qa@x");

        assertThat(revenue).isNotEqualTo(spending);
    }

    @Test
    void sameParametersOnDifferentMethodsOfTheSameService_produceDifferentKeys() {
        final var total = generator.generate(null, method(RevenueService.class, "total"), 3, 2026, null, "qa@x");
        final var byType = generator.generate(null, method(RevenueService.class, "totalPorTipo"), 3, 2026, null, "qa@x");

        assertThat(total).isNotEqualTo(byType);
    }

    @Test
    void noParameters_stillIncludesTheMethod() {
        final var a = generator.generate(null, method(RevenueService.class, "buscar"));
        final var b = generator.generate(null, method(SpendingService.class, "buscar"));

        assertThat(a).isNotEqualTo(b);
    }
}
