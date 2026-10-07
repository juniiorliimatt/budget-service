package br.com.budget;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.budget.models.dto.MonthlySeriesPointDTO;
import br.com.budget.models.entities.Revenue;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.models.entities.Spending;
import br.com.budget.models.entities.SpendingType;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.BudgetRuleService;
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
 * {@code serieMensal} (12 meses de uma vez) contra Postgres real. A propriedade central é a
 * consistência com {@code cinquentaTrintaVinte}, que a tela usava mês a mês: a série tem que
 * devolver exatamente os mesmos números. Exige Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class MonthlySeriesIT {

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

    private static final String OWNER = "serie@workbox.local";
    private static final String OTHER = "outro-serie@workbox.local";

    @Autowired
    private BudgetRuleService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate tx;

    private RevenueType revenueType(final boolean inMonthly) {
        return tx.execute(status -> {
            final var type = RevenueType.builder().name("R" + UUID.randomUUID().toString().substring(0, 8))
                    .includeInTotals(true).includeInMonthlyTotals(inMonthly).build();
            entityManager.persist(type);
            return type;
        });
    }

    private SpendingType spendingType(final SpendingCategory category) {
        return tx.execute(status -> {
            final var type = SpendingType.builder().name("S" + UUID.randomUUID().toString().substring(0, 8)).category(category).build();
            entityManager.persist(type);
            return type;
        });
    }

    private void revenue(final RevenueType type, final String owner, final String value, final LocalDate reference) {
        tx.executeWithoutResult(status -> entityManager.persist(Revenue.builder().type(entityManager.find(RevenueType.class, type.getId()))
                .value(new BigDecimal(value)).date(reference.minusDays(3)).referenceDate(reference).ownerUsername(owner).build()));
    }

    private void spending(final SpendingType type, final String owner, final String value, final LocalDate reference) {
        tx.executeWithoutResult(status -> entityManager.persist(Spending.builder().type(entityManager.find(SpendingType.class, type.getId()))
                .value(new BigDecimal(value)).date(reference).referenceDate(reference).wasPaid(true).ownerUsername(owner).build()));
    }

    @Test
    void series_alwaysHasTwelveMonthsInOrder_zerosWhenThereIsNoData() {
        final var series = service.serieMensal(2041, "ninguem@workbox.local");

        assertThat(series).hasSize(12);
        assertThat(series).extracting(MonthlySeriesPointDTO::month).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(series).allSatisfy(p -> {
            assertThat(p.totalRevenue()).isEqualByComparingTo("0");
            assertThat(p.essential()).isEqualByComparingTo("0");
            assertThat(p.personal()).isEqualByComparingTo("0");
            assertThat(p.savings()).isEqualByComparingTo("0");
        });
    }

    @Test
    void series_matchesCinquentaTrintaVinteForEveryMonth_includingFlagsOwnersAndReferenceDate() {
        final var salary = revenueType(true);
        final var decemberBalance = revenueType(false);
        final var essential = spendingType(SpendingCategory.ESSENTIAL);
        final var personal = spendingType(SpendingCategory.PERSONAL);
        final var savings = spendingType(SpendingCategory.SAVINGS);
        final var year = 2042;

        revenue(salary, OWNER, "5000.00", LocalDate.of(year, 1, 10));
        revenue(salary, OWNER, "2500.50", LocalDate.of(year, 1, 20));
        revenue(decemberBalance, OWNER, "900.00", LocalDate.of(year, 1, 2));   // fora do total mensal
        revenue(salary, OTHER, "7777.00", LocalDate.of(year, 1, 10));           // outro dono
        revenue(salary, OWNER, "4000.00", LocalDate.of(year, 3, 5));
        revenue(salary, OWNER, "100.00", LocalDate.of(year - 1, 12, 31));       // outro ano
        spending(essential, OWNER, "1500.00", LocalDate.of(year, 1, 5));
        spending(essential, OWNER, "300.25", LocalDate.of(year, 1, 28));
        spending(personal, OWNER, "800.00", LocalDate.of(year, 1, 15));
        spending(savings, OWNER, "1000.00", LocalDate.of(year, 3, 1));
        spending(essential, OTHER, "9999.00", LocalDate.of(year, 1, 5));
        spending(personal, OWNER, "50.00", LocalDate.of(year, 12, 31));

        final var series = service.serieMensal(year, OWNER);

        for (final var point : series) {
            final var expected = service.cinquentaTrintaVinte(point.month(), year, OWNER);
            assertThat(point.totalRevenue()).as("receita mês %d", point.month()).isEqualByComparingTo(expected.totalRevenue());
            assertThat(point.essential()).as("essenciais mês %d", point.month()).isEqualByComparingTo(expected.essential().actual());
            assertThat(point.personal()).as("pessoais mês %d", point.month()).isEqualByComparingTo(expected.personal().actual());
            assertThat(point.savings()).as("poupança mês %d", point.month()).isEqualByComparingTo(expected.savings().actual());
        }
        assertThat(series.get(0).totalRevenue()).isEqualByComparingTo("7500.50");
        assertThat(series.get(0).essential()).isEqualByComparingTo("1800.25");
        assertThat(series.get(2).savings()).isEqualByComparingTo("1000.00");
        assertThat(series.get(11).personal()).isEqualByComparingTo("50.00");
    }
}
