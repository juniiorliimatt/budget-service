package br.com.budget;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.models.entities.Revenue;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.entities.SpendingType;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.RevenueService;
import br.com.budget.services.SpendingService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * {@code totalPorTipo} (soma agrupada por tipo) contra Postgres real: filtro por ano, por mês
 * (competência, não data), por dono e pelas flags de {@link RevenueType}. Exige Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class TotalPorTipoIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("workbox")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/workbox"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "budget_service");
        registry.add("spring.datasource.password", () -> "budget_service");
    }

    private static final String OWNER = "qa.user@workbox.local";
    private static final String OTHER = "outro@workbox.local";

    @Autowired
    private RevenueService revenueService;

    @Autowired
    private SpendingService spendingService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate tx;

    private RevenueType revenueType(final String name, final boolean inTotals, final boolean inMonthly) {
        return tx.execute(status -> {
            final var type = RevenueType.builder().name(name + UUID.randomUUID().toString().substring(0, 6))
                    .includeInTotals(inTotals).includeInMonthlyTotals(inMonthly).build();
            entityManager.persist(type);
            return type;
        });
    }

    private void revenue(final RevenueType type, final String owner, final String value, final LocalDate date, final LocalDate reference) {
        tx.executeWithoutResult(status -> entityManager.persist(Revenue.builder().type(entityManager.find(RevenueType.class, type.getId()))
                .value(new BigDecimal(value)).date(date).referenceDate(reference).ownerUsername(owner).build()));
    }

    @Test
    void revenues_withoutMonth_sumsTheWholeYearOfTheOwnerOnly_andHonorsIncludeInTotals() {
        final var salary = revenueType("Salario", true, true);
        final var caixinha = revenueType("Caixinha", false, true);
        revenue(salary, OWNER, "1000.00", LocalDate.of(2031, 1, 5), LocalDate.of(2031, 1, 5));
        revenue(salary, OWNER, "2000.00", LocalDate.of(2031, 2, 5), LocalDate.of(2031, 2, 5));
        revenue(salary, OWNER, "500.00", LocalDate.of(2030, 12, 31), LocalDate.of(2030, 12, 31));
        revenue(salary, OTHER, "9999.00", LocalDate.of(2031, 1, 5), LocalDate.of(2031, 1, 5));
        revenue(caixinha, OWNER, "300.00", LocalDate.of(2031, 1, 5), LocalDate.of(2031, 1, 5));

        final var result = revenueService.totalPorTipo(null, 2031, OWNER);

        assertThat(result).extracting(TypeTotalDTO::typeId).containsExactly(salary.getId());
        assertThat(result.get(0).total()).isEqualByComparingTo("3000.00");
    }

    @Test
    void revenues_withMonth_usesReferenceDateAndHonorsIncludeInMonthlyTotals() {
        final var salary = revenueType("SalarioM", true, true);
        final var decemberBalance = revenueType("SaldoDez", true, false);
        // competência em março, mesmo com a data de recebimento em fevereiro
        revenue(salary, OWNER, "1500.00", LocalDate.of(2032, 2, 28), LocalDate.of(2032, 3, 1));
        revenue(salary, OWNER, "700.00", LocalDate.of(2032, 3, 10), LocalDate.of(2032, 3, 10));
        revenue(salary, OWNER, "400.00", LocalDate.of(2032, 4, 1), LocalDate.of(2032, 4, 1));
        revenue(decemberBalance, OWNER, "250.00", LocalDate.of(2032, 3, 2), LocalDate.of(2032, 3, 2));

        final var result = revenueService.totalPorTipo(3, 2032, OWNER);

        assertThat(result).extracting(TypeTotalDTO::typeId).containsExactly(salary.getId());
        assertThat(result.get(0).total()).isEqualByComparingTo("2200.00");
    }

    @Test
    void revenues_orderedByTotalDescending() {
        final var small = revenueType("Pequena", true, true);
        final var big = revenueType("Grande", true, true);
        revenue(small, OWNER, "10.00", LocalDate.of(2033, 5, 1), LocalDate.of(2033, 5, 1));
        revenue(big, OWNER, "900.00", LocalDate.of(2033, 5, 1), LocalDate.of(2033, 5, 1));

        assertThat(revenueService.totalPorTipo(5, 2033, OWNER)).extracting(TypeTotalDTO::typeId)
                .containsExactly(big.getId(), small.getId());
    }

    @Test
    void spendings_withMonth_filtersByReferenceDateAndOwner() {
        final var type = tx.execute(status -> {
            final var t = SpendingType.builder().name("Cond" + UUID.randomUUID().toString().substring(0, 6))
                    .category(SpendingCategory.ESSENTIAL).build();
            entityManager.persist(t);
            return t;
        });
        final var add = (java.util.function.BiConsumer<String, String>) (owner, value) -> tx.executeWithoutResult(status ->
                entityManager.persist(Spending.builder().type(entityManager.find(SpendingType.class, type.getId()))
                        .value(new BigDecimal(value)).date(LocalDate.of(2034, 6, 10)).referenceDate(LocalDate.of(2034, 6, 10))
                        .wasPaid(true).ownerUsername(owner).build()));
        add.accept(OWNER, "800.00");
        add.accept(OWNER, "200.00");
        add.accept(OTHER, "5000.00");
        tx.executeWithoutResult(status -> entityManager.persist(Spending.builder().type(entityManager.find(SpendingType.class, type.getId()))
                .value(new BigDecimal("77.00")).date(LocalDate.of(2034, 7, 1)).referenceDate(LocalDate.of(2034, 7, 1))
                .wasPaid(false).ownerUsername(OWNER).build()));

        final var june = spendingService.totalPorTipo(6, 2034, OWNER);
        final var year = spendingService.totalPorTipo(null, 2034, OWNER);

        assertThat(june).singleElement().satisfies(t -> assertThat(t.total()).isEqualByComparingTo("1000.00"));
        assertThat(year).singleElement().satisfies(t -> assertThat(t.total()).isEqualByComparingTo("1077.00"));
    }
}
